# ADM 14.0.27 reference notes

## Source and target record

- Reference: `~/storage/downloads/1DM/Programs/com.dv.adm_14.0.27-140027_minAPI26(arm64-v8a,armeabi-v7a,x86,x86_64)(nodpi)_apkmirror.com.apk`
- SHA-256: `6f1d3aee879fe58cbd77e8ef01b3ce6e4d3f77aadd3e8276ec8232d0bdf006c1`
- Size: `58,716,075` bytes.
- Format: regular APK/ZIP containing 2,190 entries, not a split APKM container.
- Package: `com.dv.adm`.
- Version name: `14.0.27`.
- Version code: `140027`.
- Minimum SDK: `26`.
- Target SDK: `33`.
- Launcher activity: `com.dv.get.Main`.
- Application class: `com.dv.get.AApp`.
- The reference is user-supplied and has not been independently verified as the original publisher build.

## APK structure

- DEX files: `classes.dex`, `classes2.dex`, `classes3.dex`, `classes4.dex`.
- DEX class counts: 9,742; 6,082; 11,778; 7,198 respectively.
- The app's own classes are concentrated in `classes2.dex`: 276 classes under `Lcom/dv/`.
- `classes.dex` contains one app-named class, `Lcom/dv/adm/AEditor;`.
- `classes3.dex` and `classes4.dex` contain no `Lcom/dv/` classes.
- Native libraries are present for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.
- Notable native libraries: `libjlibtorrent-1.2.19.0.so`, `libPglmetasec_ov.so`, `libEncryptorP.so`, `libapminsighta.so`, `libapminsightb.so`, `libsentry-android.so`, and `libsentry.so`.
- The manifest declares 22 permissions, including internet access, external-storage access, boot completion, exact alarms, overlay windows, notifications, and Google Play billing.

## Application surface

- `com.dv.get.Main` is the launcher and download-list UI.
- `com.dv.get.AEditor` accepts `ACTION_SEND` and `ACTION_SEND_MULTIPLE` and also exposes start/stop actions.
- `com.dv.get.Web` accepts shared `text/*` and other content through `ACTION_SEND`.
- `com.dv.get.Back` is the persistent download/torrent service.
- `com.dv.get.Deep` handles boot, widget, and exact-alarm permission events.
- `com.dv.get.Pref` is the settings activity and accepts the quick-settings tile preference action.
- Two quick-settings tile services are exported with `BIND_QUICK_SETTINGS_TILE`.
- `com.dv.get.all.receiver.ReceiverStart`, `ReceiverStop`, `ReceiverOpen`, `ReceiverPlan`, and `ReceiverExit` provide broadcast-driven service and schedule controls.

## Billing and ad-free state

- `Lcom/dv/get/f3;` is the central monetization and ad helper.
- `f3.a:boolean` is initialized to `false` in `f3.<clinit>`.
- `f3.B(List<Purchase>)` checks a purchase whose product list contains `ads_disable`; after acknowledgement it sets `f3.a` to `false` and persists `hua_voice=false`.
- `Lcom/dv/get/e3;->c(BillingResult)` queries the `ads_disable` SKU and registers purchase callbacks.
- `f3.l(MyActivity)` initializes billing and reads the stored ad-free state.
- `t0.X2()` appends ` Pro` to the displayed version when `f3.a` is false. This confirms that the observed entitlement is an ad-free/Pro label, not evidence of a broader feature unlock.
- `f3.i(Activity)` initializes Appodeal interstitials.
- `f3.h()` shows the Appodeal banner only when `f3.a` is true.
- `f3.j(MyActivity)` shows an Appodeal interstitial after a short delay.
- `f3.n(MyActivity)` coordinates remote configuration, ad initialization, banner/interstitial scheduling, and Huawei prompts.
- `f3.c()` creates an AppBrain banner, while the manifest also contains AppLovin, AdMob, Unity Ads, Vungle, Appodeal, Criteo, Pangle, Bigo, Mintegral, Fyber, and other ad SDK components.
- `Back.onDestroy()` tracks `MAIN_ADS6`, `RATE_APP10`, and `RATE_ADS22` and increments rating/ad counters. This is a separate rating-prompt surface from the Appodeal calls.

## Patch 1 — Disable ads

- Compatibility: `com.dv.adm`, version `14.0.27`, regular APK.
- The patch returns early from the four app-owned ad entry points: `f3.c()` for AppBrain/Appodeal setup, `f3.i(Activity)` for Appodeal interstitial initialization, `f3.h()` for banner display, and `f3.j(MyActivity)` for interstitial display.
- `Lcom/dv/get/Main;->s3()V` is the only automatic Telegram join-prompt inflation site. It reads `TELE1_KEY` and `TELE2_KEY`, then inflates the `Lh2/c;->s` `ViewStub` through `Lh2/u0;->a(View)` and `Lh2/u0;->b()`. The gate constant `const/4 v9, 2` is at instruction index 98.
- The patch replaces that gate constant with `const/4 v9, 0`, causing the existing `if-ge v6, v9` at index 99 to skip the prompt block for the app-written counter values without changing instruction width.
- The Telegram URL `https://t.me/adm_torrent` is also used by the explicit `Main.onOptionsItemSelected` menu item at index 105; that user-initiated link is intentionally left intact.
- Fingerprints use the verified `main-toolend`, Appodeal key, `AppoInterShow`, and Telegram preference-key strings plus exact method signatures and ordered instruction anchors. Each anchor occurs once in the reference DEX.
- The patch does not alter downloader, torrent, browser, billing, Huawei, or remote-configuration methods.
- Static fingerprint validation passed. Gradle compilation and device application are pending because Java is unavailable in the current environment.

## Patch 2 — Disable rating prompts

- Compatibility: `com.dv.adm`, version `14.0.27`, regular APK.
- The patch returns early from `Main.W(Main)`, the dedicated wrapper that calls `Main.Y1(7)` for the `RATE_APP10` rating dialog.
- `Main.W(Main)` has one verified caller: the delayed `Lcom/dv/get/g0;` callback used by the rating flow. `Back.onDestroy()` is intentionally untouched so service cleanup and normal teardown continue.
- The fingerprint uses the exact method signature, literal case value `7`, and the `Main.Y1` call.
- Static fingerprint validation passed. Gradle compilation and device application are pending because Java is unavailable in the current environment.

## Downloader controls

The following preference keys are loaded by `Lcom/dv/get/Pref;` and are strong candidates for narrowly scoped client-side patches:

- `DOWN_LOADS_3G`, `DOWN_LOADS_WF`, `DOWN_LOADS_3GWF`: simultaneous download limits by network profile.
- `DOWN_THREADS_3G`, `DOWN_THREADS_WF`, `DOWN_THREADS_3GWF`: connection count per download.
- `DOWN_MINSIZE_*`, `DOWN_ERRORS_*`, `DOWN_TIMEOUT_*`: minimum chunk size, retry count, and timeout.
- `DOWN_ALGORITM_*` and `DOWN_USERAGENT_*`: download algorithm and user-agent profile.
- `DOWN_DIRS`, `DOWN_FILENEW`, `DOWN_RESTART`, `DOWN_PROFILE`, and `DOWN_PROXY`: storage and transfer behavior.
- `WIFI_FLAG`, `WIFI_AUTO`, `WIFI_AUTO_S`, `SERV_AUTO`, and `SERV_STOP_2`: network and background-service controls.
- `TORR_*`: torrent enablement, sequential mode, trackers, connection and upload-slot limits, upload speed/time limits, watch folder, Wi-Fi/charging restrictions, and encryption mode.
- `SCHD_FLAG`, `SCHD_START`, `SCHD_STOP`, `SCHD_WIFI`, `SCHD_MOBI`, `SCHD_REPE`, and `SCHD_ALARM`: scheduler behavior.

`Back.onCreate()` registers a receiver for battery, power, Wi-Fi, widget, and exact-alarm changes. `t0.N()` checks the battery level against `Pref.C2`; `t0.W1()` opens the exact-alarm settings screen; `Deep.onReceive()` dispatches service and widget events.

## Patch 3 — Increase connection limits

- Compatibility: `com.dv.adm`, version `14.0.27`, regular APK.
- `Lcom/dv/get/Pref$o;->f(Lcom/dv/get/Pref$o;)V` is the single verified synthetic accessor for the `s213` (`Simultaneous downloads`) and `s215` (`Threads per download`) slider. Its `const/16 v1, 32` instruction at index 6 is the shared ceiling; replacing it with `const/16 v1, 64` raises both controls from 32 to 64. The lower bound remains 4.
- `Lcom/dv/get/Pref;->G1(Landroid/app/Activity;)V` loads `TORR_MAXCONNECT` with the default string `210` at instruction index 1222 and `TORR_MAXCONNECTPER` with `70` at index 1227. The patch changes those defaults to `500` and `100` respectively.
- The torrent dialog already installs `InputFilter.LengthFilter(9)`, so the torrent change is a default-value change rather than a new hard UI ceiling. Existing saved preferences are not overwritten.
- Fingerprints use the exact synthetic accessor signature, the two resource IDs, the unique `32` literal, both preference keys/default strings, and the `Pref.E1(String,String)` call.
- Selected values: download ceiling `64`; torrent global default `500`; torrent per-torrent default `100`.
- Static DEX anchor validation passed. Gradle compilation and device application are pending because Java is unavailable in the current environment.

## Patcher pitfalls

