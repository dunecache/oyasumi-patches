# Reference notes — com.one.goodnight 1.345.0 (version code 634)

Source: `Goodnight_ Voice Chat & Dating_1.345.0_APKPure.xapk` (APKPure, XAPK:
base `com.one.goodnight.apk` ~134MB + `config.arm64_v8a/en/mdpi` splits).
XAPK `manifest.json`: `package_name: com.one.goodnight`, `version_name: 1.345.0`,
`version_code: 634`, `min_sdk: 24`, `target_sdk: 36`.

Method: parsed with Python stdlib only (`zipfile` + `struct` dex `type_ids`
walk + raw byte search). No apktool/jadx here (no Java in Termux), so there
is NO smali yet — every identifier below is real (from dex/JS bytes), but
fingerprints still need smali-level confirmation before writing patches.

## App shape

- React Native (plain, non-Hermes `assets/index.android.bundle`, ~15.7MB,
  starts with `var __DEV__=false`). Most product logic is in this JS.
- Thin native shell: `com.one.goodnight.MainActivity`, `MainApplication`,
  heavily obfuscated single-letter classes (`Lcom/one/goodnight/a`..`z`,
  `A`..`H`) in `classes7.dex`, plus named RN bridge modules (same dex).
- `gatewayprotocol/v1` (~704 types, mostly `*OuterClass` protobuf) looks like
  the ad-stack protocol (`AdDataRefresh*`, `AdFormat*`,
  `AdOperationsConfiguration*`, `AdPlayerConfig*`). No VIP/user messages seen
  in class basenames — entitlement is NOT obviously in this protocol.
- 8 main dex files (`classes.dex`–`classes8.dex`, ~55MB total) + bundled
  `assets/audience_network/classes*.dex` (Meta Audience Network).

## Patch 1 — Premium/VIP unlock (root entitlement; server-driven)

- `membership` object fields (JS): `is_premium`, `is_premium_disco`,
  `is_star_premium`, `is_match_premium`.
- `isAnyPremium()` = OR of all four; `isPremium()` =
  `membership.is_premium || membership.is_premium_disco`.
- `updateUserMembership(membership)` sets `user.membership`, emits
  `AppCenterUserChanged`.
- Server endpoints (JS): `/api/me/vip_info` (`fetchVIPInfo`),
  `/api/me/vip_badge_info_v2` (`fetchVIPInfoV2`). VIP state comes from backend.
- Billing client-side = Google Play Billing (`com/android/billingclient/api/*`,
  `queryPurchases`-era API present). `BILLING` + `CHECK_LICENSE` permissions.
- Paywall analytics constants (JS): `paywall_viewed`, `purchase_initiated`,
  `purchase_completed`, `purchase_canceled`, `purchase_failed`; product kinds:
  `cans`, `disco`, `premium_disco`, `intro`, `spotlight`.
- Consequence: client patch can flip `isPremium`/`membership`, but features
  gated by server responses may still refuse. Device test decides per feature.
  Smali target: the RN-bridge/JNI path that delivers `membership`, or the
  `isPremium` getter if it exists natively (JS shows it as a JS getter, so the
  native patch point is more likely the membership delivery / API parse).

## Patch 2 — Blur removal (profile photos)

- Blur is the "star" game mechanic, NOT an image transform flag. Locale
  strings (JS): "Collect 7 stars to reveal the blurred photos of yourself and
  the other user." / "As long as you chat once a day, you can earn one-star".
- No `avatar_blur`/`unblur`/`mosaic` identifiers anywhere; `blur`×164 in JS is
  almost all RN text-input blur. So the patch point is the star-count/reveal
  gate (find: star count state, `playCountdown`, `notPlay`/`notPlayToday`
  branches), not a blur filter.
- Smali/JS target: star-balance getter or the reveal-condition check.

## Patch 3 — Chat unlock (chat without premium)

- No `chat_lock`/`message_limit`/`need_vip` strings. Currency = "cans"
  (`consumeUserCoin`, "Not Enough Cans" dialogs, `free_cans_to_new_subscriber`
  endpoint `/api/me/free_cans_to_new_subscriber`).
- Matching gate: `dailyLimitUpgradeDialog_*` — "Purchase cans to get more
  telepath tickets or upgrade to "Goodnight Disco" for unlimited matching!".
- `who_views_you` ×14 (+`whoviewsyou` ×2) — "who views you" is a separate
  gated surface, likely same membership flag.
- Smali/JS target: telepath-ticket count check / cans-deduction check on
  match/chat send. Standalone from patch 1 by design.

## Patch 4 — Call timer (free-call limit)

