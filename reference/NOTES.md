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

- The ADM compatibility declaration and first two patches are written, but not compiled or applied yet.
- The native protection libraries may perform integrity or runtime checks outside the reach of a DEX patch.
- SDK providers may still initialize independently even after the app-owned ad entry points are skipped.
- The remote ad, Huawei, Firebase, and diagnostic paths may continue independently.
- Download and torrent behavior is constrained by servers, network conditions, Android background execution, and native code.
- A device-applied bundle test is still required for both patches and every later candidate.

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
- Not verified: Gradle compilation, which cannot resolve `app.morphe.patches` plugin `1.3.4` in this environment, and device application of the bundle.
- Unverified on device: that the AppBrain container is not left as a visible empty gap, and that no other ad SDK initialises independently.