- `BuilderInstruction3rc` encodes an `invoke-*/range` register count that must equal the referenced method's parameter count, otherwise ART rejects the class. A shared helper that hardcodes a literal count silently breaks for any hook with a different arity, so derive it from `parameters.size`.
- `newLabelForIndex` attaches a label to the instruction object occupying that index when the call is made, and that object keeps its identity as later insertions shift it. When several instructions are inserted at indices `0..n`, bind the continue label to `1`, not to the post-insertion final index, or the false branch skips the injected block and leaves later registers undefined, which surfaces as a `VerifyError` when the class loads.
- The DEX prototype for `Landroid/view/MenuItem;->setShowAsAction(I)` in this build is `(I)V`, while the public SDK method returns `MenuItem`, so a fingerprint written from the SDK signature never matches and must declare `returnType = "V"`. `setIcon(I)` does return `Landroid/view/MenuItem;` in the same method, so the two cannot be assumed to agree.

## Build environment

- `openjdk-21` is installed at `/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk` and exported through `/data/data/com.termux/files/usr/etc/profile.d/openjdk.sh`.
- A local Gradle build is not possible: `https://maven.pkg.github.com/MorpheApp/registry` returns `401` for the available `gh` token, which lacks the `read:packages` scope, so `app.morphe.patches` plugin `1.3.4` cannot be resolved. Compilation is delegated to CI.

## Browser and remote data

- `Lcom/dv/get/Web;` owns the built-in browser and creates its WebView in `S2()Landroid/webkit/WebView;`, which installs `Web$h;` as the `WebChromeClient`, `Web$i;` as the `WebViewClient`, `La2/m4;` as the long-click listener, and `La2/o4;` as the `DownloadListener`.
- `Web.onOptionsItemSelected()` toggles `BROW_ADSB` through `Pref.m5`.
- Browser menu handling does **not** use the framework options menu. `Web.onCreateOptionsMenu(Menu)` is called only from `Web.o3()` and its return value is discarded, so the `Menu` it fills is never displayed. The visible browser overflow is a `PopupMenu` wrapped by `Lb2/f;`, created inside `Web.onOptionsItemSelected(MenuItem)`, filled through `Lb2/f;->d()Landroid/view/Menu;`, and shown by `Lb2/f;->e()V`. Item clicks reach the `Lb2/f$b;` callback installed by `Lb2/f;->c(Lb2/f$b;)V` through `Lb2/e;->onMenuItemClick(MenuItem)`.
- Register windows: `Lb2/f;->e()V` has 8 registers with `this` in `v7` and the `PopupMenu` in `v0` at original indices 3 and 6 before the two `SDK_INT >= 29` branches. `Lb2/e;->onMenuItemClick` has 3 registers with `this` in `v1` and the item in `v2`, delegating to `Lb2/f$b;->b(MenuItem)` at index 1. `Web$i;->onPageStarted` and `Web$i;->shouldInterceptRequest` both place `this` in `v3`, so `(WebView,String)` is `v4,v5`.
- `Web.onCreateOptionsMenu(Menu)` has 18 registers and 412 instructions; `this` is `v16` and the `Menu` argument is `v17`, and the returns sit at indices `19, 60, 140, 293, 411`. `Web.onOptionsItemSelected(MenuItem)` has the same register layout, calls `MenuItem.getItemId()` at index `2` into `v1`, and consumes the id with a `sparse-switch` at index `25` whose payloads are resource ids such as `0x7f09003a`.
- `Lb2/f;` has a single private field `a` of type `Landroid/widget/PopupMenu;`, and `d()Landroid/view/Menu;` simply returns `a.getMenu()`. `android.widget.PopupMenu` exposes no `getAnchorView()`, so a hook needing the host `Activity` has to capture it from the `WebView` context earlier and cache it, for example in a `WeakReference`; the click path's `host` is the `Lb2/e;` wrapper, not an `Activity`.
- The default resource table contains `alive_hosts` and an `https://adm.dimonvideo.ru/alive_hosts.txt` value, confirming a remote host/ad-block list path.
- `Web` also manages cookies, history, JavaScript, image loading, dark mode, saved tabs, search engines, and the `file://` URL bridge.
- DEX recon resolves the existing direct-download intents in `C0` and `c1` to `Lcom/dv/get/AEditor;`; `Lcom/dv/adm/AEditor;` is a separate class.
- `f3.g()` reads a remote response through resource ID `str07` and stores key/value pairs in the `xyz` shared-preference file. The resource table identifies the endpoint as `https://adm.dimonvideo.ru/data`; the request adds `?jack=927`.
- `f3.E()`, `f3.F()`, and `f3.G()` read Huawei/AppGallery state and message data. `f3.p()` and `f3.q()` invoke Huawei/AppGallery-related paths.

## Privacy and diagnostics

- `AApp.onCreate()` installs a custom uncaught-exception handler in `Lf2/a;`, creates a `crash_reports` directory, and starts a `Lf2/b;` worker thread.
- The package includes Sentry native libraries, APM Insight native crash libraries, Google data transport, AppBrain components, and advertising identifiers.
- `t0.p2(Activity)` reads the `firebase.test.lab` system setting into `t0.n`; this is a verified control-flow path, not proof of a particular Firebase event.

## Next implementation suggestions

1. **Disable Huawei/AppGallery prompts and remote configuration:** target the `HUA_*` branches in `f3` and the `f3.g()` remote-config read separately from ad removal.
2. **Privacy mode:** disable the custom crash handler and diagnostic worker in `AApp.onCreate()`; assess Sentry/AppBrain separately because they are separate SDKs.
3. **Download tuning:** change or expose the existing `DOWN_*` limits rather than inventing new downloader code; test against real servers because server-side limits still apply.
4. **Torrent tuning:** adjust the existing `TORR_*` settings, but validate the native jlibtorrent boundary and do not assume a DEX-only edit changes native engine behavior.
5. **Scheduler/background reliability:** inspect the `Back`, `Deep`, and `t0` battery/Wi-Fi/exact-alarm paths; this is feasible but device- and Android-version-sensitive.
6. **Browser ad blocking:** force or repair the existing `BROW_ADSB`/hosts path instead of adding a new blocking engine.

## Unresolved risks

- The native protection libraries may perform integrity or runtime checks outside the reach of a DEX patch.
- SDK providers may still initialize independently even after the app-owned ad entry points are skipped.
- The remote ad, Huawei, Firebase, and diagnostic paths may continue independently.
- Download and torrent behavior is constrained by servers, network conditions, Android background execution, and native code.
- The three patches compile and apply, but their runtime effect is still unconfirmed on a device.
- The 14.0.27 fingerprints were removed when the patches were retargeted, so 14.0.27 is no longer declared as a target. Declaring it would advertise support that fails with a fingerprint error, because every fingerprint is 14.0.39-only. Re-adding it means either re-deriving the 14.0.27 fingerprints or selecting between two fingerprint sets per patch, which is a larger change than the retarget.

# ADM 14.0.39 reference notes

## Source and target record

- Reference: `~/storage/downloads/com.dv.adm_14.0.39-140039_4arch_7dpi_19lang_1feat_24af8dcba9ae0566c68f55845c95e1ef_apkmirror.com.apkm`
- Format: APKM bundle. Only `base.apk` was analysed; the ABI, density, and language splits were ignored because they contain no DEX.
- `base.apk` size: `41,687,635` bytes. DEX files: `classes.dex` through `classes6.dex` (9,405 / 5,551 / 10,626 / 5,447 / 2,319 / 2,339 classes).
- Package: `com.dv.adm`. Version name: `14.0.39`. Version code: `140039`.
- The app's own code moved from `classes2.dex` (14.0.27) into `classes.dex` (14.0.39).
- `com.dv.get` activity and service classes keep their names, but every obfuscated member was renamed: `f3` became `Lv2/o5;`, `f3.g()` became `Lv2/e3;->run()`, the rating wrapper's dispatcher became `Lv2/p1;`, and `Main.s3()`/`Main.Y1()`/`Main.W()` became `Main.K()`/`Main.n()`/inlined.
- `Lcom/dv/get/Pref$o;` no longer exists. 14.0.39 has no inner classes under `Lcom/dv/get/Pref;` at all.
- `Pref.E1(String, String)` became `Pref.A(String, String)`. `Pref.G1(Activity)` became `Pref.C(Activity)`.
- Every fingerprint below was re-derived from this DEX. None of the 14.0.27 fingerprints resolve against 14.0.39.

## Monetization surface