- Locale strings (JS): "Free calls are limited to 3 minutes, the user can
  choose whether to extend the free time" (`angelCallInDialog_desc`);
  `angelRadioFreeEndDialog_title: 'Free Trial Ended'`; "Listening to the call
  requires at least {0} Cans" (`angelRadioNoCansDialog_desc`).
- `calltime`/`calltimeout` dex hits are RN-framework `callTimers` internals —
  NOT the app timeout. Real anchors are the dialog keys above + the free-time
  countdown state behind them. Native voice = Agora (`io/agora` ~600 types,
  `com.one.goodnight.AgoraService`, `AgoraModulesManager`).
- Smali/JS target: free-call countdown / `angelRadioFreeEnd` trigger.

## Ads (context for later)

- Mediation: AppLovin MAX (~4k `applovin` refs), ironSource/LevelPlay
  (`InterstitialAdModulesManager`, `RewardedAdModulesManager`,
  `NativeBannerViewManager`, `VponBannerViewManager` — all real classes in
  `com.one.goodnight`), AdMob, Vungle, Bigo (`sg/bigo` 2150 types, incl.
  `RealtimeBlurLinearLayout`), Mintegral, Inmobi, Pangle, Chartboost, Unity.
- `shouldShowAd` = `!adDisabled && !isAnyAngel()`; `shouldShowMatchAd` also
  false when any premium flag set — i.e. patches 1 and ads interact.

## Permissions of note

`BILLING`, `CHECK_LICENSE`, `CAMERA`, `RECORD_AUDIO`, `ACCESS_FINE_LOCATION`,
`SYSTEM_ALERT_WINDOW`, `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`.

## Patch 0 (prerequisite) — Play license bypass: gate mapped from real smali

Symptom on device: patched app redirects to the Play Store and exits.
Cause: PairIP protection in `classes2.dex`, wired into the app entry point:

- `Lcom/pairip/application/Application;` extends `MainApplication`; its
  `attachBaseContext` calls, in order: `VMRunner.setContext`,
  `SignatureCheck.verifyIntegrity` (throws `SignatureTamperedException` when
  the APK signature differs from Play — always true for patched APKs; allows
  `expectedSignature` / `expectedLegacyUpgradedSignature` /
  `expectedTestSignature` / hardcoded
  `Vn3kj4pUblROi2S+QfRRL9nhsaO2uoHQg6+dpEtxdTE=`), then
  `LicenseClient.checkLicense`.
- `LicenseClient.checkLicense` → `performLocalInstallerCheck()Z`: SDK<30 or
  no PackageManager bypasses (returns false); system/updated-system app
  passes (returns true); otherwise requires installing package ==
  `com.android.vending`, else "Local install check failed due to wrong
  installer." On failure the LVL path runs and `LicenseActivity` opens the
  Play paywall (`showPaywallAndCloseApp` via `paywallintent` PendingIntent,
  `onStart` ordinal != 0) then `closeApp`/`exitApp` (`System.exit`).
- Same `checkLicense` is also called from
  `LicenseContentProvider.onCreate`, so patching the method itself (not the
  call sites) covers both.
- Patch (`patches/.../license/`): return-early `return-void` in
  `SignatureCheck.verifyIntegrity(Landroid/content/Context;)V` (anchor strings
  `SHA-256`, `Apk signature is invalid.`) and in
  `LicenseClient.checkLicense(Landroid/content/Context;)V` (anchor strings
  `Cannot check license with null context.`,
  `Skipping license check in isolated process.`). All four strings and both
  method signatures verified unique (x1) in 1.345.0 `classes2.dex`.
- Open: PairIP `VMRunner`/`VmDecryptor` regions and any server-side license
  re-checks; device test decides.
- Layer 2 (same patch): return-early `return-void` in
  `LicenseActivity.showPaywallAndCloseApp()V` (anchors `paywallintent`,
  `Paywall intent is not provided.`, both x1) and
  `LicenseActivity.showErrorDialog()V` (anchor: `runOnUiThread` call;
  uniqueness from class + name + empty params). Rationale: step-1 device
  test (license-only patch) still redirected, so the verdict comes from the
  native core; killing the effect covers dex and JNI triggers alike.

## TODO (needs smali)

1. ~~Get smali for 1.345.0 (tooling decision pending)~~ DONE (option a):
   androguard 4.1.4 installed via `pip install --no-deps` + `loguru`,
   `apkInspector`, `pydot`, `networkx`, `pygments`, `click`, `asn1crypto`,
   `mutf8`, `colorama` (system `python-lxml` reused). NOTE: full `pip install
   androguard` fails here (`psutil` has no Android support); apt route
   (apktool/openjdk) failed on mirror network errors. Helper:
   `/data/data/com.termux/files/usr/tmp/opencode/dump_smali.py`
   (kept OUT of the repo). Working dumps: `reference/smali/` (gitignored).