- `Lv2/o5;` is the monetization and ad helper, the successor of `Lcom/dv/get/f3;`.
- `Lv2/o5;->a Z` is the ad-free flag: `o5.b()` returns immediately when it is set, so `false` means "show ads".
- `Lv2/o5;->c(Activity)` initializes Appodeal with the publisher key, guarded by a one-shot `o5.p` latch.
- `Lv2/o5;->b()` shows the Appodeal banner: it requests the banner view, attaches it to the app's ad container, broadcasts `main-toolend`, and otherwise calls `Appodeal.show(activity, 64)`.
- `Lv2/o5;->d(Activity)` shows the Appodeal interstitial, rate limited by the `AppoInterShow` timestamp preference.
- `Lv2/o5;->n()` is the single accessor for the ad container, resolving `findViewById(2131296377)` or `2131296356` depending on `o5.d`. It has exactly four callers, all of which null-check the result: `Lv2/e3;->run()` at two sites, `Lv2/o5;->b()`, and `Ls3/g;->onInterstitialClosed()`.
- 14.0.27 combined banner creation and Appodeal setup in one `f3.c()`. 14.0.39 split them: the Appodeal banner moved to `o5.b()` and the AppBrain banner moved into a remote-configuration dispatch case of `Lv2/e3;->run()`.
- `Lv2/e3;->run()` is a packed-switch runnable over `e3.a` with cases 8, 4, 24, 0, and 1. Its AppBrain case constructs `AppBrainBanner` (index 89), attaches it (93), reveals the container (94), and broadcasts `main-toolend` (96) before continuing into the Appodeal banner setup and returning at 152.
- The Appodeal application key `18b2becc3142993292bf348e92467eded74e23229100a646` is user supplied and unchanged between the two versions. `main-toolend` is an app broadcast action, not an SDK key; it also appears in `o5.b()` and in `Main.K()`.
- `Lcom/dv/get/Main;->K()V` performs activity start-up and inflates the Telegram prompt. Both `ViewStub.inflate()` (108) and `View.setVisibility()` (126) occur exactly once in the method, which makes them reliable anchors.
- The Telegram prompt is gated by two view counters: `if-ge v4, v7` at 103 skips the prompt once `TELE1_KEY` reaches the `const/4 v7, 2` threshold at 101, and `if-ge v6, v7` at 105 applies the same test to `TELE2_KEY` against 9. Setting the first threshold to 0 makes the comparison always hold and lands on the normal start-up path at 163. 14.0.39 inserts one `const/4 v8, 1` between the threshold and the branch, which is why the branch anchor allows one intervening instruction.

## Patch 1 — Disable ads (14.0.39)

- Compatibility: `com.dv.adm`, version `14.0.39`. `ApkFileType.APK` is non-required, so the APKM bundle is also accepted.
- `Lv2/o5;->c(Activity)`, `Lv2/o5;->b()`, and `Lv2/o5;->d(Activity)` return early, which is the same three entry points 14.0.27 patched under different names.
- The AppBrain banner is not in its own method in 14.0.39, so the patch does not return early from `Lv2/e3;->run()`. It replaces `ViewGroup.addView(View)` at index 93 and `View.setVisibility(I)` at index 94, each a three-code-unit `invoke-virtual`, with three `nop`s of the same width. Leaving the remote-configuration fetch and the rest of that dispatch case intact, and leaving every address in the method unmoved. The registers the two calls read (`v0` at 83, `v1` at 87, `v4` at 4) are still assigned, so no undefined read is introduced.
- The `main-toolend` broadcast at 96 still fires, so any component that reacts to it is unaffected.
- The Telegram gate constant is replaced with `const/4 v7, 0`, keeping the register and the instruction width.
- Fingerprints: the Appodeal key, `AppoInterShow`, `main-toolend`, the `TELE*_KEY` preference keys, the unobfuscated `Lcom/appbrain/AppBrainBanner;` type, and the SDK classes `Lcom/appodeal/ads/Appodeal;`, `Landroid/view/ViewStub;`, `Landroid/view/ViewGroup;`, and `Landroid/view/View;`. Each anchor was confirmed to occur exactly once inside its own method.
- The app's own "remove ads" placeholder at `Lv2/e3;->run()` 168-208 is intentionally left intact; it is a house promo, not an ad SDK view.

## Patch 2 — Disable rating prompts (14.0.39)

- The dedicated `Main.W(Main)` wrapper is gone. `Main.n(7)` is now called from `Lv2/p1;->run()` at index 724, one of three calls in that dispatcher, inside the case bounded by `Main.s()` at 719 and `return-void` at 725.
- The patch replaces only the `Main.n(I)` invocation with three `nop`s, so the delayed case still returns normally and the other eleven cases are unaffected. `v0` (721) and `v5` (723) are still assigned.
- The user-initiated rating menu item calls `Main.n(7)` directly from `Main.onOptionsItemSelected`, so it still opens the dialog. This matches the 14.0.27 intent of silencing only the automatic prompt.
- `Back.onDestroy()` and its `RATE_APP10` / `RATE_ADS22` / `MAIN_ADS6` counters are untouched, so service teardown is unaffected.

## Patch 3 — Increase connection limits (14.0.39)

- `Lcom/dv/get/Pref;->U()V` builds the download settings screen. Each control is a `Lv2/j4;` preference whose bounds are the `a` and `b` fields; `Lv2/j4;->showDialog` sets the seek bar range to `b - a`.
- The bounds are register constants loaded once at the top of the method, not per-control literals, so the 14.0.27 `Pref$o.f` slider accessor has no successor. The constants are `v2=0`, `v3=6`, `v6=961`, `v7=5`, `v8=16`, `v9=1`.
- `v7` is the ceiling for all three simultaneous-download controls. It is also reassigned and reused for unrelated case identifiers and objects later in the method, but a proper liveness pass shows its value reaches exactly the three `DOWN_LOADS_*` maxima and nothing else, so raising `const/4 v7, 5` to `const/16 v7, 32` affects only those. `const/4` cannot encode 32, so the replacement is one code unit wider; the method has no switch or array payload and the patcher recomputes branch offsets.
- **The per-download ceiling needs a different mechanism.** `v8=16` is the ceiling for all three `DOWN_THREADS_*` controls, but the same constant is also the *minimum* of the three chunk-size controls at indices 46, 184, and 330, where the maximum is `v6=961`. Editing `v8` would silently raise the minimum chunk size from 16 to the new ceiling, changing download chunking behaviour.
- Instead, a `const/16` is written immediately before each `DOWN_THREADS_*` maximum store, and the store is rewritten to read that constant. `v5` is the register used: it is assigned four times in `Pref.U()` (indices 5, 106, 246, 393) and **never read**, so it is dead and cannot disturb any other value. Each control's maximum is written independently, and each site's `j4` target register and field are taken from the original instruction rather than hardcoded.
- The three maxima are at indices 33, 170, and 317, each written just *before* its own preference key at 38, 175, and 322. Note that a control's bounds precede its key, so a fingerprint that searches forward from the key finds the next control's bounds instead. The `ThreadCeilingFingerprint` chain therefore walks shared minimum constant, `DOWN_LOADS_*` maximum, `DOWN_THREADS_*` minimum, `DOWN_THREADS_*` maximum, then the key, which lands on 9, 16, 32, 33, and 38.
- `Lcom/dv/get/Pref;->C(Activity)` still reads `TORR_MAXCONNECT` with the default `210` at 1222 and `TORR_MAXCONNECTPER` with `70` at 1227, through `Pref.A(String, String)`. Both are `const-string` replacements that keep the original width and destination register, so the surrounding reads are untouched. Already saved preferences still win.
- Resulting defaults: at most 32 simultaneous downloads and 64 connections per download, torrent defaults 500 global and 100 per torrent, chunk size still 16 to 961.
- 14.0.39 is more restrictive than 14.0.27 here: 14.0.27 capped simultaneous downloads at 32 with a minimum of 4 and threads at a single shared ceiling, while 14.0.39 caps downloads at 5 with a minimum of 1 and threads at 16.

## Verification performed for 14.0.39

- A re-implementation of Morphe's own matching algorithm (type-declaration comparison, `parametersMatch`, the `matchFilters` backtracking loop, and `MatchAfterWithin` distance rules) was run against the 14.0.27 DEX first. It reproduced every instruction index recorded in the 14.0.27 notes above, including the Telegram gate at 98, the slider ceiling at 6, and the torrent defaults at 1222 and 1227, which is what makes it trustworthy for 14.0.39.
- All nine 14.0.39 fingerprints were then resolved against the 14.0.39 DEX and each reported the expected instruction indices.
- Every replacement instruction was chosen to keep the source register. Five of the eight replacements are exactly width-preserving, so the two rewritten methods that contain a `packed-switch` payload (`Lv2/e3;->run()` at byte `0x052c` and `Lv2/p1;->run()` at byte `0x0c60`) keep that payload at its original 4-byte-aligned address. The width changes are confined to `Pref.U()`, which has no switch or array payload, where only branch offsets move and `MutableMethodImplementation.replaceInstruction` re-fixes them.
- `Pref.U()` was additionally checked by a register liveness pass and by simulating the patched instruction stream. The simulation confirms all six download controls receive the intended bounds, the three chunk-size controls keep minimum 16 and maximum 961, and the patch introduces no uninitialised read and no int/object type violation on any instruction it writes. The four findings the simulation reports exist identically before and after patching and are in untouched app code, where a linear pass cannot model per-merge-point register typing.
- Note for future work: androguard's `Instruction.get_length()` returns a nibble count, not code units, so instruction addresses derived from it are wrong. The widths used above come from the DEX instruction format table instead.
- Verified in CI: the patch project compiles and the bundle builds and publishes as a release asset.
- Verified on device: the `0.2.1-dev.2` bundle applies cleanly to 14.0.39 on Android 15 with all three patches enabled, so every fingerprint resolves and every generated smali instruction assembles.
- Still unverified on device: the runtime effect of each patch. Applying successfully proves the fingerprints and encodings, not that ads are gone, that the sliders show the new bounds, or that no layout gap is left where the AppBrain container used to sit. Those need a manual pass.

# Pinterest 14.38.0 reference notes

Disassembly record for the reference build and the six Pinterest patches written against it.
Patch status and remaining work are tracked in `todo.md` on the branch that carries this work.

## Source and target record

- Reference: `~/apks/com.pinterest_14.38.0-14388010_minAPI29(arm64-v8a,armeabi-v7a,x86,x86_64)(nodpi)_apkmirror.com.apk`
- SHA-256: `af6b383adb445cebee1ca43f14ac409f91475c1d62e0e11ef52ef52e29fb0553`
- Size: `133,740,741` bytes.
- Format: regular APK/ZIP with 7,581 entries, not a split APKM container. The manifest carries
  `STAMP_TYPE_STANDALONE_APK` and `com.android.vending.derived.apk.id` `2`, confirming this is the
  APKMirror standalone re-pack rather than a Play-delivered split set.
- Package: `com.pinterest`.
- Version name: `14.38.0`.
- Version code: `14388010`.
- Minimum SDK: `29`.
- Target SDK: `36`, compile SDK `36` (Android 16).
- Application class: `com.pinterest.ReleaseHiltApplication`, which extends `Lf62/a;`.
- Launcher: the `activity-alias` for `com.pinterest.activity.PinterestActivity`; the real
  `activity/task/activity/MainActivity` is separate.
- The reference is user-supplied and has not been independently verified as the original
  publisher build. Because it is a standalone all-ABI re-pack, it may contain code that differs
  from the Play split APK even at the same version code.

## APK structure

- DEX files: `classes.dex`, `classes2.dex`, `classes3.dex`, `classes4.dex`, `classes5.dex`,
  `classes6.dex`, `classes7.dex`, `classes8.dex` — 60,061,368 bytes total.
- Class counts: 7,562 / 1 / 11,365 / 9,730 / 12,876 / 8,184 / 20,519 / 3,483. Total 73,720.
- `classes2.dex` is 868 bytes and holds a single class. It is a leftover empty multidex slot;
  nothing in this build resolves through it.
- App-owned classes (`Lcom/pinterest/`, `Lcom/linecorp/`, `Linfo/mqtt/android/`) by DEX:
  `classes.dex` 1,625 / `classes3.dex` 676 / `classes4.dex` 751 / `classes5.dex` 5,593 /
  `classes6.dex` 3,112 / `classes7.dex` 2,181 / `classes8.dex` 161. Total 14,099.
- 64 native libraries, 137,722,368 bytes, for `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.
- Notable native libraries: `libquikklycore.so` and `libquikklycore-jni.so` (Pinterest's own
  QUIC stack), `libcronet.143.0.7445.0.so` (HTTP), `libbugsnag-ndk.so` plus its
  `plugin-android-anr`, `root-detection` siblings (crash reporting), `librive-android.so`,
  `libxrenderer.so`, `libzune_jpeg-*.so`, `libsurface_util_jni.so`, `libdatastore_shared_counter.so`.
- `res/` holds 7,182 entries. `assets/` holds 46, including a 94,534-byte `dexopt/baseline.prof`,
  `adChoicesRemoval.js` (6,565 bytes), `om_static.js` (49,724 bytes), `pinmarklet.js`, and eight
  `real_feed_*.jsonl` files totalling roughly 22 MB.
- The manifest declares 32 permissions. Notable ones: `AD_ID`,
  `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, `VENDING.BILLING`,
  `DETECT_SCREEN_CAPTURE`, `READ_CONTACTS`, `POST_NOTIFICATIONS`, and the custom
  `com.pinterest.account.Credentials` guarding `CredentialsContentProvider`.

## Obfuscation is partial and per-package

This is the single most useful structural fact about this build, and it differs from the
obfuscated targets in this repo.

- 14,052 classes sit under `Lcom/pinterest/`. 9,285 of them (66.1%) have short, obfuscated simple
  names; 4,767 are fully readable.
- Obfuscation is applied per package, not per build: every package checked is either 0% or
  100% short-named. `com/pinterest/identity/core/error` (62), `com/pinterest/api/model/deserializer`
  (51), and `com/pinterest/feature/core/view` (42) are entirely readable, while
  `com/pinterest/collage/effects` (74), `com/pinterest/feature/nux/usecasepicker` (50), and
  `com/pinterest/boardShopTool/sba` (50) are entirely obfuscated.
- Consequence for fingerprints: a readable package can be anchored on class name plus method
  signature, but an obfuscated package can only be anchored on literals, parameter and return
  types, access flags, and instruction shape. Never assume a readable class name exists.

## Application surface

- Base class for every activity is the abstract `Lcom/pinterest/baseActivity/a;`. This is the
  central per-activity object and the most useful single class in the build.
- `BotChallengeActivity` extends `Lcom/pinterest/baseActivity/a;` directly.
- `NUXActivity` (onboarding) extends it and is the only class named in the `autoAnalytics` check
  at `baseActivity.onResume`.
- Other readable activities: `MainActivity`, `CreationActivity`, `RepinActivity`, `CameraActivity`,
  `PinItActivity` (exported, handles `SEND` and `com.pinterest.action.PIN_IT`), `NavActivity`,
  `WebViewActivity`, `CommentActivity`, `MediaGalleryActivity`, `ComponentBrowserActivity`,
  `ScrapedImagesResultsActivity`, `UserSetImageActivity`, `WebhookActivity` (NDEF/VIEW),
  `SendShareActivity`, `ExperimentsReloaderActivity`, `AuthenticatorActivity`, `SSOActivity`.
- Services: `MessagingService` and `FirebaseMessagingService` both on `MESSAGING_EVENT`,
  `PinUploaderService`, `MqttService` (`info.mqtt.android.service.MqttService`), plus
  `AppMeasurementService` / `AppMeasurementJobService` (Firebase Analytics) and the WorkManager
  trio.
- Broadcast receiver `com.pinterest.engage.GoogleEngageBroadcastReceiver` is exported on
  `com.google.android.engage.action.PUBLISH_RECOMMENDATION`.
- Exported providers: only `com.pinterest.account.CredentialsContentProvider`, protected by the
  custom `com.pinterest.account.Credentials` permission.

## Base activity as a patch anchor

`Lcom/pinterest/baseActivity/a;` carries the infrastructure a patch usually wants:

- Analytics: `analyticsApi : Lg20/a;` with `getAnalyticsApi()`/`setAnalyticsApi()`;
  `pinalyticsFactory : Lx30/c;`; `pinalyticsScheduler : Lmi2/a;`; `getPinalytics()Lx30/b;`;
  `networkPinalytics : Lf20/z;`; `autoAnalytics : Z` with `getAutoAnalytics()`/
  `setAutoAnalytics()`; `trackingParamAttacher`; `timeSpentLoggingManager : Lv30/n;`.
- Ads: `adFormats : Lj33/a;` with `getAdFormats()`/`setAdFormats()`; `adDataEventData : Lqo2/e;`
  plus the constants `AUXDATA_IS_THIRD_PARTY_AD` and six `AuxDataKey_*` / `DL_AD_CLOSEUP_*` fields.
- Lifecycle and other hooks: `setupActivity()V`, `onCreate`, `onResume`, `onStart`, `onDestroy`,
  `init()V`, `injectDependencies()V`, `configureTheme()V`, `generateLoggingContext()Lqo2/x0;`,
  `getViewType()Lqo2/b8;`, `showToast`, `showError`, `showInlineAlert`, `showInlineEducation`.
- `setupActivity()V` is virtual on the base and called from the base's own `onCreate`, so it has
  no in-code callers of its own. Fingerprints must not rely on a caller of `setupActivity`.

## Verified candidate surfaces

### Disable the bot challenge

Cleanest surface found so far, and the most likely first patch.

- `Lcom/pinterest/securityChallenge/ui/BotChallengeActivity;` (classes3.dex), 20 classes in the
  package, public final, extends `Lcom/pinterest/baseActivity/a;`.
- `onCreate(Landroid/os/Bundle;)V` has 2 registers and exactly three instructions:
  `invoke-virtual inject()`, `invoke-super Lcom/pinterest/baseActivity/a;->onCreate`,
  `return-void`. Trivial to replace with an immediate `return-void`.
- `setupActivity()V` has 3 registers and 18 instructions. It loads
  `sget Luq2/b;->bot_challenge_host I` and passes it to `setContentView(I)`, sets
  `Window.setFlags(8192, 8192)` (`FLAG_SECURE`), registers a back-pressed callback through
  `new-instance Lcom/pinterest/securityChallenge/ui/b;`, and finishes with
  `const/4 v0, 1` + `D(Z)V`.
- `onNewIntent(Intent)` has 4 instructions: `getClass`, `invoke-super onNewIntent`,
  `setIntent`, then `const/4 v1, 0` + `D(Z)V`.
- `D(Z)V` and `E(String)V` are the internal navigation helpers; `getFragment()` returns
  `La0/f;`, which is constructed with `Lcom/pinterest/securityChallenge/ui/BotChallengeActivity;`
  as its only parameter, and `Lop0/b;->H(BotChallengeActivity, Lpr/b9;)V` is the presenter.
- Unverified: making the activity a no-op leaves the caller that started it without a result, so
  a complete patch also has to short-circuit whatever launched the challenge. That launch site is
  not yet identified, and `D(Z)V` is the likely place the completion is signalled.

### Skip onboarding NUX

- `Lcom/pinterest/activity/nux/NUXActivity;` (classes.dex) is fully readable and large.
- Useful methods: `goHome()V`, `dismissExperience()V`, `completeExperience()V`, `exitNUX()V`,
  `logNuxStart(Lbp0/y;)V`, `logNuxEnd(Lbp0/y;)V`, `goToStep(...)V`, `restoreStepIndex(Integer)V`,
  `incrementAndGetNUXStep()`, `decrementNUXStep()`.
- `goHome()V` is 11 instructions: it puts the boolean extra
  `com.pinterest.EXTRA_REQUEST_LOCATION_PERMISSION` with value `1` on the intent, calls
  `Lz72/c;->g(Activity, Z)V`, then `finish()`. Its only two callers are `goToStep` (index 176)
  and `dismissExperience` (index 98), both inside `NUXActivity`.