2. ~~Map `membership` delivery path~~ DONE, with a twist (see below).
3. Star-balance/reveal check — patch 2 hinge.
4. Telepath-ticket/cans check on match/chat — patch 3 hinge.
5. Free-call countdown trigger — patch 4 hinge.

## Smali-confirmed (androguard, classes7.dex)

- `Lcom/one/goodnight/InterstitialAdModulesManager;`
  (`ReactContextBaseJavaModule`): `loadInterstitialAd(String)`,
  `show(String)` (posts `Lcom/one/goodnight/r;` runnable via
  `UiThreadUtil.runOnUiThread`), `prepareAds(ReadableArray)`, `getName()`.
  `reference/smali/InterstitialAdModulesManager.txt`.
- `RewardedAdModulesManager` (+`$a/b/c`), `NativeBannerViewManager`,
  `VponBannerViewManager`: `reference/smali/RewardedAdModulesManager.txt`,
  `reference/smali/BannerManagers.txt`.
- `Lcom/one/goodnight/i` (+`$a/b/c/d`) is CAMERA code (CameraDevice,
  SurfaceTexture), not membership. Single-letter app classes are per-feature
  natives; do not assume which is which.
- NO native references to `membership` / `vip_info` / `is_premium` strings in
  `classes2.dex` or `classes7.dex`: VIP/entitlement logic lives in the JS
  bundle and crosses via the generic RN bridge. Consequence for patch 1: a
  dex `bytecodePatch` cannot flip `isPremium()` directly — either a resource
  patch on `assets/index.android.bundle` (check Morphe resource-patch DSL;
  template only shows `bytecodePatch`) or dex patches on the native ad-gate
  methods (e.g. no-op `show`/`loadInterstitialAd`), which give ad removal
  without touching JS.

---

# Djezzy 3.0.9 (version code 40076) — Phase 1: walk-and-win step path (recon)

Source: `Djezzy_3.0.9_APKPure.xapk` (APKPure, XAPK: base
`com.djezzy.internet.apk` 615 entries + `config.arm64_v8a/en/mdpi/zh`
splits). XAPK `manifest.json`: `package_name: com.djezzy.internet`,
`name: Djezzy`, `version_name: 3.0.9`, `version_code: 40076`,
`min_sdk: 24`, `target_sdk: 36`. API host (from `libapp.so` strings):
`https://apim.djezzy.dz/mobile-api`.

Method: Python stdlib (`zipfile`, raw byte search) + androguard 4.1.4
(class/method/instruction dumps). Full dumps kept OUT of the repo in
`/data/data/com.termux/files/usr/tmp/opencode/djezzy_{registrant,pedometer,li5c,li5b}.txt`;
only findings recorded here. Every identifier below is real output, never
invented.

## App shape (differs from Goodnight in every way that matters)

- Flutter (Dart AOT), NOT React Native. `lib/arm64-v8a/libapp.so`
  (~14.7MB) lives in the `config.arm64_v8a` split next to `libflutter.so`.
  All product logic (34 `djezzy_app_implementation/features/*` folders) is
  compiled Dart — Morphe `bytecodePatch` (dex) can only touch the thin
  Java shell + plugins, never Dart logic directly.
- Thin dex shell: `classes.dex` 11863 classes, `classes2.dex` 134,
  `classes3.dex` 217. R8 full-mode obfuscated: plugin classes renamed to
  single-letter names (`Li5/a`, `Lf7/a`, …); only some first-party
  (`io.flutter.plugins.*`, `com.djezzy.internet.MainActivity`) kept names.
- No ad-mediation SDKs in any dex (`applovin`/`UnityAds`/`ironsource`/
  `vungle`/`mintegral`/`pangle`/`inmobi`/`chartboost` all x0; `admob` x5
  only, Firebase-adjacent). No `billingclient`, no `BILLING` permission, no
  PairIP/`SignatureCheck`/`LicenseClient` strings — Goodnight-style license
  bypass has no target here.

## Step pipeline (smali-confirmed, classes.dex)

The `pedometer` Flutter plugin survived R8 as obfuscated `Li5/a` (proven by
`GeneratedPluginRegistrant.registerWith`, which does
`new-instance Li5/a` immediately before the catch block logging
`"Error registering plugin pedometer, com.example.pedometer.PedometerPlugin"`):

- `Li5/a.onAttachedToEngine`: creates EventChannel `"step_detection"`
  (field `i`) and EventChannel `"step_count"` (field `j`); attaches
  `Li5/c` stream handlers constructed with int selectors **18** and **19**
  (= `Sensor.TYPE_STEP_DETECTOR` / `Sensor.TYPE_STEP_COUNTER`).
  `onDetachedFromEngine` clears both handlers.