- `dismissExperience()V` resolves the current experience from `getExperiences()` plus
  `getPlacement()` with a `LinkedHashMap` fallback before calling `Lbp0/y;->c()V`.
- The supporting package is `Lcom/pinterest/feature/nux/` (163 classes), which is a mix: the
  `usecasepicker` sub-package is 100% obfuscated.

### Analytics

- `baseActivity.onResume()` reads `autoAnalytics` at instruction 17, compares against
  `const/4 v2, 0` at 18, branches at 19, then excludes `NUXActivity` via `instance-of` at 20 and
  calls `getPinalytics()` plus `Lx30/b;->t(HashMap)` at 22-24. Setting `autoAnalytics` false
  suppresses exactly that one resume event and nothing else.
- `setAutoAnalytics(Z)V` has exactly one caller:
  `Lcom/pinterest/activityLibrary/activity/task/activity/MainActivity;.onCreate(Bundle)V` at
  instruction 672.
- The field is read in only four methods total: `baseActivity.<init>`, `getAutoAnalytics`,
  `onResume`, and `setAutoAnalytics`. So this boolean is narrow, not a master switch.
- `baseActivity.setContentView(I)V` is 5 instructions and wraps every activity layout in the
  `baseActivityLayout : Landroid/widget/FrameLayout;` field, inflating into it rather than
  replacing the decor content view.
- A readable analytics surface worth noting: `Lcom/pinterest/component/board/view/BoardRep;` is
  fully readable and its `markImpressionEnd()Ljava/lang/Object;` builds a 19-register
  impression proto including the literal `board_id`.

### Ads

Weakest area so far, and the one most likely to need real work.

- No global ad kill-switch literal exists. `ads_enabled`, `enable_ads`, `hide_ads`, `no_ads`,
  `ad_free`, `adBlock` and variants return nothing across all eight DEX files.
- The literal `ad_block` exists exactly once, at `Lads_mobile_sdk/jp;-><clinit>()V` in
  classes3.dex. That is obfuscated Google Mobile Ads SDK internals, not Pinterest code, so it is
  a poor fingerprint and a poor patch target.
- `ads_offramp_ads_only` looks promising by name but is not an ad gate.
  `Lfq0/c;->b(String, Lfq0/w0;)Z` is 7 instructions that forward the literal to
  `Lfq0/a0;->h(String, String, Lfq0/w0;)Z`, i.e. it selects an analytics filter for an
  "ads only" offramp session. Four sibling methods in `Lh03/h0;` and the key provider
  `Let1/b;->a()String` use it the same way.
- `is_sponsored_content` is used in four places, and the only readable one is
  `BoardRep.markImpressionEnd()`; the rest are protobuf serialization in the obfuscated
  `Lcom/pinterest/api/model/s7$b;` and a map builder at `Lhg2/b;->q(...)`.
- Pinterest-owned ad code is spread over many small packages rather than one module:
  `Lcom/pinterest/ads/` (184), `Lcom/pinterest/adPreview/` (69, mostly obfuscated),
  `Lcom/pinterest/adFormatsLibrary/` (11), `Lcom/pinterest/adsGmaLibrary/` (2),
  `Lcom/pinterest/adsStlUiLibrary/` (2), `Lcom/pinterest/adsWebViewPin/` (9),
  `Lcom/pinterest/adsOpenMeasurement/` (2), `Lcom/pinterest/adsCollageHeroCutout/` (50),
  `Lcom/pinterest/pinBoost/` (40), `Lcom/pinterest/bundledCart/` (36).
- `Lcom/pinterest/ads/screen/AdsLocation;` is readable and enumerates 17 surfaces
  (`ADS_CORE`, `ADS_SHOPPING`, `ADS_COLLAGE`, `ADS_DEBUGGER`, `ADS_STORY`, `LEAD_GEN_COUNTRY_MODAL`,
  `WEIGHT_LOSS_OPT_OUT_MODAL`, and so on). It is an analytics location enum, not a gate, but it is
  the best available map of where ads appear.
- The central rendering entry point has not been located. Until it is, an ad patch should be
  expected to need several narrow edits rather than one.

## Not yet done

- No Pinterest patch, fingerprint, or patch-list entry has been written.
- No Gradle build, bundle application, or device test has been run.
- The bot-challenge launch site, the ad render entry point, the notification/push gate, the
  cookie-consent entry point, and the `feature/settings` preference keys are all unmapped.

## Why the Phase 0 settings patches cannot work on this build

The four "Morphe settings" patches that a Pinterest patch bundle is expected to carry (settings
entry, settings screen label, settings screen manifest, runtime state) were traced to their
counterparts in the official bundle, `MorpheApp/morphe-patches`. They are implemented on top of
`androidx.preference`, and this build has none of it:

- `Landroidx/preference/` contributes **0** classes, and no method anywhere in the APK references
  any type in that package.
- `res/xml/` holds 10 entries and none is a preference screen: `authenticator.xml`,
  `file_provider_paths.xml`, `ga_ad_services_config.xml`, `gallery_wall_widget_info.xml`,
  `locales_config.xml`, `network_security_config.xml`, `single_image_widget_info.xml`,
  `splits0.xml`, and two AppsFlyer backup/data-extraction files.
- `Landroidx/compose/` contributes 221 classes, so the settings UI is Compose-based.

The official mechanism, in outline, is a `resourcePatch` that copies the Morphe preference
resources in and rewrites one existing `strings.xml` element's `textContent` to `"Morphe"`, plus a
`bytecodePatch` that hooks `PreferenceManagerLegacyFingerprint` and
`PreferenceDestinationLegacyFingerprint` to insert the row and intercept its navigation into a
settings activity supplied by an extension. All of those hooks need a `PreferenceFragment`, a
`PreferenceScreen` XML document and an `res/xml/*_prefs.xml`. Pinterest's settings live in
`Lcom/pinterest/feature/settings/` (1,183 classes) behind a custom UI, so there is nothing for
those fingerprints to match.

This template also has none of the supporting framework: no `extensions/` directory, no
`app.morphe.patches.shared.*` or `app.morphe.patches.all.*`, no `app.morphe.util.*` DOM helpers and
no settings resources. Porting the framework would not fix the missing `PreferenceFragment`.

Decision taken: defer the settings UI, start the patches that do not depend on it, and treat the
settings screen as its own piece of work once the set of toggles that are actually wanted is
known. Every patch written in the meantime is a plain `default = true` toggle, which is already
how every patch in this repository behaves.

## Advertising identifier

### The app-owned wrapper

`Lvi2/b;` in classes3.dex is Pinterest's own wrapper around Google's advertising identifier. Its
three methods are `a()`, `b(Context)` and `c(Activity, boolean, int)`.

- `b(Context)` returns the `AdvertisingIdClient$Info`. It calls `c(...)` first, which runs a
  consent and permission-request flow built on `Lxi/c;` and `Lxi/d;`, returning `false` and so a
  `null` identifier when the user declines. Otherwise it calls
  `AdvertisingIdClient.getAdvertisingIdInfo(Context)`, stores the result in the field `b`, and
  returns it. A `catch` handler logs `Log.getStackTraceString` and returns the cached field.
- `a()` is the cached getter. It returns the field when it is set and otherwise builds an
  `Lvi2/a;`, launches it through `Lhi0/c;->a()V`, and returns `null`.
- `c(Activity, boolean, int)` is the consent flow and has no callers other than `b(Context)`.

`a()` has **eight** callers, which is what decides the shape of the patch:

```
Lf20/d0;.C(Lnb1/h;)V                                  classes.dex
Lf20/d0;.R(Lqo2/e2;String;ArrayList;HashMap;...)V       classes.dex
Lf20/d0;.q(Lqo2/b8;Map;)V                             classes.dex
Lf20/k0;.a(Lf20/k0;Lx30/a;HashMap;)V                   classes.dex
Lv70/g;.intercept(Lr93/b0;)Lr93/r0;                    classes.dex
Lm21/b;.u(Lt30/n;)V                                    classes5.dex
Lds1/b;.t3()V                                           classes6.dex
Ljv2/c;.b()V                                            classes7.dex
```

Because all eight read only the cache, a replacement that returns early without writing the field
would leave the cache permanently `null` and make every one of them relaunch the asynchronous
fetch on every call. The patch therefore replaces the fetch and leaves the caching in place.

### Consumers tolerate a missing or empty identifier

`Lur/j;.b()V` in classes.dex is the one consumer that has been read in full. It calls
`b(Context)`, then handles three separate degenerate cases before using the value: a `null`
`Info` at index 10, a `null` id at index 11, and an id of length zero at index 18. Only after all
three does it put the value into a `LinkedHashMap` under the literal `advertising_identifier` and
read `isLimitAdTrackingEnabled()`.

An empty identifier is therefore a state the app already has a path for, and it drops the
parameter from the request entirely. The patch still supplies a random UUID rather than an empty
string, because the other seven readers of the same cache have not been read and an empty
identifier is a legal `getId()` result that no reader has a particular reason to expect. A random
value in the usual shape cannot fault a caller, is not derived from the device or the account,
and is constant for the process.

### Paths this does not cover

`AdvertisingIdClient.getAdvertisingIdInfo` has six call sites in five methods, and only one of
them is Pinterest's. The rest belong to bundled third-party code and are excluded by their own
signatures:

| method | returns | parameters | verdict |
| --- | --- | --- | --- |
| `Lvi2/b;->b` | `AdvertisingIdClient$Info` | `(Context)` | the target |
| `Lads_mobile_sdk/ez;->w` | `Object` | `(Lk53/a;)` | Google Mobile Ads SDK |
| `Lcom/appsflyer/internal/AFe1eSDK;->getCurrencyIso4217Code` | `Z` | `(Context, AFe1eSDK$AFa1ySDK)` | AppsFlyer |
| `Luk/o2;->f2` | `Pair` | `(String)` | GMS measurement, called twice |
| `Luk/x0;->j` | `Z` | `()` | GMS measurement |

This is consistent with the patch list, which carries "Disable AppsFlyer tracking" and "Disable
third party trackers" as separate items. Neutralising the identifier at `Lvi2/b;` does not affect
the AppsFlyer, Google Mobile Ads or measurement paths, and those patches must not be assumed to
cover Pinterest's own use.

## Patch 1 — Neutralize advertising ID

- Compatibility: `com.pinterest`, version `14.38.0`, version code `14388010`, regular APK.
- `AdvertisingIdInfoFingerprint` matches on the return type
  `Lcom/google/android/gms/ads/identifier/AdvertisingIdClient$Info;`, the parameter list
  `(Landroid/content/Context;)` and a `methodCall` to
  `AdvertisingIdClient.getAdvertisingIdInfo(Context)`. Neither the obfuscated defining class
  `Lvi2/b;` nor the method name `b` is pinned.
- Verified uniqueness in the reference: the return type alone matches four methods
  (`AdvertisingIdClient->d()`, `AdvertisingIdClient->getAdvertisingIdInfo(Context)`,
  `Lvi2/b;->a()` and `Lvi2/b;->b(Context)`); adding the single-`Context` parameter list and the
  `getAdvertisingIdInfo` call filter leaves exactly one.
- The replacement is one `invoke-static` over one register, the same opcode and the same width as
  the instruction it replaces. `b(Context)` branches at index 4 and index 9, so a wider
  replacement would shift its branch targets. The register is read from the matched invoke with
  `ThirtyFiveCInstruction.getRegisterC(0)` rather than hardcoded, because a 35c invoke carries its
  arguments in the C slots and has no receiver.
- The synthetic value is cached by the app's own `move-result-object` and `iput-object` at indices
  7 and 8, so the eight readers of the cache keep working.
- The extension exists because the logic cannot be expressed inline at the same width: smali has
  no way to construct an `AdvertisingIdClient$Info` inside one `invoke-static`, and widening the
  replacement is not safe in a method that branches.
- Static validation passed: `tools/checks/patch_smali_checks.py` reports 0 problems across 17
  files including the three new ones, and `tools/checks/test_invoke_arity.py` passes 19/19. This
  covers the invoke arity and the escaped `$` in `AdvertisingIdClient$Info`.
- Not verified: the Gradle build, because `app.morphe.patches` `1.3.4` cannot be resolved from
  `maven.pkg.github.com` with the available token, and `extensions/` is the first extension module
  in this repository, so its wiring is unproven here. The extension also has never been run on a
  device, and the `AdvertisingIdClient.Info` constructor has not been checked against a compiled
  artifact.

## AppsFlyer

### Layout

432 classes under `Lcom/appsflyer/`, split 280 in classes.dex and 152 in classes4.dex. The public
API is not obfuscated (`AppsFlyerLib`, `AFLogger`, `AppsFlyerConsent`, `AppsFlyerProperties`,
`AFInAppEventType`, `PurchaseHandler`, the `deeplink` and `share` packages), while
`Lcom/appsflyer/internal/` is renamed to an `AF<letter>1<letter>SDK` scheme. Member names inside
those classes are obfuscated too, including **method** names: several carry the readable names of
completely unrelated SDK methods, so a method name in this package is never a reliable signal.

`assets/com/appsflyer/internal/` holds six asset files.

### The concrete implementation

`Lcom/appsflyer/AppsFlyerLib;` is abstract and declares `init`, `start`, `logEvent`, `stop`,
`onPause` and `getInstance`. Exactly one class extends it:

```
Lcom/appsflyer/internal/AFa1tSDK;  extends  Lcom/appsflyer/AppsFlyerLib;
```

That is where every real method body lives. **This class name is the one genuinely fragile element
in the patch**, because it is obfuscated and changes when the SDK is updated. Everything else in
the fingerprints is the exact signature and the `public final` access flag.

### What Pinterest actually calls

Twelve `AppsFlyerLib` members are called from Pinterest code, from five call sites:

| member | Pinterest call site |
| --- | --- |
| `init(String, AppsFlyerConversionListener, Context)` | `Lvs1/k0;.invoke(Object)Object` ins 2878 |
| `setAdditionalData(Map)` | `Lvs1/k0;` ins 2826 |
| `setSharingFilterForPartners(String[])` | `Lvs1/k0;` ins 2848 and 2868 |
| `setConsentData(AppsFlyerConsent)` | `Lvs1/k0;` ins 2976, 2996 and 3016 |
| `setCustomerIdAndLogSession(String, Context)` | `Lvs1/k0;` ins 3100 |
| `logEvent(Context, String, Map)` | `Lvs1/k0;` ins 3110 and `Lw20/h;.a(String, Map)V` ins 50 |
| `start(Context)` | `Lvs1/k0;` ins 3116 |
| `getAppsFlyerUID(Context)` | `Lvs1/k0;` ins 3130 |
| `getInstance()` | `Lvs1/k0;` 2724, `Lnf/h;.v()V` 28, `Lw20/h;` 42, `MessagingService.onNewToken` 46 |
| `unregisterConversionListener()` | `Lnf/h;.v()V` ins 36 |
| `stop(boolean, Context)` | `Lnf/h;.v()V` ins 44 |
| `updateServerUninstallToken(Context, String)` | `MessagingService.onNewToken` ins 62, `Lvr/g;.invoke(Object)` ins 2598 |

The initialisation sequence is one Kotlin lambda, `Lvs1/k0;.invoke(Object)Object`, which calls
`getInstance` and then works down the list in order. Patching the lambda would mean editing a
coroutine body thousands of instructions long, so the SDK side is patched instead.

### The transmit path, and why `init` alone is not enough

`AFa1tSDK.init` is only ten instructions. It marshals its three arguments into an `Object[]` with
`filled-new-array`, loads two large integer constants, calls
`System.identityHashCode`, and hands all of that to a private static. Returning early loses
nothing.

Suppressing `init` alone would also crash. The SDK's core is fetched through the accessor
`AFAdRevenueData()Lcom/appsflyer/internal/AFc1dSDK;`, and `getAppsFlyerUID` uses it like this:

```
 8  invoke-virtual   AFAdRevenueData()Lcom/appsflyer/internal/AFc1dSDK;
 9  move-result-object v0
10  invoke-interface v0, Lcom/appsflyer/internal/AFc1dSDK;->copy()Lcom/appsflyer/internal/AFd1pSDK;
```

There is no null check between the fetch and the dereference, and Pinterest calls
`getAppsFlyerUID` itself at `Lvs1/k0;` ins 3130. With `init` suppressed the core would be null and
that read would throw.

### R8 noise

Every public method in `AFa1tSDK` is wrapped in the same pattern before doing anything:

```
sget  AFa1tSDK;->AFInAppEventParameterName I
add-int/lit8   v0, v0, 103
rem-int/lit16  v0, v0, 128
sput  AFa1tSDK;->AFInAppEventType I
```

The three obfuscated member names in that sequence are static ints used as junk counters; they
say nothing about the method. `getAppsFlyerUID` is 41 instructions of which roughly half is this.
Any fingerprint built from that traffic would be pinning obfuscation artefacts, so none is.

## Patch 2 — Disable AppsFlyer tracking

- Compatibility: `com.pinterest`, version `14.38.0`, version code `14388010`, regular APK.
- Six methods on `Lcom/appsflyer/internal/AFa1tSDK;` are replaced with a type-correct immediate
  return: `init` returns `this`, `start`, `logEvent`, `setCustomerIdAndLogSession` and
  `updateServerUninstallToken` return `void`, and `getAppsFlyerUID` returns `null`.
- Every replacement is `addInstructions` at index 0, which is what the existing patches in this
  repository already do on branching methods. The bodies become unreachable but the branch targets
  inside them are recalculated by the patcher.
- `init` returns `v0`, which is `this`: the method has four registers and all four are declared
  parameters. A plain register is used rather than smali's `p0`. `p0` is accepted by the patcher's
  smali renderer, as the official bundle uses it, but nothing in this repository's checks exercises
  a parameter register, so the checked spelling is the one the existing patches already use.
- Verified in the reference that each fingerprint resolves to exactly one method. `start` has three
  overloads and `logEvent` has two; the parameter list separates them and the extra overloads are
  left alone deliberately.
- `getInstance` is untouched. It is the singleton accessor, every call site chains on it, and
  AppsFlyer's own internals use it, so suppressing it would break the SDK's plumbing without
  stopping anything.
- `setAdditionalData`, `setSharingFilterForPartners` and `setConsentData` are untouched: they only
  mutate local configuration that the suppressed methods would have read. `stop` and
  `unregisterConversionListener` are untouched because they only tear the SDK down.
- `getAppsFlyerUID` returning `null` is safe because `Lur/j;->b()V` tests an identifier for `null`,
  then again for `null` after the getter, then for zero length, before using it.
- No extension is used, so this patch does not depend on the untested `extensions/` wiring.
- Static validation passed: `tools/checks/patch_smali_checks.py` reports 0 problems across 19
  files including the two new ones, and `tools/checks/test_invoke_arity.py` passes 19/19.
- Not verified: the Gradle build, and any device run. `init` is the method most likely to change
  between AppsFlyer releases, and the whole patch hangs off one obfuscated class name.

## Google Engage