- `Li5/c.<init>(binding, sensorType)`: `sensorType == 19` → sensorName
  `"StepCount"`, else `"StepDetection"`; `getDefaultSensor(sensorType)`.
  `onListen`: null sensor → `sink.error("1", "<name> is not available on
  this device")`; else `registerListener(new Li5/b(sink), sensor, 0)`
  (delay 0 = fastest). `onCancel`: `unregisterListener`.
- `Li5/b.onSensorChanged` (THE emission point): `sink.success(
  Integer.valueOf((int) event.values[0]))` — 10 instructions: null-check,
  `iget event.values [F`, `aget 0`, `float-to-int`, `Integer.valueOf`,
  `EventSink.success`. A spoof patch edits here (replace the sensed int
  with a constant/increment before `valueOf`).

## Fingerprint anchors (substring counts across all 3 dex, all x1 in classes.dex unless noted)

| String | classes.dex | classes2 | classes3 | Lives in |
|---|---|---|---|---|
| `step_count` | 1 | 0 | 0 | `Li5/a.onAttachedToEngine` const |
| `step_detection` | 1 | 0 | 0 | `Li5/a.onAttachedToEngine` const |
| `StepCount` | 1 | 0 | 0 | `Li5/c.<init>` const |
| `StepDetection` | 1 | 0 | 0 | `Li5/c.<init>` const |
| `stepCountChannel` | 1 | 0 | 0 | `Li5/a` null-guard |
| `stepDetectionChannel` | 1 | 0 | 0 | `Li5/a` null-guard |
| `Error registering plugin pedometer, com.example.pedometer.PedometerPlugin` | 1 | 0 | 0 | `GeneratedPluginRegistrant.registerWith` catch |
| `null cannot be cast to non-null type android.hardware.SensorManager` | 1 | 0 | 0 | `Li5/c.<init>` (also in sensors_plus `Lf7/a`; count is dex-wide so fingerprint must combine anchors) |
| `flutterPluginBinding` | 1 | 0 | 0 | `Li5/a.onAttachedToEngine` |

Note: `Li5/a` has only 3 methods (`<init>`, `onAttachedToEngine`,
`onDetachedFromEngine`); `Li5/b` has 3 (`<init>`, `onAccuracyChanged`,
`onSensorChanged`); `Li5/c` has 3 (`<init>`, `onCancel`, `onListen`).
Fingerprint should anchor on the `step_count`/`step_detection` const-strings
+ `EventChannel.<init>` opcode shape, NOT on the obfuscated `Li5/*` names
(R8 renames are version-fragile).

## Dart side (libapp.so strings, all real paths)

- 15 files under `features/walk_and_win/`: `data/services/pedometer_service.dart`,
  `data/datasources/walk_and_win_remote_datasource.dart`,
  `data/repositories/walk_and_win_repository_impl.dart`,
  `data/models/waw_campaign_model.dart`,
  `domain/entities/waw_campaign.dart`,
  `presentation/bloc/walk_and_win_{bloc,event,state}.dart`, 5 widgets incl.
  `walk_step_counter_card.dart`.
- Storage keys (x1 each): `walk_and_win_current_steps`,
  `walk_and_win_last_pedometer_value` — Dart keeps last sensor value and
  computes deltas (TYPE_STEP_COUNTER is cumulative-since-boot, so the app
  diffs readings). Spoof design must account for this: a fixed constant
  yields ONE delta then zeroes; a steady trickle needs an incrementing
  counter (static field) or scaled real values.
- Server endpoints (path fragments, x1 each): `/services/walk/campaign/`,
  `/services/walk/activate-reward/` (siblings: `/services/scan/activate-reward`,
  `/services/mgm/activate-reward`). Rewards are server-issued: client
  reports steps, backend decides. Spoofed steps can be rejected server-side
  (e.g. implausible deltas) — device test decides.
- Trust signals: NO `attest`/`SafetyNet`/`PlayIntegrity`/`cheat`/`fraud`/
  `suspicious` strings; `verifyQrToken`/`verify_qr_token_usecase` belong to
  scan-and-win (Phase 2); `checksum` hits are archive-lib internals;
  `signature*` hits are all e-signature-pad UI widgets (unrelated). No
  step-specific anti-tamper found — spoofable in principle at the sensor
  layer.

## Sibling note (Phase 2 input, not Phase 1 scope)

`Lf7/a` = sensors_plus (accel/gyro/magnet/barometer channels only, no step
types) — NOT an alternate step source. Scan-and-win already shows the same
remote-datasource + response-model shape (`scan_and_win_remote_datasource.dart`,
`scan_qr_code_usecase.dart`, `scan_response_model.dart`) with server token
verification (`verifyQrToken`).