### Layout

The Google Engage SDK is bundled: about 90 classes under `Lcom/google/android/engage/` in
classes4.dex, covering `audio`, `books`, `common`, `food`, `service`, `shopping`, `social` and
`video` datamodels. `Lcom/google/android/engage/service/` holds only `AppEngageException`,
`ClusterList` and `ClusterMetadata`; the transport itself is the obfuscated
`Lcom/google/android/gms/internal/engage/zzp;`.

Pinterest's own code is two classes, both in classes.dex, both with readable names:

```
Lcom/pinterest/engage/GoogleEngageBroadcastReceiver;   extends Lec2/a;
Lcom/pinterest/engage/GoogleEngageWorker;              extends Landroidx/work/RxWorker;
```

The manifest registers the receiver as exported on
`com.google.android.engage.action.PUBLISH_RECOMMENDATION`, `com.pinterest.unauth.ACTION_USER_LOG_IN_SUCCESS`
and `com.pinterest.unauth.ACTION_USER_LOG_OUT_SUCCESS`.

### The receiver only enqueues a job

`GoogleEngageBroadcastReceiver.onReceive(Context, Intent)` is 49 instructions, 15 registers, two
declared parameters and one declared parameter used nowhere. It does not read the intent action.
Its whole body is:

```
 2  invoke-static   Ld63/e0;->w()V
 5  new-instance    LinkedHashSet
21  invoke-direct   Lbd/e;-><init>(NetworkRequest, State, Z Z Z Z, J J, Set)V
23  const-class     Lcom/pinterest/engage/GoogleEngageWorker;
25  const-string    "google_engage_one_time_publish_job"
29  invoke-virtual  Lbd/d0;->m(Lbd/e;)Lbd/s0;
32  sget-object     Lbd/a;->EXPONENTIAL
33  const-wide/16   30000
34  invoke-virtual  Lbd/s0;->l(Lbd/a; J)Lbd/s0;
47  invoke-virtual  ... ->c(String, KEEP, Lbd/e0;)V
48  return-void
```

So the receiver is not where publishing happens, it is what schedules publishing. `google_engage_one_time_publish_job`
occurs **exactly once in the whole APK**, in this method.

### The worker is the thing that publishes

`GoogleEngageWorker` extends `androidx.work.RxWorker` and overrides its `doWork`, here named `g()`
because R8 renames methods in this app. It returns `Lc43/v;`, an obfuscated RxJava `Single`, and is
28 instructions of pure construction: a `Context`, a `Schedulers` selector at index 19, a
`CompositeException` holder, an `o1/o1` collaborator taking the literal `25`, a retry/timeout
selector at `14` and `15`, and two `flatMap`/`compose`-shaped wrappers, finishing with
`subscribeOn(Lb53/f;->c)`. No literal identifies it, and every collaborator is obfuscated, so this
method is the weakest target in the bundle so far.

`GoogleEngageWorker` is constructed in exactly two places and neither schedules it: `Lpr/n9;` is
the WorkManager `WorkerFactory` that rebuilds a worker, and `Lpo0/f;` holds the injected instance.
The only `const-class` for it in the APK is index 23 of the receiver's `onReceive`. The receiver is
therefore the only enqueue point, which is what makes suppressing it sufficient on its own.

## Patch 3 — Disable Google Engage

- Compatibility: `com.pinterest`, version `14.38.0`, version code `14388010`, regular APK.
- `GoogleEngageReceiverFingerprint` pins `Lcom/pinterest/engage/GoogleEngageBroadcastReceiver;`,
  which is **not** obfuscated, the `onReceive` signature, and the literal
  `google_engage_one_time_publish_job`.
- Verified in the reference: the APK has 33 methods with the signature `onReceive(Context, Intent)V`
  and exactly one of them contains that literal, so the filter is unique. The surrounding WorkManager
  types are all obfuscated (`Lbd/d0;`, `Lbd/e;`, `Lbd/e0;`, `Lbd/a;`) and none is used as an anchor.
- The replacement is `addInstructions(0, "return-void")`. `onReceive` returns `void`, so no value
  register is involved, and neither parameter is read by the original body.
- This is the first patch in the bundle that needs no obfuscated anchor at all, and the first that
  needs no extension either.
- Static validation passed: `tools/checks/patch_smali_checks.py` reports 0 problems across 21
  files including the two new ones, and `tools/checks/test_invoke_arity.py` passes 19/19.
- Not verified: the Gradle build, and any device run.

## Phase 0 — runtime state, and a bug in the official version check

### What the runtime state patch is

`patches/src/main/kotlin/app/pinterest/patches/shared/VersionState.kt`. It is an **unnamed**
`bytecodePatch`, which matters: `PatchLoader` only loads patches that carry a name, so an unnamed
patch is invisible in the Manager's patch list and cannot be toggled, while still being usable as a
`dependsOn` target by every other Pinterest patch. It changes no code.

The official bundle does this as `app.morphe.patches.<app>.misc.version.VersionCheckPatch`. It reads
`context.packageMetadata.versionName` inside `execute`, which is the real API:
`PackageMetadata` exposes `packageName`, `versionName` and `versionCode` as `String`, with
`versionCode` notably a **string**, not an integer.

All three Pinterest patches that exist now declare `dependsOn(versionCheckPatch)`, so the version
is recorded before their own `execute` blocks run.

### The official comparison is wrong for Pinterest's own version numbers

The official helper is:

```kotlin
fun isEqualsOrGreaterThan(version: String): Boolean = versionName >= version
```

That is Kotlin's string comparison. It is wrong as soon as a component reaches two digits, which
Pinterest passed long ago. Checked against real Pinterest version numbers:

| patched version | compared to | correct | official `>=` |
| --- | --- | --- | --- |
| `14.38.0` | `14.38.0` | true | true |
| `14.38.0` | `14.9.0` | **true** | **false** |
| `14.9.0` | `14.38.0` | **false** | **true** |
| `14.38` | `14.38.0` | **true** | **false** |
| `14.100.0` | `14.38.0` | **true** | **false** |
| `14.38.0` | `14.38.1` | false | false |
| `15.0.0` | `14.99.99` | true | true |
| `14.38.0` | `14.38.0.1` | false | false |

Four of eight are inverted, in both directions. `14.38.0` sorts below `14.9.0` as text because
`'3'` is less than `'9'`, so any predicate written against `14.9.0` or `14.10.0` would be
permanently false and any predicate written against `14.38.0` would also be false on `14.38.0`
itself once compared against a shorter form. The failure is silent: the patch simply takes the
other branch.

`compareVersions` in `VersionState.kt` compares component by component instead. A missing
component counts as zero, so `14.38` equals `14.38.0`. A component that is not numeric reads as
zero rather than throwing, so an unexpected version string degrades to a numeric-prefix comparison
instead of aborting the whole patch run.

Verified by reimplementing the same algorithm and running the table above: all eight cases agree
with the correct answer. That validates the logic, not the Kotlin, which has not been compiled.

### Only one predicate exists

`is_14_38_0_or_greater`, matching the single `AppTarget` in `COMPATIBILITY_PINTEREST`. No
speculative predicates for versions this bundle does not target were added, per `AGENTS.md`.

## Phase 0 — status

| item | state |
| --- | --- |
| Morphe runtime state | **done** |
| Settings entry | blocked |
| Settings screen (label) | blocked |
| Settings screen (manifest) | blocked |

The three settings items remain blocked for the reason recorded earlier: they hook
`androidx.preference`, which Pinterest 14.38.0 does not contain. Building them would require a
bespoke settings activity, an anchor inside Pinterest's own Compose settings list, and a set of
string resources, none of which can be compiled or device-tested in this environment.

The runtime state patch does **not** make a failing fingerprint survive an app update. It makes the
patch able to know which version it is running against, which is the prerequisite for noticing that
a fingerprint has stopped matching.


## Third party trackers: the description does not match this build

Enumerated the SDKs actually bundled rather than trusting the patch description, which names
"Google, MoPub, Adjust, Nielsen, Segment, etc.".

**Absent entirely** (0 classes): MoPub, Adjust, Nielsen, Segment, the Facebook SDK, AppLovin,
Unity Ads, IronSource, Vungle, Pangle, Mintegral, Appbrain, Appodeal, Branch, Singular, Amplitude,
Mixpanel, Firebase Crashlytics, Google Tag Manager.

**Present**:

| SDK | classes | dex | disposition |
| --- | --- | --- | --- |
| Google Mobile Ads SDK | 3,337 | classes.dex | advertising, not tracking; belongs to the "Disable ads" item |
| Google Measurement (Firebase Analytics) | 289 | classes4.dex | self-initialising, see below |
| AppsFlyer | 432 | classes.dex + classes4.dex | already patch 2 |
| Google Engage | 85 | classes4.dex | already patch 3 |
| Bugsnag | 172 | classes.dex | crash reporting, not an advertising tracker |

Other bundled third-party code is not tracking: `com/bumptech/glide` (43, image loading),
`com/airbnb/lottie` (35, animation), `com/linecorp/linesdk` (41, Line login),
`com/amazonaws/*` (about 290, Amazon Shopping).

So for this build the two trackers that are actually disableable are AppsFlyer and Google Engage,
and both are already implemented as separate patches.

### Pinterest never calls the measurement package

Checked every method in the APK for calls into the obfuscated GMS measurement package:

- Pinterest methods calling anything in `Luk/`: **0**.
- Callers of `AppMeasurement.logEventInternal(String, String, Bundle)`: **0**.
- Callers of `Luk/a2;->e(String, String, Bundle)`, the internal event dispatch it calls: **0**.

`AppMeasurement.getInstance(Context)` is 51 instructions and returns a live object that the
manifest-declared `AppMeasurementService`, `AppMeasurementReceiver` and
`AppMeasurementJobService` all depend on, so it cannot be made to return `null`. The SDK is
driven entirely by its own manifest components and content providers through
`androidx.startup.InitializationProvider`, not by Pinterest code.

There is therefore no clean Pinterest-side gate for it. Disabling it would mean either patching
`Luk/` internals, whose uploader has not been identified — `Luk/j0;` turned out to be a protobuf
and `Uri` helper class rather than the uploader — or removing manifest components, which is a
resource patch and changes app startup behaviour. Neither is a narrow, verifiable edit, so no patch
was written for it.

**Conclusion**: "Disable third party trackers" as described does not map onto Pinterest 14.38.0.
Its real content is already covered by patches 2 and 3. The Google Mobile Ads SDK is
"advertising" rather than a tracker and belongs to the "Disable ads" item, whose central render
entry point is still unmapped.

## Pinterest's settings UI is workable, just not Preference-based

The blocker recorded earlier is specific: there is no `androidx.preference`. What there is instead
is a typed, View-based settings list with readable row classes, which is a better anchor than a
`PreferenceFragment` would have been.

```
Lcom/pinterest/feature/settings/menu/SettingsMenuFeatureLocation
    $SETTINGS_MAIN
    $SETTINGS_APP_ABOUT
    $SETTINGS_PRIVACY_MODAL
Lcom/pinterest/feature/settings/shared/view/
    SettingsListActionItemView      <- a row that navigates to a sub-page
    SettingsToggleItemView
    SettingsTextItemView
    SettingsPageItemView
    SettingsSectionHeaderView
    SettingsHeaderSubHeaderItemView
Lcom/pinterest/feature/settings/menu/model/     <- 36 classes, ALL obfuscated (a..l, each with 0/1)
```

The list package holds only two readable classes, `SettingsMenuFeatureLocation` and
`AccountSwitcherFeatureLocation`; `Lcom/pinterest/feature/settings/menu/a` and `b` are the
obfuscated builders. `SETTINGS_MAIN` is referenced from exactly one place,
`SettingsMenuFeatureLocation.<clinit>()V`, so it is a singleton screen location.

Binding works through `SettingsListActionItemView.h1(SettingsListActionItemView, model/h)`, called
from two builders, `Lnw/a;.e(Liu1/l;Ljava/lang/Object;I)V` and `Lh71/e;.e(...)V`. The view is
constructed by `Lak1/g;.invoke()V` through a ten-argument constructor that takes two
`Function1` click handlers, so the click behaviour is injected at construction rather than looked
up.

### What this means for the three blocked settings patches

The official mechanism hooks `PreferenceManagerLegacyFingerprint` and
`PreferenceDestinationLegacyFingerprint`, neither of which can match here. A Pinterest-native
equivalent would instead be:

1. **Entry**: append a row to the list that `SETTINGS_MAIN` renders. The append point is the
   obfuscated builder in `Lcom/pinterest/feature/settings/menu/`, which is the weak link — every
   candidate is a single-letter class in an obfuscated package.
2. **Label**: this half is unchanged and still straightforward. The official patch walks `res` for
   every `strings.xml` and rewrites one element's `textContent` by attribute name; the equivalent
   here is to add a string resource and point the new row at it. Nothing about the label depends on
   `androidx.preference`.
3. **Manifest**: a settings activity supplied by an extension, declared through the decoded
   manifest in a `resourcePatch`. The API to confirm is `document("AndroidManifest.xml")`, since
   the documented DOM helper is `document(String)` and `get(String, Boolean)` returns a `File` for
   decoded resource paths.

None of this is written. The blocker moved from "the mechanism is impossible" to "the entry anchor
is obfuscated and unverified", which is a smaller problem but still a real one.

## Settings entry: the anchor hunt

The `SETTINGS_MAIN` list that would carry a "Morphe" row was traced as far as it goes.

`SettingsMenuFeatureLocation$SETTINGS_MAIN` is referenced from exactly one place,
`SettingsMenuFeatureLocation.<clinit>()V`. `SettingsMenuFeatureLocation` itself is referenced from
exactly one place outside itself: a `Parcelable.Creator` at
`Lcom/pinterest/feature/board/d;.createFromParcel(Object)Object`, which round-trips it through
`valueOf(String)`. So the screen location is passed around as a parcel, not navigated to by name,
and there is no call site that names `SETTINGS_MAIN` to hook.

Rows are models in `Lcom/pinterest/feature/settings/menu/model/`, all obfuscated (`a` through `l`,
each with `0` and `1` subclasses). The navigable row `SettingsListActionItemView` is bound by
`h1(SettingsListActionItemView, model/h)` from exactly three callers: `Lnw/a;.e(...)V`,
`Lh71/e;.e(...)V` and `model/z;.e(...)V`. The type is dispatched by `getItemViewType(I)I` in
eight adapter classes, of which only `model/a0;` is in the settings package.

The view is constructed in exactly one place, `Lak1/g;.invoke()Object` (114 instructions, ten
constructor arguments including two `Function1` click handlers). `Lcom/pinterest/feature/settings/menu/a`
and `b`, the only other classes in the list package, are both empty marker classes with no fields
and no methods — not builders.

So the list is built by obfuscated code outside the settings package, dispatched through generic
adapters, and bound by three separate callers. No single seam names `SETTINGS_MAIN`,
`SettingsListActionItemView`, or any model in a way a fingerprint can pin without using an
obfuscated class name. That is where the hunt stops: the entry needs either a fingerprint on an
obfuscated builder, or a different strategy entirely, such as intercepting at the parcel boundary.

## Patch 5 — Morphe settings screen (label)

- Compatibility: `com.pinterest`, version `14.38.0`, version code `14388010`, regular APK.
- A `resourcePatch` that appends `<string name="morphe_settings_entry">Morphe</string>` to the
  decoded `res/values/strings.xml` through `document(String)`, which is a documented patcher API.
- Only the default locale is touched. The description promises all languages, and the official
  bundle does that by walking `res` for every `values-*/strings.xml`. No documented API enumerates
  the decoded resource tree from inside a patch — `get` addresses one file by name and
  `listApkEntries` lists the input APK — so the multi-locale walk is not written.
- A dedicated `res/values/morphe_settings.xml` would be tidier than appending to the shared file,
  but writing a file that does not exist in the reference hits the same unconfirmed-API problem,
  so the shared file is used.
- No extension and no obfuscated anchor. This is the one Phase 0 settings item that is finishable
  as described.
- Not verified: resource recompilation, because the patcher build does not run here.

## Patch 6 — Morphe settings screen (manifest)

- Compatibility: `com.pinterest`, version `14.38.0`, version code `14388010`, regular APK.
- A `resourcePatch` that adds an `activity` element for
  `app.oyasumi.extension.MorpheSettingsActivity`, `exported=false`, to the decoded manifest
  through `document("AndroidManifest.xml")`.
- That component name is a contract with the extension that does not exist yet: the settings
  activity must be written under exactly that name in the `app.oyasumi.extension` namespace for
  this declaration to resolve to anything. Until then this patch declares an activity with no code.
- No theme is declared, so the activity inherits the application theme (`@7F150341` in the
  reference) rather than guessing at a style that may not exist.
- `document("AndroidManifest.xml")` is an inference, not a confirmed pattern: the documented DOM
  helper is shown on `res/values/strings.xml` and no official example applies it to the manifest.
  Whether the rebuilt manifest still parses cannot be checked without the patcher build.
- Not verified: the manifest path, resource recompilation, and the missing activity.

### Settings entry: anchor hunt, completed without a seam

Traced to the end, in order:

1. `SETTINGS_MAIN` is referenced only from `SettingsMenuFeatureLocation.<clinit>()V`. The location
   class itself is referenced from one place: a `Parcelable.Creator` at
   `Lcom/pinterest/feature/board/d;.createFromParcel(Object)Object`. It travels as a parcel, and
   nothing names it to navigate to.
2. `Com/pinterest/feature/settings/menu/a` and `b`, the only other classes in the list package,
   are both empty (no fields, no methods). Not builders.
3. The list models in `menu/model/` are all obfuscated single letters. `model/h` implements
   `Lmu1/s;` and declares exactly one method, `getViewType()I`. `model/a` declares nothing.
   Neither has an indexed constructor call, consistent with deserialized or server-shaped data.
4. The row binder `SettingsListActionItemView.h1(view, model/h)` is called from exactly three
   methods (`Lnw/a;`, `Lh71/e;`, `model/z;`), all generic multi-screen binders.
5. Eight adapters dispatch on the models' `getViewType()`. `Lex0/a;` is constructed from five
   unrelated places, including an onboarding/experiences flow, so it is shared, not
   settings-specific. `Lfz0/b;` has a single construction site, `Lhs/d;.invoke()Object`, but that
   method makes 329 invokes and 175 field references with zero settings-related targets — a generic
   factory, not a settings builder.
6. The row view is constructed in exactly one place, `Lak1/g;.invoke()Object`, a synthetic lambda.

No class in this chain names the settings screen, its list, or a Morphe-relevant seam in a way a
fingerprint can pin without using an obfuscated name. Appending a row needs the list at build
time, and the list is built by obfuscated code outside the settings package through generic
adapters. The entry patch is therefore not writable as a verifiable static patch from this
reference. The viable unblockers are dynamic analysis on a device to find the live builder, or
intercepting at the parcel boundary where the screen location is materialised.
