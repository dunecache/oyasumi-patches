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
- A second copy of the same APKM (`...apkmirror.com (1).apkm`) is intact: `base.apk` inflates to its full 90,777,380 bytes, and all eleven DEX files plus the whole `res/` tree are present, including the `classes8.dex` and `classes9.dex` that the first copy lost. The two files are byte-different and share a name, and only the `(1)` copy is usable.

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
- Local `:patches:compileKotlin` now works: use Java 21, the Gradle 9.7.1 wrapper, a `GITHUB_TOKEN` with `read:packages`, and `GITHUB_ACTOR=dunecache`, then run `./gradlew --stop` before building after any credential or environment change. A stale Termux Gradle daemon can otherwise reuse the old environment and keep reporting auth failures. The Android `:extensions:extension` module still needs a real SDK through `ANDROID_HOME` or `local.properties`, so a complete local bundle build remains unavailable without one.

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
- Every replacement instruction keeps the source register of the instruction it replaces. An earlier draft of this note also claimed the two methods holding a `packed-switch` payload (`Lv2/e3;->run()` at byte `0x052c` and `Lv2/p1;->run()` at byte `0x0c60`) kept that payload at its original 4-byte-aligned address. That was wrong, and is corrected under "Second device crash" below: `replaceInstructions` removed the two instructions after each invoke as well, so those methods shrank by 6 and 3 code units and their payloads did move. Both now remove the invoke on its own and pad it back to its original width, which keeps both method sizes unchanged.
- `Pref.U()` was additionally checked by a register liveness pass and by simulating the patched instruction stream. Those first passes were linear and could not model per-merge-point typing or block termination, and they were redone as a control-flow analysis with liveness and reaching definitions; the results are recorded under "The shared download constant does reach all three profiles". The simulation confirms all six download controls receive the intended bounds, the three chunk-size controls keep minimum 16 and maximum 961, and the patch introduces no uninitialised read and no int/object type violation on any instruction it writes. The four findings the simulation reports exist identically before and after patching and are in untouched app code, where a linear pass cannot model per-merge-point register typing.
- Note for future work: androguard's `Instruction.get_length()` returns a nibble count, not code units, so instruction addresses derived from it are wrong. The widths used above come from the DEX instruction format table instead.
- Verified in CI: the patch project compiles and the bundle builds and publishes as a release asset.

### Device crash in `Pref.U()` — root cause (v0.2.1 and later)

A patched 14.0.39 build dies with a hard verifier failure the moment the download
settings screen is built:

```
java.lang.VerifyError: Verifier rejected class com.dv.get.Pref: void com.dv.get.Pref.U()
failed to verify: void com.dv.get.Pref.U(): [0x5A] register v12 has type IntegerConstant
but expected Reference: f5.g
    at f3.j.j  at b3.c.b  at b3.c.run
```

Re-analysed against the pinned 14.0.39 DEX (`classes.dex`, `.registers 23`, 1 002 code
units). Measured facts, not inference:

- **The two register assumptions both hold.** `v5` is written at indices 5, 106, 246 and
  393 and read at none of them, so the `const/16 v5, 64` scratch is safe. `v7` is written
  at index 7 and only read from index 16 onward, so raising `const/4 v7, 5` to
  `const/16 v7, 32` is type-safe and reaches only the three `DOWN_LOADS_*` maxima.
- **The cause is `replaceInstructions`, not the registers.** Morphe's extension is
  implemented in `app.morphe.patcher.extensions.InstructionExtensions` as

  ```kotlin
  fun MutableMethodImplementation.replaceInstructions(index, instructions) {
      removeInstructions(index, instructions.size)
      addInstructions(index, instructions)
  }
  ```

  It removes as many instructions as it is given. The thread edit supplies two
  instructions, so it deleted index 33 *and* index 34. Index 33 is the intended
  `iput v8, v11, Lv2/j4;->b:I`, but index 34 is
  `iget-object v12, v0, Lcom/dv/get/Pref;->f:Lf5/g;` — the instruction that gives `v12`
  its `f5/g` reference type. With it deleted, `v12` keeps the `const v12, 2131755440`
  written at index 23, so `v12` is an `IntegerConstant` where the later
  `Lv2/j4;->h(Lf5/g; I Lv2/o4; Ljava/lang/String; ...)` invocation reads it. That is
  exactly the reported mismatch, and it explains why the message names `v12` and `f5/g`
  when neither register appears in the patch's own text.
- **Branch offsets were never the problem.** Branch targets in dexlib2 are `Label`
  objects bound to instruction identity, and offsets are only assigned when the method
  is written, so an insertion cannot invalidate them. `Pref.U()` has five branches, two
  of which cross the edit point (`if-eqz` at index 10 targeting index 289, and `if-nez`
  at index 12 targeting index 150, `new-instance v12, Lv2/j4;`); both stay correct
  because the labels move with their instructions. An earlier draft of this note blamed
  those two branches; that was wrong, and the real fault is the extra deletion above.
- The prior register liveness pass and patched-stream simulation both missed this
  because they checked the registers the patch *writes* and never checked which
  instructions the patch *removes*.

The fix removes the original store explicitly and then inserts, so exactly one
instruction is deleted:

```kotlin
fingerprint.method.removeInstruction(maximum.index)
fingerprint.method.addInstructions(maximum.index, "const/16 v5, 64\niput v5, v11, ...")
```

### Second device crash: `Lv2/p1;->run()` (v0.3.3)

With the `Pref.U()` fix in place, v0.3.3 still died on launch, this time before any
screen was built:

```
VerifyError: void v2.p1.run() failed to verify: [0x5FC] tried to get class from
non-reference register v0 (type=Conflict)
    at com.dv.get.Main.onCreate
```

`Lv2/p1;->run()` is the delayed-callback dispatcher that `DisableRatingPromptsPatch`
edits. The same `replaceInstructions` trap as the `Pref.U()` crash, reached from a
different direction. The patch passes three `nop`s to silence one `invoke-virtual`, and
that removes three instructions, so it deleted:

- 724 `invoke-virtual` — the intended target, three code units
- 725 `return-void` — the dispatch case's own return
- 726 `iget-object v0, v1, ...` — **the only assignment of a reference to `v0` on that
  path**

Without 726, `v0` holds a reference on the paths that kept the `iget-object` and something
else everywhere else, so the verifier sees a `Conflict` the first time `v0` is used as a
receiver. `DisableAdsPatch` has the identical defect in `Lv2/e3;->run()`: its first site
ate the second invoke outright, and both sites ate the `new-instance` that writes `v0`.

Measured, not inferred: each matched invoke is a 35c, three code units wide, and
`replaceInstructions` left `Lv2/p1;->run()` at 1 607 code units instead of 1 610 and
`Lv2/e3;->run()` at 668 instead of 674, so the "same width, so no payload moves" reasoning
in the original comments never held.

The fix removes the invoke on its own and pads it back to its original width, which keeps
both methods byte-for-byte the same size. `DisableAdsPatch` additionally reads both
indices before editing and applies them highest first, because removing one invoke and
adding three nops moves every later index up by two.

### `DOWN_THREADS_*` ceilings extended to all three profiles

The patch previously raised only the `DOWN_THREADS_3G` maximum, while its description
claimed "64 connections per download". `Pref.U()` contains three structurally identical
profile blocks. Every access to `Lv2/j4;->a:I` and `->b:I` in the method, in order, is:

| indices | control | value written to `b` |
| --- | --- | --- |
| 15, 16 | `DOWN_LOADS_3G` min/max | `v7` |
| 32, 33 | `DOWN_THREADS_3G` min/max | `v8` |
| 46, 47 | chunk size 3G min/max | `v8` / `v6` |
| 152, 153 | `DOWN_LOADS_WF` min/max | `v7` |
| 169, 170 | `DOWN_THREADS_WF` min/max | `v8` |
| 184, 185 | chunk size WF min/max | `v8` / `v6` |
| 299, 300 | `DOWN_LOADS_3GWF` min/max | `v7` |
| 316, 317 | `DOWN_THREADS_3GWF` min/max | `v8` |
| 330, 331 | chunk size 3GWF min/max | `v8` / `v6` |

Every access is an `iput`; there are no `iget` reads of either field, and nothing else
touches them between a profile's `DOWN_LOADS_*` key and its `DOWN_THREADS_*` store. So
each profile is now matched by a fingerprint anchored on its own `DOWN_LOADS_*` key and
then walking the next `a`, the next `b`, and that profile's `DOWN_THREADS_*` key, which
resolves to `21/32/33/38`, `158/169/170/175` and `305/316/317/322`. `string()` compares
with `StringComparisonType.EQUALS`, so `DOWN_LOADS_3G` cannot match `DOWN_LOADS_3GWF`.

Because each edit removes one instruction and adds two, the three stores are resolved
before any mutation and then applied from the highest index down, so no edit invalidates
an index a later edit still needs. `v5` remains the scratch register: it is written at
indices 5, 106, 246 and 393 and read at none of them, and materialising the value
immediately before each store means the result does not depend on what any register held
on the way there. Simulating all three edits gives 434 to 437 instructions, keeps every
neighbouring instruction intact, and puts `const 64` in front of all three thread maxima
while the chunk-size minimum stays 16 and the chunk-size maximum stays 961.

### The shared download constant does reach all three profiles

An earlier draft of this note recorded an open question: the simultaneous-download
ceiling is raised by editing the single shared `const/4 v7, 5` at index 7, on the stated
grounds that it reaches all three `DOWN_LOADS_*` maxima, and a control-flow pass appeared
to contradict that. The contradiction was the analysis's fault, not the patch's. A
control-flow graph built for `Pref.U()` was giving `return-void` a fall-through edge, so
the finished 3G block looked like it flowed into the WiFi block, and `v7`'s write at
index 103 inside the 3G block looked like a second reaching definition at the WiFi store.
With terminators modelled, the answer is unambiguous:

| store | value register | every definition that can reach it |
| --- | --- | --- |
| 16 `DOWN_LOADS_3G` max | `v7` | `const/4 v7, 5` |
| 153 `DOWN_LOADS_WF` max | `v7` | `const/4 v7, 5` |
| 300 `DOWN_LOADS_3GWF` max | `v7` | `const/4 v7, 5` |
| 33 / 170 / 317 thread max | `v8` | `const/16 v8, 16` |
| 46 / 184 / 330 chunk min | `v8` | `const/16 v8, 16` |
| 47 / 185 / 331 chunk max | `v6` | `const/16 v6, 961` |

Each store is fed by exactly one definition, so raising `v7` to 32 does reach all three
profiles, and the download half of the patch needs no change. The same table is why the
thread half cannot work the same way: `v8` feeds the three `DOWN_THREADS_*` maxima *and*
the three chunk-size minima, so raising `v8` would silently raise the minimum chunk size
from 16 as well. Materialising the value at each store avoids that, which is what the
three per-store edits do.

`v5` was also confirmed to be a genuinely dead scratch register by liveness rather than
by reading the disassembly: it is written at indices 5, 106, 246 and 393 and read at
none of them, and it is not live at 33, 170 or 317. Note that `filled-new-array` names
its destination array as the first register, so counting that as a read makes `v5` look
live and would have wrongly rejected the edit.

### `replaceInstructions` audit

`replaceInstructions(index, smali)` removes as many instructions as the replacement list
is long and then inserts that list. It is only correct when the list is exactly as long as
the span meant to be erased, *and* the list is the same width as that span. Both conditions
have now been violated and caused a launch crash. Every call site, re-checked:

| call site | before | now |
| --- | --- | --- |
| `IncreaseConnectionLimitsPatch.kt` (threads) | 2-instruction list over a 1-instruction `iput`: deleted the `iget-object` that gave `v12` its reference type | `removeInstruction` + `addInstructions`, one instruction removed |
| `DisableRatingPromptsPatch.kt` | `"nop\nnop\nnop"` over one 3-unit `invoke-virtual`: deleted the case's `return-void` and the `iget-object` that wrote `v0` | `removeInstruction` + `addInstructions`, method size unchanged |
| `DisableAdsPatch.kt` (two sites) | same, plus the first site deleted the second invoke outright | indices read up front, applied highest first, size unchanged |

No `replaceInstructions` call remains in the repository. `DisableHomeScreenAdsPatch.kt`
(1DM) only ever used `addInstructions`, which removes nothing.

`replaceInstruction` (singular) is unaffected: it replaces exactly one instruction, and is
used correctly by the download-ceiling, torrent-default and Telegram-gate edits. The lesson
is that a single-instruction edit must be expressed as a single-instruction operation, not
as a width-matched block of `nop`s handed to a helper that deletes by list length.

- Verified on device: the `0.2.1-dev.2` bundle applies cleanly to 14.0.39 on Android 15 with all three patches enabled, so every fingerprint resolves and every generated smali instruction assembles.
- Still unverified on device: the runtime effect of each patch. Applying successfully proves the fingerprints and encodings, not that ads are gone, that the sliders show the new bounds, or that no layout gap is left where the AppBrain container used to sit. Those need a manual pass.

# 1DM 18.2 reference notes

## Source and target record

- Reference: `/storage/emulated/0/Download/idm.internet.download.manager_18.2-30249_4arch_7dpi_85518970fbabdebca09caf183d786bde_apkmirror.com.apkm`
- SHA-256 of the APKM: `09e36d9356c8013c1eab3f0132b869ff3919f6194d1807d4c71f6bba2a581c7d`
- Size: `82,244,030` bytes. Format: APKM bundle (universal, 4 architectures, 7 densities), 18 entries.
- Package: `idm.internet.download.manager`. Version name: `18.2`. Version code: `30249`.
- Minimum SDK: `24`. Target SDK: `34`.
- Launcher activity: `idm.internet.download.manager.MainActivity` (also `LEANBACK_LAUNCHER`).
- Application class: `acr.browser.lightning.app.BrowserApp`. 1DM is a fork of the Lightning browser, so the shared view layer lives under `Lacr/browser/lightning/`.
- The manifest declares 32 permissions, including `AD_ID`, `ACCESS_ADSERVICES_TOPICS`, `ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_AD_ID`, `com.applovin.array.apphub.permission.BIND_APPHUB_SERVICE`, `com.android.vending.BILLING`, `SYSTEM_ALERT_WINDOW`, and `QUERY_ALL_PACKAGES`.
- Own services: `DownloadService`, `MediaScannerService`, `CheckAppVersion`, `IDMFirebaseMessagingService`, `TempFilesDeletionService`, `LogcatCaptureService`, and four quick-settings tile services.
- Bundled mediation stack: AdMob, AppLovin MAX, Unity Ads, IronSource, Chartboost, Vungle, Pangle, BidMachine, Moloco, MobileFuse, Smaato, InMobi, Bigo, MyTarget, PubMatic, Fyber, Verve, Mintegral, plus the Amazon APS banner (`com.amazon.device.ads`).
- The reference is user-supplied and has not been independently verified as the original publisher build.
- **A second, complete 18.2 artifact exists, and it is what everything below was re-derived
  from**: the installed app's own `base.apk`, at
  `/data/app/~~WHrUNUEWf-QcrR98fn1o6g==/idm.internet.download.manager-LYZjVSMnmWC9nmwe13xo4Q==/base.apk`.
  65,504,886 bytes, mode 0644, a single unsplit APK carrying all eleven DEX files
  (`classes.dex` through `classes11.dex`) and the whole of `res/`. Its manifest metadata is
  identical to the APKM's: `idm.internet.download.manager`, `18.2`, `30249`, minSdk `24`,
  targetSdk `34`, application class `acr.browser.lightning.app.BrowserApp`. This closes the
  "a complete `base.apk` is needed" gap recorded under unverified risks.
- **It is a different build of 18.2, not the same artifact.** Re-inflating the APKM's damaged
  `base.apk` up to the failure point and walking its local file headers shows most of its DEX
  files are byte-identical to this APK's under a renumbering, verified by SHA-256:

  | APKM entry | this APK | note |
  | --- | --- | --- |
  | `classes10.dex` | `classes3.dex` | identical |
  | `classes2.dex` | `classes4.dex` | identical |
  | `classes3.dex` | `classes5.dex` | identical |
  | `classes4.dex` | `classes6.dex` | identical |
  | `classes5.dex` | `classes7.dex` | identical |
  | `classes6.dex` | `classes8.dex` | identical |
  | `classes7.dex` | `classes9.dex` | identical |
  | `classes8.dex` | `classes10.dex` | same 8,706,412 bytes; the APKM copy is truncated by the damage |
  | `classes9.dex` | `classes11.dex` | never reached by the recovery; sizes agree |
  | `classes.dex` | `classes2.dex` | **same 10,713,976 bytes, different content** |

  Only the Lightning core differs: `BannerManager.load(Z)V` has 37 instructions in the APKM
  build and 39 in this one, and `BannerView.setAd` has 210 against 213. So these are two
  releases of 18.2 from the same publisher, not one release in two containers.
- Consequence for this patch: the three fingerprints that already existed were derived from
  the APKM build and were re-resolved against the on-device build. All four match, which is
  the evidence that they are not over-fitted to one build's instruction layout. Anything
  under `Lidm/`, and every layout, can only ever be checked against the on-device build,
  because the APKM's copies are exactly the DEX files its damaged download lost.

## Where the app's own code lives (on-device build)

The damaged-download inventory below is superseded for anything under `Lidm/`. On the
on-device build the split is:

| DEX | bytes | classes | contents |
| --- | --- | --- | --- |
| `classes.dex` | 142,764 | 3 | `R` and `BuildConfig` |
| `classes2.dex` | 10,713,976 | 9,198 | `Lacr/browser/**` (914), `androidx.compose/**` (5,195) |
| `classes11.dex` | 7,557,860 | 6,732 | `Lidm/internet/**` (669), `io.bidmachine/**` (3,357) |

So `Lidm/internet/download/manager/BannerView` and `manager/NewBannerView` are in
`classes11.dex`, `Lacr/browser/lightning/view/BannerManager` and `BannerView` are in
`classes2.dex`, and the Appodeal SDK itself is `classes5.dex` and `classes6.dex`. A scan of
app-owned classes in `classes.dex`, `classes2.dex` and `classes11.dex` is enough to cover all
of the app's own code.

## The reference file is damaged, and what that allowed

- The APKM is a corrupt download. `unzip` and `zipfile` both fail on the `base.apk` entry with `zlib.error: invalid distance code` after 68,812,800 of 90,777,380 bytes, and `split_config.armeabi_v7a.apk` fails with `invalid block type`. Every other entry inflates cleanly, so the container is intact and only two member streams are damaged.
- The `base.apk` deflate stream was decompressed manually up to the failure point (compressed offset 27,262,976 of 35,799,044), and the surviving prefix was walked entry by entry. Because deflate is a stream, everything decoded before the error is intact.
- Recovered from that prefix: `AndroidManifest.xml` (912,896 bytes, binary XML with an embedded resource table) and eight DEX files — `classes.dex`, `classes2.dex` through `classes7.dex`, and `classes10.dex`. Each recovered DEX matches the `file_size` in its own header and verifies against its embedded SHA-1 signature and Adler-32, so they are byte-exact copies, not approximations.
- Lost: `classes8.dex` (truncated at the damage point), `classes9.dex` (never reached), all `res/` layout and drawable XML, and every other entry stored after that offset in the ZIP.
- Consequence for this patch: **all** classes under `Lidm/internet/download/manager/` live in the two missing DEX files. The eight recovered DEX files contain 916 classes under `Lacr/browser/lightning/` and zero under `Lidm/`. `MainActivity`, the layout that hosts the banner, `Lidm/internet/download/manager/d` (the ad-configuration provider) and `Lidm/internet/download/manager/amazon/AmazonService` could not be read. Anything below that is inference, and is marked as such.

## DEX inventory (recovered only)

| DEX | bytes | classes | `Lacr/browser/lightning/` | `Li/*` |
| --- | --- | --- | --- | --- |
| `classes.dex` | 10,713,976 | 9,200 | 916 | 1,002 |
| `classes2.dex` | 182,952 | 137 | 0 | 0 |
| `classes3.dex` | 9,621,188 | 9,703 | 0 | 1,976 |
| `classes4.dex` | 8,227,912 | 7,609 | 0 | 631 |
| `classes5.dex` | 8,565,268 | 7,393 | 0 | 422 |
| `classes6.dex` | 8,499,332 | 10,434 | 0 | 929 |
| `classes7.dex` | 9,635,900 | 10,070 | 0 | 511 |
| `classes10.dex` | 7,434,716 | 8,555 | 0 | 1,625 |

- `Li/nu2;` (the settings/preferences class, 657 methods), `Li/ru;` (the banner model, 69 methods), `Li/x17;` (static helpers, 709 methods) and `Li/kk;` (the main-thread dispatcher) are in `classes4.dex`.
- `Lacr/browser/lightning/view/BannerManager;`, `Lacr/browser/lightning/view/BannerView;`, `Lacr/browser/lightning/view/BannerCallback;`, `Lacr/browser/lightning/view/DefaultBannerCallback;` and `Lacr/browser/lightning/view/BannerManager$1;` are in `classes.dex`. These names are unobfuscated, which is what makes them usable as fingerprint anchors.

## The home screen banner

- `Lacr/browser/lightning/view/BannerManager;` is a singleton (`INSTANCE`) with `mDisabled` and `mLoaded` (`AtomicBoolean`), `bannerInfoList` (`List`), `currentBannerInfo` (`Li/ru;`), `mTimer` (`Timer`) and `networkAdShowing` (`ConcurrentHashMap`). All field and method names in this class are unobfuscated.
- `BannerManager.load(Z)V` is `public synchronized`, 3 registers, 37 instructions, 2 try blocks. With its argument `true` it clears `mDisabled`, then fills `bannerInfoList` from `Lidm/internet.download/manager/d;->ۦۙۢ()Ljava/util/List;` and, when that list is empty, appends `Lidm/internet/download/manager/d;->ۦۜۡ()Li/ru;`, and finally sets `mLoaded` to `true`. It is the only writer of `bannerInfoList` other than `disable()`.
- `BannerManager.disable()V` is the app's own no-ads state: it sets `mDisabled` and `mLoaded` to `true`, clears `bannerInfoList`, nulls `currentBannerInfo`, and cancels `mTimer`.
- `BannerManager.resume()V` returns immediately when `mDisabled` is set, and also returns when `mLoaded` is false or `bannerInfoList` is empty. Otherwise it schedules `BannerManager$1` on a `Timer` with a 500 ms period. `MyAppCompatActivity.onResume()`/`onPause()` call `resume()`/`pause()`, so the timer restarts on every activity resume.
- `BrowserApp.lambda$initApp$2(Context)` is the only caller of `load()`: when `Li/x17;->ۦۤ۟(context)->Li/nu2;->ۦ۫ۗ()` is true it calls `disable()`, otherwise it calls `load(true)`. `BrowserApp.lambda$initApp$1()` calls `resume()`. A cross-DEX scan of all eight recovered files found no other caller of `load`, `disable`, `getCurrentAd` or `setAd`; the only other users of the class are `BannerView` itself, the `Li/bv;` click listener, and the two `Li/su;`/`Li/tu;` timer runnables.
- `BannerManager.postAd()` pushes the current ad to the main thread through `Li/tu;` → `BannerManager.ۦۖۨ` → `lambda$postAd$1`, which ends at `BannerView.onAdReceived(DefaultBannerCallback)` → `setAd`. `setNetworkAdShowingAndNotify(Activity, boolean)` publishes the same `DefaultBannerCallback` on the event bus, so a banner can also arrive from a caller in the missing DEX files.
- `BannerView` is a custom view (the app passes it as a `Landroid/view/View;` to its own `setVisibilityIfChanged`) that holds five children looked up by id: `icon` (`ImageView`, 2131362838), `title` (`TextView`, 2131364059), `action` (`Button`, 2131361850), `aps_banner` (`ViewGroup`, 2131362193) and `default_banner` (`View`, 2131362506). `onFinishInflate()` calls `setupAdView()`, which reads the current ad and calls `setAd(null, it)`.
- `BannerView.setAd(Ljava/lang/Integer;Li/ru;)V` is `private`, 8 registers (`this` in `v5`, the activity hash `Integer` in `v6`, the ad in `v7`), 210 instructions, 1 try block. Resolved control flow, with instruction indices:

| index | instruction | effect |
| --- | --- | --- |
| 0, 2, 4 | `iget-object` of `icon`, `title`, `action` | return if a child is missing |
| 10–13 | `Li/nu2;->ۥۡ()Z` | return immediately when the app's ads-disabled flag is set |
| 20, 23 | `BannerManager.isNetworkAdShowing(Activity)` | hide the banner when a network ad is on screen |
| 24–36 | activity hash comparison | hide unless this ad belongs to the current activity |
| 37 | `if-eqz v7` | return when there is no ad |
| 38–46 | `AmazonService.isInitialized()` and `getBannerBackfillAd("any")` | Amazon APS banner branch |
| 52–58 | `aps_banner` revealed, `default_banner` hidden | APS branch |
| 93–96 | `aps_banner` hidden, `default_banner` revealed | custom banner branch |
| 115, 124, 149, 199 | `setImageBitmap`, `title`, `action` populated from the ad | custom banner content |
| 202 | `View.setOnClickListener` | click target installed |
| 203 | `setVisibilityIfChanged(this, VISIBLE)` | the banner is revealed |
| 205 | `setVisibilityIfChanged(this, GONE)` | the app's own hide path |

- `GONE` is `8`, which `const/4` cannot encode, so the app itself loads it with `const/16 v2, 8` (index 22). A patch that writes the constant must use `const/16` too.

## The banner on the home screen is Appodeal's, and 1DM starts it twice (v0.6.0-dev.8, corrected v0.6.0-dev.10)

This is the finding behind two failed attempts, and both failures had the same cause: the
banner is a different ad system, and 1DM brings it up from two unrelated methods.

Evidence, in the order it was obtained:

1. A uiautomator dump of the installed build (`/storage/emulated/0/1dm_main_hierarchy.xml`)
   shows the footer of the download list as
   `LinearLayout id=footer` -> `FrameLayout id=appodealBannerView` -> `RelativeLayout` ->
   `RelativeLayout` -> `WebView`, sized `[0,2170][1080,2308]`. The leaf is a `WebView`, which is
   how the Appodeal SDK renders a banner. Nothing in the dump is a `BannerView`, an `ImageView`
   icon, a `Button` or an `AmazonService` view, so none of the four app-owned banner classes is
   on screen.
2. `res/layout/activity_main_bottom.xml` decodes to a `com.appodeal.ads.BannerView` as the last
   child of the drawer's content column: `android:id="@+id/appodealBannerView"`,
   `android:visibility="gone"`, `android:layout_width="match_parent"`,
   `android:layout_height="wrap_content"`, `android:layout_gravity="top|left"`. Because it is
   `wrap_content` and `gone` in the layout, never loading it removes the strip rather than
   leaving a gap, which is why the fix does not have to resize anything.
3. `Appodeal`'s own constants, read out of `classes5.dex`: `BANNER = 4`, `BANNER_VIEW = 64`,
   `INTERSTITIAL = 3`, `REWARDED_VIDEO = 128`, `ALL = 4095`. The SDK is therefore asked for
   `BANNER` through `Appodeal.cache(Activity, 4)`, and `2131362191` (`0x7f0a018f`) is the
   `appodealBannerView` entry in `resources.arsc`.
4. A scan of every method outside `com/appodeal/**`, in all eleven DEX files, for
   `Appodeal.initialize`, `setBannerViewId`, `setAutoCache`, `setBannerCallbacks`, `cache`,
   `show` and `destroy` returns exactly eight methods. Three of them write SDK state:

   | method | Appodeal calls | reached from |
   | --- | --- | --- |
   | `Li/rm;->ۦۖۢ(MyAppCompatActivity, Li/m15;)V` | `setAutoCache`, `setBannerViewId`, `initialize`, `setBannerCallbacks`, `setInterstitialCallbacks` | start-up, through `Li/rm;->ۦۖۗ()` |
   | `Lidm/internet/download/manager/e;-><init>()V` | `setBannerViewId`, `initialize`, `setBannerCallbacks`, `cache` | only from `MyAppCompatActivity$2`, the `ConsentManagerError` callback, via `d;->ۦۜ۟(...)` |
   | `Li/rm;->fetch()V` | `cache` x4, with `4`, `3` or `7` | `Li/rm;` itself |

   The other five are read-only or interstitial-only: `Lidm/internet/download/manager/d;->ۦۡۗ`
   and `ۦۡۚ` call `isLoaded(4)` then `show(Activity, 64)`, where `64` is `BANNER_VIEW`, purely to
   tell 1DM's own `BannerManager` through `setNetworkAdShowingAndNotify` that a network ad is
   on screen; `d;->ۦۤۥ(...)V` and `Li/rm;->ۦۗۤ`/`ۦۗۡ` do the same for the interstitial. None of
   them can bring the SDK up or register the view.

`Li/rm;` is 1DM's Appodeal ad manager: a singleton (`public static ۦۖۡ Li/rm;`) that implements
both `BannerCallbacks` and `InterstitialCallbacks`, with `onBannerLoaded(IZ)`, `onBannerShown`,
`onBannerClicked`, `onInterstitialLoaded(Z)` and the rest. Its `ۦۖۢ` is the real bootstrap:
`setAutoCache(false, 7)`, `muteVideosIfCallsMuted(true)`, `setBannerViewId(2131362191)`,
`initialize(context, "b1eafec41c5ab762a5acc356f9526305d05536819d4d0184", 4 or 7, Li/qm)`,
`setBannerCallbacks(this)`, `setInterstitialCallbacks(this)`. `fetch()` then picks its cache mask
from three gates -- `ۦۖۤ()Z` for a loaded, showable banner, the premium check from
`d;->ۦۤ۠(...)`, and the persisted `AppodealNetwork` network preference.

Also settled here: the four app-owned banner classes are `Lacr/browser/lightning/view/BannerView`
(Lightning's, `classes2.dex`), `Lidm/internet/download/manager/BannerView` and
`Lidm/internet/download/manager/manager/NewBannerView` (both `classes11.dex`), plus the Lightning
`default_banner` layout pair. None of them appears in `activity_main_bottom.xml`, which is the
layout the footer is inflated from.

### What the device test proved, and the mistake behind it

v0.6.0-dev.8 suppressed `Lidm/internet/download/manager/e;-><init>()V` on the reasoning that it
was the app's only ad bootstrap. It applied cleanly, CI was green, and the banner was still on
screen in the next dump. Pulling the installed APK and reading its DEX settled it:

- The patched build's `classes.dex` is 144,148 bytes and holds exactly the four classes the
  patch rewrites -- `BannerManager`, `BannerView`, `Lidm/…/d;` and `Lidm/…/e;` -- which is how
  the patcher emits patched classes. So the patch really had been applied.
- `Lidm/…/e;-><init>()V` begins `return-void`; `BannerManager.load` begins
  `invoke-virtual {v1}, …->disable()V`; `BannerView.setAd` begins the `setVisibility(GONE)`
  block; `d;->ۦۜۡ()` begins `const/4 v0, 0 / return-object v0`. All four edits were present.

So the edits were right and the *choice of class* was wrong. The cause is a filter in the
scratch tooling, not in the fingerprint: `idmscan.py` scanned only classes whose name starts
with `Lidm/` or `Lacr/`, on the assumption that the app's own code lives under those two
prefixes. `Li/rm;` is in the obfuscated `Li/` package, so the one class that registers the banner
was invisible to every scan that produced the first two attempts. Both of those attempts are
recorded here as failures, and the lesson generalises: **for a fingerprint, "no caller in the
app's own packages" is not evidence of absence.** Scan the whole APK and exclude the SDK by its
own package, not by a guess about where the app's code lives.

## Disable home screen ads (1DM 18.2)

- Compatibility: `idm.internet.download.manager`, version `18.2`, `ApkFileType.APKM` (non-required, so the plain APK is accepted too).
- **Three edits suppress Appodeal, because 1DM starts it twice**, and all three are needed:
  `Li/rm;->ۦۖۢ(Lacr/browser/lightning/activity/MyAppCompatActivity;Li/m15;)V` gets `return-void`
  at index **1**, `Li/rm;->fetch()V` gets it at index 0, and
  `Lidm/internet/download/manager/e;-><init>()V` gets it at index 0. Nothing is resized or
  hidden: the `BannerView` stays `visibility="gone"` from `activity_main_bottom.xml` and the
  footer collapses, because the view is `wrap_content`.
- The index-1 return is deliberate. Index 0 of `ۦۖۢ` stores the callback argument into
  `Li/rm;->ۦۖ۠Li/m15;`, and every one of `onBannerLoaded`, `onBannerShown`, `onBannerClicked`,
  `onBannerFailedToLoad`, `onInterstitialLoaded`, `onInterstitialShown` and
  `onInterstitialFailedToLoad` reads that field and calls through it, so the store is kept and
  only the SDK bring-up is skipped. `.registers 5`, so `p0` is `v4` and the two declared
  parameters are `v3` and `v2`.
- Suppressing `e;` at index 0 is safe because that singleton's only caller,
  `Lidm/internet/download/manager/d;->ۦۜ۟(Lacr/browser/lightning/activity/MyAppCompatActivity;Li/q15;)V`,
  constructs it solely on the ads-enabled branch and otherwise calls `Li/a6;->ۦۖ۟(...)`, and
  the class's two other instance methods are empty list callbacks (`ۦۖۨ(List)V` is a bare
  `return-void`, `ۦۖ۫(List)V` calls it and returns), so the `Random` field left unwritten is
  never read. The consumers that outlive it are guarded: `d;->ۦۤۥ(...)V` tests
  `Appodeal.isLoaded(3)`, and `ActivityLifecycleListener.onStateChanged(...)` tests
  `AmazonService.isInitialized()` before calling `AmazonService.start`/`stop`. Suppressing `e;`
  also stops `AmazonService.start(true)` and `SmaatoSdk.init`, which is deliberate and in
  scope: `AmazonService`'s only two readers are `BannerView.setAd` and `d;->ۦۤۥ`, both already
  suppressed or guarded here.
- None of the three fingerprints declares a `definingClass`, because `Li/rm;` and
  `Lidm/internet/download/manager/e;` are both obfuscated and change between releases. Each
  pins only `returnType = "V"` plus an ordered chain of anchors that are not obfuscated --
  Appodeal member names, the publisher key, the `AppodealNetwork` preference key, and the
  banner view id literal. Offline resolution against all eleven DEX files of the installed
  build returns exactly one method for each:
  - `AppodealStartupInitFingerprint` -> `Li/rm;->ۦۖۢ(...)V`, 43 instructions, `.registers 5`,
    `public`, filter indices `[20, 23, 24, 33, 39, 40, 41]`: `setAutoCache` -> the view id
    `2131362191` -> `setBannerViewId` -> the key -> `initialize` -> `setBannerCallbacks` ->
    `setInterstitialCallbacks`.
  - `AppodealFetchFingerprint` -> `Li/rm;->fetch()V`, 38 instructions, `.registers 5`, `public`,
    filter indices `[13, 23, 28]`: the `AppodealNetwork` string -> `cache` -> `cache`. The two
    consecutive `cache` filters force two distinct call sites, which is what makes the chain
    specific rather than merely plausible.
  - `AppodealAdInitFingerprint` -> `Lidm/internet/download/manager/e;-><init>()V`,
    69 instructions, `.registers 8`, `public constructor`, filter indices
    `[34, 45, 54, 57, 67]`: `setBannerViewId` -> the key -> `initialize` -> `setBannerCallbacks`
    -> `cache`.

  One caveat about those indices: `idm_fp_check.py` currently reads the *installed* build,
  which already has the `return-void` this patch inserts, so the Appodeal chain resolves one
  index later there -- `[35, 46, 55, 58, 68]` in 70 instructions rather than `[34, 45, 54, 57,
  67]` in 69. The unpatched numbers are the ones above, because a patch run resolves against
  unpatched input. The `Li/rm;` chains are unaffected, as nothing in this patch touches that
  class.

  The first chain ends at `setInterstitialCallbacks` and the third at `cache`, which is what
  keeps the two bring-ups apart. The shape is the same "pin the contract, not the obfuscated
  name" one the Pinterest `AdvertisingIdInfoFingerprint` uses.
- `BannerManager.load(Z)V` is redirected to `BannerManager.disable()V`, which is the exact state 1DM enters when its ad configuration reports the banner as disabled. Consequences: `bannerInfoList` is never populated, `currentBannerInfo` stays null, and `resume()` returns at its `mDisabled` check, so the 500 ms rotation timer never starts and nothing is ever published to the banner view. Nothing else in the ad path is changed.
- The inserted call runs before the method's own `monitor-enter`, so `disable()` is not executed under the method's monitor. That is safe because after the patch every entry into the list and the current ad goes through `disable()`, and the two fields it writes with `AtomicBoolean.set` are the ones `resume()` reads. `disable()` has its own try/catch around the `Timer` access.
- `BannerView.setAd(Ljava/lang/Integer;Li/ru;)V` is replaced with `const/16 v0, 0x8`, `invoke-virtual {v5, v0}, Landroid/view/View;->setVisibility(I)V`, `return-void`. This is the app's own hide path (the branch at index 205), applied unconditionally, so a banner that arrives from any other publisher of `DefaultBannerCallback` is also hidden. `v0` is a scratch local in this method and is only read after the early return, and `v5` is read from the original `iget-object` rather than hardcoded.
- Fingerprints, all six resolved with `.scratch/idm_fp_check.py`, a re-implementation of
  Morphe's matcher. The indices below are the installed build's; the APKM build's, recorded
  before that artifact was available, are in the commit that introduced each fingerprint. The
  three Appodeal fingerprints are listed above rather than repeated here.
  - `BannerManagerLoadFingerprint` → `load(Z)V`, 39 instructions, `public synchronized`, filter indices `[2, 5, 6, 10, 12, 14, 16, 17, 18, 22, 31]`. On the APKM build: 37 instructions, indices `[0, 3, 4, 8, 10, 12, 14, 15, 16, 20, 29]`. The chain is `monitor-enter` (first instruction) → `mDisabled` read → `AtomicBoolean.set` → `mLoaded` read → `mTimer` read → `Timer.cancel` → `currentBannerInfo` write → `bannerInfoList` read → `List.clear` → `List.addAll` → `List.add`. The obfuscated `Li/ru;` type of `currentBannerInfo` is deliberately not declared, because it changes between releases.
  - `BannerViewSetAdFingerprint` → `setAd(Ljava/lang/Integer;Li/ru;)V`, 213 instructions, `private`, filter indices `[3, 5, 7, 9, 23, 48, 49, 55, 205]`. On the APKM build: 210 instructions, indices `[0, 2, 4, 6, 20, 45, 46, 52, 202]`. The chain is the three child-view reads → `View.getContext` → `BannerManager.isNetworkAdShowing` → the `any` slot string → `AmazonService.getBannerBackfillAd` → `aps_banner` read → `View.setOnClickListener`. `any` is the only `const-string` in the method, and both the string and the `AmazonService` call sit in the Amazon branch, which no other method in this class has.
  - `IdmPlusBannerFingerprint` → `Lidm/internet/download/manager/d;->ۦۜۡ()Li/ru;`, 37
    instructions, `.registers 3`, `public static`, filter indices
    `[2, 13, 16, 19, 22, 25]`. This is the one fingerprint that had to be written blind,
    because its target class was in one of the DEX files the damaged download lost; it is now
    verified against the on-device build.
  - The three fingerprints that declare a defining class do so with a trailing `;`, which
    Morphe's type comparison resolves to an exact class match, so each is pinned to a single
    method by construction.
- The `Li/ru;` parameter in `setAd`'s signature and the `Lidm/internet/download/manager/amazon/AmazonService;` call are release-specific, as documented for every obfuscated name in this file. Both are acceptable only because the compatibility declaration is pinned to 18.2/30249.
- The patch does not touch `setNetworkAdShowingAndNotify`, `AmazonService`, billing, or the download service. Suppressing the bootstrap does mean the Appodeal and Smaato SDKs are never initialized and `AmazonService` is never started, which is a deliberate widening: those three exist only to serve the banner, the interstitials and the Amazon backfill.
- The three blocks that existed before v0.6.0-dev.8 were assembled against the same smali build Morphe uses, so they are known to parse. The three Appodeal blocks are one-line `return-void`s with no interpolation, no 35c register list and no arity, so none of the three failure modes recorded below can apply to them. See the pitfalls section for the method and for the brace requirement that 0.3.0 violated.

## 1DM launch crash: `setAd` invoke arity (v0.3.4)

The 1DM build died inflating the home screen layout:

```
VerifyError: void acr.browser.lightning.view.BannerView.setAd(Integer, i.ru)
failed to verify: [0x2] Rejecting invocation, expected 1 argument registers,
method signature has 2 or more
    at android.view.LayoutInflater.rInflateChildren
    at idm.internet.download.manager.MainActivity.onCreate
```

The inserted call was

```
const/16 v0, 0x8
invoke-virtual {v5}, Landroid/view/View;->setVisibility(I)V
return-void
```

`setVisibility(I)V` declares one argument, so its 35c register list has to name the
receiver **and** the visibility int. Only the receiver was named, so the list had one
register where the signature demands two. This is an arity error, not a narrower
encoding of the same call, and the verifier rejects the whole class rather than the
instruction.

The two earlier 1DM fixes were about smali *syntax* -- the braces a 35c register list
requires, and the `v` prefix on interpolated register numbers. Both are satisfied by
`{v5}`, so the call assembled, the patch project compiled, and CI published a bundle.
Nothing in the build path checks arity, so this could only be caught on a device.

The call is now `{v5, v0}`. `BannerManager.load()`'s inserted `disable()V` takes no
arguments, so its single-register list is correct and is unchanged.

The general lesson, and the one worth keeping: smali validity and call validity are
different checks. Braces, prefixes and widths are all verified by assembling. The number
of registers in an invoke list is only verified by the verifier at class-load time, which
means an arity mistake ships as a green build and a launch crash. Any inserted invoke
should have its register count read off the target method's descriptor and asserted
before release.

## The "install 1DM+" strip is not an ad (18.2)

After the arity fix the patched build launched, but the home screen showed a tappable
"Install 1DM+ for an Ad free experience" prompt in exactly the strip where the banner
used to be. It survives the whole "Disable home screen ads" patch, and the reason is that
it is not an ad at all:

| | patched by this project | what was on screen |
| --- | --- | --- |
| class | `Lacr/browser/lightning/view/BannerView;` | `Lidm/internet/download/manager/BannerView;` |
| dex | `classes.dex` | `classes9.dex` |
| content | ad SDK mediation | literal `1DM+: Fastest download manager ($1.99)` and an `INSTALL` button |
| started by | `BannerManager.load()` / `setAd()` | `onFinishInflate()`, unconditionally |

The text and button are string literals in `res/layout/banner_view.xml`, inflated by the
app's own view. Nothing in that path reads the ad configuration, so redirecting `load()`
to `disable()` and hiding the container from `setAd()` both leave it untouched. The
click-through is a 250 ms `Timer` started by `ۦۖۤ()`, not the ad rotation timer.

`BannerViewUpsellFingerprint` now targets `Lidm/internet/download/manager/BannerView;->ۦۖۤ()V`
(59 instructions, `.registers 8`, `this` in `v7`) and prepends
`const/16 v0, 0x8` plus `invoke-virtual {v7, v0}, Landroid/view/View;->setVisibility(I)V`.
Its filter chain resolves at indices 0, 41, 43, 45 and 56: the `ۦۖۚ` guard read,
`Html.fromHtml`, `TextView.setText`, the `ۦۖۚ` write, and `Timer.schedule`. The chain
deliberately avoids the obfuscated members `ۦۖ۠`/`ۦۖۡ`/`ۦۖۦ`/`ۦۖۧ` and the view ids, all of
which are release-specific, and leans on the upsell copy and the timer instead.

GONE rather than an early return: `banner_view.xml` gives the view a fixed
`layout_height` of 55dp, so returning early would swap a populated strip for an empty
one. GONE is also what the view already treats as "stop" -- `BannerView$a.run()` reads
`getVisibility()` and calls `Timer.cancel()` when it equals 8 -- so hiding it also disarms
the click-through timer instead of leaving it running against a hidden view.

This is a deliberate departure from the ADM precedent, where the house "remove ads"
placeholder was intentionally left alone as "a house promo, not an ad SDK view". It is
recorded here as a scope decision, not an oversight.

## 1DM upsell fingerprint failed to match (v0.4.0)

v0.4.0 applied no patches at all, failing on
`BannerViewUpsellFingerprint` with "Failed to match the fingerprint". The target method
was correct -- the obfuscated name and field name were verified codepoint by codepoint
against the DEX, and the five filters did land on instructions 0, 41, 43, 45 and 56 --
but one filter declared the wrong signature:

```
actual:  Ljava/util/Timer;->schedule(Ljava/util/TimerTask; J J)V
filter:  returnType = "Ljava/util/Timer;"
```

`Timer.schedule` returns `void`, not the `Timer`. `MethodCallFilter` compares the
declared `returnType` against the reference's return descriptor, so that filter could
never match, the ordered chain never completed, and the whole fingerprint failed. This is
a plain transcription error: the return type belongs to the *called* method, and copying
the defining class into it is an easy slip.

Every other `methodCall` in the repository was re-checked against the DEX or against the
platform signature it targets, and the rest are correct. The two app-owned ones are
`Lacr/browser/lightning/view/BannerManager;->isNetworkAdShowing(Activity)Z` and
`Lidm/internet/download/manager/amazon/AmazonService;->getBannerBackfillAd(String)
DTBAdResponse;` -- note that the latter has a second `(String, Z)` overload in the same
class, so the filter's explicit single-parameter list is what selects the right one.
The rest are JDK or Android methods whose signatures are fixed by the platform.

## Static checks for the defects that only a device could catch

Five consecutive releases shipped a defect that compiling cannot catch, because each was
correct Kotlin producing wrong smali. `tools/checks/` now covers the two classes that are
mechanically checkable from the sources alone, and `tools/checks/README.md` records what
is still not covered.

- `check_invoke_arity` derives the required register count from the target method's own
  descriptor. It catches the v0.3.3/v0.3.4 `setVisibility` bug, where the inserted 35c
  invoke named only the receiver for a one-argument method.
- `check_replace_instructions` flags a `nop` block longer than the single invoke it is
  meant to erase, since the helper also removes what follows. It catches the
  `DisableAdsPatch` and `DisableRatingPromptsPatch` sites that shipped as v0.2.1 and
  v0.3.3.

`tools/checks/replay_history_check.py` replays released tags to show the checks fire on
the real defects and stay quiet on the fixes: v0.2.1 and v0.3.3 report 3 and 4 problems,
v0.3.4 reports 1, and v0.4.0 and v0.4.1 report 0.

`check_imports` was added after a `methodCall` import was removed on the assumption it had
become unused, which broke `:patches:compileKotlin` in CI. Counting occurrences over the
whole file is not sufficient -- the name also appears in the import line and in prose -- so
usage is measured against the body alone, over the Morphe API surface only, since Kotlin
stdlib members resolve without an import.

Two gaps remain, and both are stated in the checks' README rather than papered over.

- **Fingerprint resolution** needs the pinned APK, a user-supplied 80 MB file CI cannot
  fetch. v0.4.0 declared `returnType = "Ljava/util/Timer;"` for `Timer.schedule`, which
  returns void, and the fingerprint matched nothing. Only a reference DEX catches this.
- **Verifier-visible register typing.** A patch can have correct arity and still leave a
  register holding a reference where an integer is required, which is what the original
  three `VerifyError`s turned on. Only a real verifier catches it.

## The 1DM+ banner is an ad object, not a view (v0.4.0 – v0.4.1 were ineffective)

v0.4.0 and v0.4.1 did apply cleanly and the prompt still appeared. Both patched the
wrong thing, and the reason is that 1DM has **four** app-owned banner classes:

| class | dex | role |
| --- | --- | --- |
| `Lacr/browser/lightning/view/BannerView;` | `classes.dex` | ad SDK banner, hidden by `setAd` |
| `Lidm/internet/download/manager/BannerView;` | `classes9.dex` | a literal upsell strip in `banner_view.xml`, hidden by v0.4.0 |
| `Lidm/internet/download/manager/manager/NewBannerView;` | `classes9.dex` | **the class actually on the home screen** |
| `Lidm/internet/download/manager/AppodealBannerView;` | `classes9.dex` | Appodeal container |

v0.4.0 hid the second of those, which is a real view with real layout, but it is not the
one being drawn.

The prompt is a banner **ad**, built by a static factory:

```
Lidm/internet/download/manager/d;->ۦۜۡ()Li/ru;      // .registers 3, static, 35 instructions
```

It constructs the `Li/ru;` ad object from literals: a base64 PNG icon, the copy
`Install <b>1DM+</b> for an Ad free experience and support developement of the app`, the
label `Install`, the Play Store package `idm.internet.download.manager.plus`, a
`utm_source=1DM&utm_medium=App&utm_campaign=DefaultBanner` campaign tag, the accent
colour `#43A047`, and a `const/16 v1, 30000` 30-second click-through. That is why it is
tappable and opens the Play Store listing — those are the ad object's own fields.

`manager/NewBannerView.ۦۖۦ(Li/ru;)V` (122 instructions) is what paints it, reading the
same `Li/ru;` accessors (`ۦۖۥ` for the icon and sizes, `ۦۖۘ` for the text, `ۦۖۢ` for the
click url).

The factory has four callers, and redirecting `BannerManager.load()` to `disable()`
covers only one:

- `BannerManager.load(Z)V` and `BannerManager.getNewBannerInfo(AtomicBoolean)Li/ru;` —
  the ad rotation
- `Li/s82;->ۦۖۢ(MyAppCompatActivity, Li/m15;)V` and `Li/s82;->ۦۖۦ(MyAppCompatActivity;)Z`
  — reach `manager/NewBannerView` directly

All four null-check the result and skip the banner when it is null, so the fix is to make
the factory return null rather than to hide any view. `IdmPlusBannerFingerprint` prepends
`const/4 v0, 0` and `return-object v0`; `return-void` would be illegal on a
reference-returning method. The chain resolves at indices 0, 11, 14, 17, 20 and 23, and
its filters are the factory's own literals, none of the obfuscated member names.

Note the order matters: the `30000` click-through is written at index 17, *before* the
Play Store id at 20, and filters match in increasing instruction order, so listing the
literal last would leave it permanently unreachable. That was caught by replaying the
chain against the DEX rather than by reading it.

## Unverified risks for 1DM 18.2

- **Layout, 1DM.** Resolved once a sound copy of the APKM turned up: `res/layout/banner_view.xml` is readable, and `Lidm/internet/download/manager/BannerView` has a fixed `layout_height` of 55dp. That is why the upsell strip is hidden rather than merely emptied. `Lacr/browser/lightning/view/BannerView` is a different class in a different dex, and its own layout is `res/layout/banner_view.xml`'s sibling set (`default_banner.xml`, `default_banner_new.xml`).
- **Layout, ADM.** Still unconfirmed. The ADM reference DEX was read from a sound APKM, but its `res/` was never walked for the AppBrain container, so whether that strip leaves an empty gap behind is untested.
- **Other ad surfaces are now readable but still out of scope.** The on-device build has the `Lidm/` DEX, so the interstitial path can be read for the first time: it is `Lidm/internet/download/manager/d;->ۦۤۥ(MyAppCompatActivity, c$a, Runnable)V`, which asks `Appodeal.isLoaded(3)`, installs `d$z` as the interstitial callback and shows, with an `AmazonService.getInterstitialAd("any")` fallback. Suppressing the bootstrap disables it in practice, but the patch does not target that method, so any future interstitial entered from outside Appodeal would still show. The rewarded path is still unexamined.
- **The APKM build's `Lidm/` code can never be verified.** Both 18.2 builds are declared compatible, but the APKM's `Lidm/` classes are in the two DEX files its damaged download lost, so `AppodealAdInitFingerprint` and `IdmPlusBannerFingerprint` are verified against the on-device build only. If the APKM build turns out to register its banner differently, those two fingerprints will fail to resolve there rather than misfire: neither declares a `definingClass`, and both are pinned to SDK member names that have to be present for the ad path to work at all.
- **No compile.** `app.morphe.patches` 1.3.4 cannot be resolved locally: `maven.pkg.github.com` returns `401` for the configured `gh` token, whose scopes are `gist`, `read:org`, `repo` and do not include `read:packages`. Compilation and bundle application are delegated to CI, as with the ADM patches.
- **One device test has been done and it failed; its replacement has not.** The v0.6.0-dev.8 bundle was applied to 18.2 on a device and the banner survived it. Pulling the installed APK showed all four edits present in its DEX, which moved the failure from "did it apply" to "was the right method patched", and that in turn produced the `Li/rm;` findings above. The three-fingerprint replacement resolves offline and passes `tools/checks/patch_smali_checks.py`, but **it has not been applied to a device and the banner has not been seen to disappear.** That is the one claim in this file that still needs a device.
- **Do not trust a scan that filtered by app package prefix.** See "What the device test proved" above. `idmscan.py`'s `Lidm/`/`Lacr/` allowlist is what hid `Li/rm;`; it survives only as a convenience for quick lookups, and its results must not be used to argue that something does not exist. `idm_appodeal_scan.py` is the whole-APK replacement.
- **The offline smali harness has to be rebuilt before it means anything.** `.scratch/check_smali.py` compiles its helper into `/usr/tmp/opencode/jars/out`, and that directory did not survive; the script now fails with an explicit message instead of quietly skipping. Its `CALLS` table also listed two `addInstructions` sites when the patch had four, so it had been exiting on the count check rather than assembling anything. Both are fixed. Rebuild `SmaliTest` per the recipe below before relying on it.

## Patcher pitfalls (1DM 18.2)

- **`addInstructions` smali must be parsed, and 35c invokes need braces.** `addInstructions` routes the string through `InlineSmaliCompiler`, which wraps it in a dummy `.method` built from the matched method's own parameters, register count, and static flag, and then parses it with smali's ANTLR grammar. The 0.3.0 bundle shipped `invoke-virtual v1, L...;->disable()V` and died on-device with `Encountered 2 parser syntax errors and 0 lexer syntax errors!`. The grammar rule is `instruction_format35c_method : INSTRUCTION_FORMAT35c_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference` (`smaliParser.g`, line 1088), so the register list is mandatory and must be braced: `invoke-virtual {v1}, ...`. Only 35c/3rc/45cc invoke forms take braces; 22c forms such as `iput v5, v0, L...;->a:I` take a bare register pair, which is why the ADM patches parse.
- **A register list holds register names, not numbers, and the `v` prefix does not come from the interpolation.** `getRegisterA()` and `getRegisterB()` return integers, so writing `invoke-virtual {$receiver}` renders `invoke-virtual {1}` and 0.3.1 died on-device with `Encountered 1 parser syntax errors` (`no viable alternative at input '1'`). The rendered text has to be `{v$receiver}`. The two failures are distinguishable by the reported count: two errors is a missing brace, one error is a bare number inside braces.
- **Verify the rendered string, not the literal.** Both of the failures above were missed by reading the source and by pasting a hand-written copy of the smali into a local test, because the defect only exists in what the string template produces. `.scratch/check_smali.py` closes that gap: it pulls each `addInstructions(...)` argument out of the patch source, applies the Kotlin templates the way the compiler would, and assembles the result through the same method template `InlineSmaliCompiler` uses. Run it after editing any smali string here; it is offline, takes a few seconds, and it is the only check in this repository that has not needed a release to catch a mistake. Two things about it are load-bearing and were both got wrong at least once: the `CALLS` table must list **every** `addInstructions` site in the patch, in source order, with that method's `.registers` and declared parameters, or it exits on the count check without assembling anything; and the helper class has to be compiled into the `out` directory it names on the classpath, or every result is a failure to exec rather than a parse result. It guards the second case explicitly now.
- **The dummy method means the register numbers are the real ones.** Because the template uses the matched method's `.registers` and parameter list, `p0` resolves to the receiver: in `load(Z)V` (`.registers 3`, one declared parameter) `p0` and `v1` are the same register, and in `setAd` (`.registers 8`, two declared parameters) `p0` and `v5` are the same. Verified by assembling both forms, so the explicit `v`-register form used by the patch is equivalent and does not depend on the template's parameter list being passed correctly.
- **How to verify smali offline without the Morphe plugin.** The forks' smali is published on JitPack at `com.github.MorpheApp.smali:<module>/<commit>/<module>-<commit>.jar` (not the flat Maven path, which 404s), and Morphe tracks `com.github.MorpheApp.smali:smali` at commit `d856bad65f`. With `smali`, `smali-dexlib2`, `smali-util`, `antlr-runtime:3.5.2`, `stringtemplate:3.2.1`, `guava:31.1-android`, and `jsr305:1.3.9` on the classpath, a ~60-line Java program that copies `METHOD_TEMPLATE` from `InlineSmaliCompiler.kt` reproduces the exact parse, the exact error count, and the assembled instruction registers. That is how the brace fix was proven without a Gradle build, and it should be the first step for any new smali here. The same tool reproduces the numbers in the table above.

## The fallback banner is 1DM's own view, and the patch never touched it

A uiautomator dump of the patched build (`/storage/emulated/0/1dm_main_hierarchy.xml`)
shows the footer as `footer` -> `defaultBannerViewNew` (`LinearLayout`,
`[0,2129][1080,2322]`) -> `default_banner` (`[0,2143][1080,2308]`) -> `icon`
(`ImageView`), `title` (`TextView` "Play fun Quizzes and Get Rewards"), `action`
(`Button` "PLAY"). That is 1DM's own fallback banner, and none of the six existing
edits touches the view that draws it:

- It is not Appodeal's. Appodeal's banner would hold a `WebView`, and it is the one
  the fingerprint file says is on screen. Appodeal is suppressed, and the app falls
  back to its own banner. (`gone` views do not appear in uiautomator dumps, so a
  hidden Appodeal view proves nothing either way.)
- It is not the "Install 1DM+" promo. `IdmPlusBannerFingerprint` nulls the
  `d;->ۦۜۡ()Li/ru;` factory, but this ad's copy comes from the server-side ad config,
  not from that factory.
- `BannerManager.load()` -> `disable()` does not stop it. `Li/s82;->ۦۖۢ` and `ۦۖۦ`
  drive `manager.NewBannerView` directly, bypassing `BannerManager`.
  `BannerViewSetAdFingerprint` patches `acr.browser...BannerView`, a different class.

Resource ids, read out of the on-device `resources.arsc` (`ARSCParser.get_res_id_by_key`):
`defaultBannerViewNew` 2131362504 (`0x7f0a02c8`), `default_banner` 2131362506,
`icon` 2131362838, `title` 2131364059, `action` 2131361850, `offer_vpnLL` 2131363548.
The `icon`/`title`/`action`/`aps_banner` numbers agree with the `BannerView.setAd`
notes, which is expected: both banner layouts bind the same ids.

Organized cache (per `AGENTS.md`, created once from the available artifact, not
re-extracted): `~/apks/idm.internet.download.manager/18.2/` holds
`idm.internet.download.manager-18.2-30249_apkv-base.apk` (65,504,886 bytes, SHA-256
`5784871b01259be7f7ca4694e8c33bb892c50ed9b5fb0f9f56063f1515cbef5a`, byte-identical
to the installed `base.apk` and to the `base.apk` inside
`~/apks/idm.internet.download.manager_18.2.apkv`), `apk.sha256`, `dex/` with
`classes.dex`/`classes2.dex`/`classes11.dex`, and the decoded `resources.arsc` plus
`res/layout/default_banner.xml`, `default_banner_new.xml`, `banner_view.xml`. The
flat `~/apks/idm.internet.download.manager_18.2.apkv` (manifest SHA
`5784871b...`) is the source artifact and is left in place.

The renderer is `Lidm/internet/download/manager/manager/NewBannerView;->ۦۖۦ(Li/ru;)V`
(`classes11.dex`): 122 instructions, `.registers 6`, `this` in `v4`, the ad in `v5`.
Indices 0/2/4 read the three child-view fields, `ۦۖۡ()V` binds them with `findViewById`
on exactly the three ids above (indices 5/10/15: `2131362838`/`2131364059`/`2131361850`),
and the method then paints the bitmap (29 `setImageBitmap`), the text (54 `setText`),
the button (111 `setText`), installs the click target (116 `setOnClickListener`) and
reveals itself (117). `NewBannerView` extends `LinearLayout`, so `setVisibility` on
`this` hides the whole `defaultBannerViewNew` strip.

The driver is `Li/s82;->ۦۖۦ(Lacr/browser/lightning/activity/MyAppCompatActivity;)Z`
(`classes11.dex`): 116 instructions, `.registers 11` (`this` in `v9`, the activity in
`v10`). Indices 78-81 load `const v3, 2131362504` and call
`AppCompatActivity.findViewById`, casting to `NewBannerView`; 83-85 run the `ۦۖ۬`
guard, 88-90 post the banner through `Li/q82`. The `IdmPlus` factory is null-checked
at 73-77 (`if-nez v2` returns `false`), but the on-screen copy never passes through
that factory, so nulling it cannot stop this path. The caller (`ۦۗۡ`) ignores the
boolean return. Two sibling helpers (`ۦۖ¨`, 11 insns; `ۦۖ¬`, 12 insns) read the same
container id; the literal's little-endian bytes (`c8020a7f`) occur exactly three times
in the whole `base.apk`, all in `classes11.dex`, which is those three methods and
nothing else.

Fix, same trick as the `setAd` patch:

- `FallbackBannerRendererFingerprint` (`NewBannerView`, `V`, `(Li/ru;)`) pins no
  obfuscated method or field name. Its chain is four unobfuscated SDK calls in
  increasing order: `ImageView.setImageBitmap` (29), `TextUtils.isEmpty` (34),
  `TextView.setText` (54, not the `setTextColor` at 45 -- the exact
  `(CharSequence;)V` signature selects it), `View.setOnClickListener` (116). Offline
  resolution against `classes11.dex` returns exactly this method at `[29, 34, 54, 116]`.
  The patch inserts at index 0 `const/16 v0, 0x8`, `invoke-virtual {v4, v0},
  View;->setVisibility(I)V`, `return-void`. `v0` is safe because the original index 0
  overwrites it; `v4` is `this`, measured not guessed; `const/16` because `const/4`
  cannot encode 8; two registers because `setVisibility(I)V` declares one argument
  plus the receiver (the v0.3.4 arity crash named only the receiver).
- `FallbackBannerDriverFingerprint` (`Z`,
  `(Lacr/browser/lightning/activity/MyAppCompatActivity;)`) pins no obfuscated class
  or method name. Its chain is the container literal, `findViewById`, `Class.getName`
  (111). Offline resolution returns exactly `Li/s82;->ۦۖۦ` at `[78, 79, 111]`; the
  other two literal holders never reach `getName`. The patch inserts at index 0
  `const/4 v0, 0`, `return v0` (`return`, not `return-void`, for a `Z` method). Index 0
  already writes `v0`, so no live register is clobbered, and no mid-method scratch
  liveness has to be proven, unlike a `setVisibility` after the `findViewById`.
  Returning `false` is a state the caller already handles (it ignores the value).

Deliberately out of scope: the `offer_vpnLL` bar ("VPN not connected, click here to
Install...") is a separate ad-like promo with its own container; `hide_vpn_message` is
only its dismiss button, so it needs its own hide if wanted.

Verification: `tools/checks/patch_smali_checks.py` 30 files, 0 problems;
`tools/checks/test_invoke_arity.py` 25/25; both new fingerprints replayed against the
on-device DEX with Morphe's own comparison rules. `:patches:compileKotlin` still cannot
run locally (`app.morphe.patches` 1.3.4 needs `read:packages`, same 401 as recorded
above), so compilation is delegated to CI. Not verified on device: that the strip
disappears and the footer collapses, and (as before) anything about the APKM build's
`Lidm/` code, which was never readable.

## The banner is gone but its slot is not (minHeight column, v0.6.0-dev.22)

v0.6.0-dev.21 removed the ad content but left an empty ~60dp strip. The cause is not a
view the patch missed but a `minHeight` on its parent. Decoded with androguard's
`AXMLPrinter`, `res/layout/activity_main_bottom.xml` wraps the footer in a vertical
`LinearLayout` (`match/wrap`, `minHeight="60dip"`) containing the two banner
`<include>`s, two already-`GONE` promo slots and the `GONE` Appodeal view; the identical
column appears in `activity_main.xml` and `activity_torrent_details.xml`. All three
were verified. `default_banner_new.xml` itself starts `GONE` (`visibility="2"`) and is
`wrap_content`, so hiding the banner is not what leaves the gap -- the column's
`minHeight` keeps a 60dp strip even with every child `GONE`. The v0.6.0-dev.21 driver
early-return made this certain: with the renderer never running, nothing ever hid the
column.

The fix hides the column from the renderer rather than touching the driver. The driver
edit is deleted; the driver now runs its bookkeeping (maps, `Li/q82` post,
impression counters, `Li/r82` timers) and every paint path still ends in the patched
renderer, which at index 0 calls `getParent()` on `this`, hides the parent with
`setVisibility(8)`, hides itself the same way, and returns. The column holds nothing
but ad views in all three layouts, so no legitimate view is affected.

Register safety, measured not guessed (`ۦۖ¦`, 122 insns, `.registers 6`, `this` in
`v4`): index 0 overwrites `v0` and `v1` is unassigned there, so both are free scratch.
`getParent()` declares no argument, so `{v4}` is the complete list; each
`setVisibility(I)V` names receiver plus int. `this` is never null and an inflated
layout child always has a parent that is a `ViewGroup`, hence a `View`, so neither the
call nor the `check-cast` can fail. No new fingerprint was needed and none of the
removed driver's index arithmetic survives: the edit stays at index 0.

Verification: renderer fingerprint unchanged (`[29, 34, 54, 116]`);
`tools/checks/patch_smali_checks.py` 30 files, 0 problems (covers the one-register
`getParent` and both two-register `setVisibility` calls);
`tools/checks/test_invoke_arity.py` 25/25; `:patches:compileKotlin` delegated to CI as
before. Not verified on device: that the footer collapses to the download list.



# Djezzy 3.0.9 reference notes

## Source and target record

- Reference: `~/storage/0/Documents/VInstall/Backups/com.djezzy.internet_3.0.9.apkv`
- The reference is a VInstall APKV container, not a plain APK. Its `manifest.json` reports
  `"format": "apkv"`, `"isSplit": true`, and lists six members.
- SHA-256 of the APKV as stored: not computed for the container; the members are recorded below.
- `base.apk` SHA-256: `f36dab7f20448f05287b5d480178a1a87f6b82a0dcef3557412470707e3ce9ee`, 12,500,283 bytes.
- `split_config.arm64_v8a.apk` SHA-256: `0e29e143baa7157831ec884b9d8d4f51aed249e599e23918fe1620a0556d4201`, 26,100,466 bytes.
- Package: `com.djezzy.internet`. Version name `3.0.9`. Version code `40076`.
- Minimum SDK `24`, target SDK `36`.
- Label "Djezzy". Declares `ACTIVITY_RECOGNITION` plus the camera, contacts, phone-state and
  biometric permissions, which is consistent with a loyalty feature that counts steps.
- The reference is user-supplied and has not been independently verified as the original
  publisher build.

## Where the app's own code lives

This is a Flutter application. The application logic is Dart, AOT-compiled into
`lib/arm64-v8a/libapp.so` (14,681,008 bytes) inside the `arm64_v8a` split; the
`assets/flutter_assets/` tree in `base.apk` holds only fonts, SVG/PNG art and a few
JSON blobs, no Dart source. Nothing in the Dart layer can be read as source text and
nothing in it is reachable by a Dalvik-level patch.

The step number is nonetheless produced by ordinary Java/Kotlin in `classes.dex`, because
Flutter's `pedometer` package is a platform plugin. That is what makes the feature
patchable at all, and it means the patch is architecture-independent: it edits
`base.apk`, which is shared by all four ABI splits, so no `libapp.so` work is needed
and one patch covers every ABI.

`classes.dex` holds 11,863 classes; `classes2.dex` and `classes3.dex` hold 134 and 217
and contain no app classes. Only one app class exists in the whole set:
`Lcom/djezzy/internet/MainActivity;`.

## Walk & Win: the step data flow

The feature is a loyalty "walk and win" campaign. Dart-side evidence, all read out of
`libapp.so` as canonical strings:

- `package:djezzy_app_implementation/features/walk_and_win/` holds the whole feature:
  `presentation/bloc/walk_and_win_bloc.dart` with `WalkAndWinBloc`, `WalkAndWinState`,
  `WalkAndWinLoaded`, `WalkAndWinError`; `data/services/pedometer_service.dart` with
  `PedometerService`; `data/datasources/walk_and_win_remote_datasource.dart`;
  `data/models/waw_campaign_model.dart` with `WawCampaignDataModel.fromJson` and
  `WawLevelModel.fromJson`; and the widgets `walk_step_counter_card.dart`,
  `walk_progress_bar.dart`, `walk_and_win_modal.dart`, `walk_dual_action_buttons.dart`.
- `package:pedometer/pedometer.dart` supplies `Pedometer.stepCountStream`. The sibling
  `stepDetectionStream` is **absent** from the binary, so only the count channel is consumed.
- Persisted keys, all `SharedPreferences` strings: `walk_and_win_current_steps`,
  `walk_and_win_last_pedometer_value`, `walk_and_win_is_walking`,
  `walk_and_win_accumulated_minutes`, `walk_and_win_session_start_time`.
- Network: `GET /services/walk/campaign/` loads the campaign, and
  `POST /services/walk/activate-reward/` claims the reward. User-visible strings include
  `Walk & Win`, `Start Walk`, `Steps`, `Insufficient Steps`,
  `You need more steps to convert to a reward.`, `Claiming reward...`,
  `Reward claimed successfully`, and `Steps taken: `.

The producer is the `pedometer` plugin, registered as
`com.example.pedometer.PedometerPlugin` and obfuscated to three classes. The registrant
string in `Lio/flutter/plugins/GeneratedPluginRegistrant;->registerWith` reads
`Error registering plugin pedometer, com.example.pedometer.PedometerPlugin`, and
`new-instance Li5/a;` is the class constructed beside it. That is a known pub package
whose published source matches the disassembly below instruction for instruction.

- `Li5/a;` is the `FlutterPlugin`. `onAttachedToEngine` builds two `EventChannel`s and
  names them in the DEX: `step_detection` and `step_count`. It constructs `Li5/c;` twice,
  with sensor type `18` and sensor type `19`.
- `Li5/c;` is the `EventChannel$StreamHandler`. Its constructor reads the sensor type and
  stores the name `"StepCount"` for 19 and `"StepDetection"` for 18 in field `l`, then
  calls `SensorManager.getDefaultSensor(type)` and keeps the result in field `k`. Its
  `onListen` registers the listener and returns; **it never pushes an initial value**.
- `Li5/b;` is the `SensorEventListener`, holding the sink in field `a`.
  `onSensorChanged` is the single point where a step number enters the app.

`Li5/b;->onSensorChanged(Landroid/hardware/SensorEvent;)V` disassembles to eleven
instructions with `.registers 3` and one declared parameter:

```smali
const-string          v0, "event"
invoke-static         {v2, v0}, Lkotlin/jvm/internal/i;->e(Ljava/lang/Object;Ljava/lang/String;)V
iget-object           v2, v2, Landroid/hardware/SensorEvent;->values:[F
const/4               v0, 0
aget                  v2, v2, v0
float-to-int          v2, v2
invoke-static         {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
move-result-object    v2
iget-object           v0, v1, Li5/b;->a:Lio/flutter/plugin/common/EventChannel$EventSink;
invoke-interface      {v0, v2}, Lio/flutter/plugin/common/EventChannel$EventSink;->success(Ljava/lang/Object;)V
return-void
```

Instruction `5` is the conversion that is replaced. The parameters occupy the highest
registers (`v1` is the event, `v0` is `this` after the frame is accounted for), which is
the standard Dalvik layout; `addInstructions` builds its dummy method from this method's
own register count, so the register numbers written in the patch are these numbers.

A second `SensorEventListener` exists and is **not** a valid match target: `Lf7/b;` is
`dev.fluttercommunity.plus.sensors.SensorsPlugin` (accelerometer, gyroscope,
magnetometer, barometer, user_accel). It copies the float values into a `double[]`,
appends a timestamp, and calls `success([D)` — it never calls `Integer.valueOf` and
never reads `SensorEvent.values` as the payload. The `methodCall` filter on
`Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;` therefore separates the two cleanly.

## Why two edits are required

Sensor type `19` is `Sensor.TYPE_STEP_COUNTER`. Two properties of that sensor drive the
design, and both are Android platform behaviour rather than anything read from the app:

1. **It is cumulative since boot, not per-step.** A single sample is "total steps since
   the device was last booted", which is why the Dart layer persists
   `walk_and_win_last_pedometer_value` and subtracts a baseline.
2. **It only fires when a step is actually detected.** Standing still produces no events
   at all.

Consequence 2 is the one that breaks the obvious patch. Overriding the value in
`onSensorChanged` alone would still emit nothing while the user is stationary, because
the method is never called. The patch therefore also pushes a value from
`Li5/c;->onListen`, immediately after the listener is registered, so the stream carries a
number as soon as Dart subscribes.

## Patch — Force Walk & Win steps to 10000

- `onSensorChanged`: instruction `5` (`float-to-int v2, v2`) is replaced with
  `const/16 v2, 0x2710`. `0x2710` is 10000 and fits a signed `const/16`. The
  `values[0]` read above it still executes and is discarded. The boxing and the
  `success` call below are untouched and already accept an int, so nothing downstream
  changes shape.
- `onListen`: twelve instructions are inserted at index 22, immediately after the store
  into the plugin's listener field, pushing the same constant through `Integer.valueOf`
  and `success` and logging what it pushed.

  **The insert index is the whole difficulty in this method, and the natural choice is
  wrong.** The `EventSink` arrives in parameter register `v4` (`.registers 5`, three
  declared parameters, so `v2` is `this`, `v3` the `Object` argument and `v4` the sink).
  Three instructions after the listener store, index 22, the method loads the
  `SensorManager` into `v4`, and from there on `v4` is a `SensorManager`. An insert placed
  at the tail — before the closing `return-void`, which is where a tail insert naturally
  goes and where this patch was first written — would therefore hand a `SensorManager` to
  `EventSink.success`, and the verifier would reject the class when the app loads. The
  store into the listener field is the last instruction before `v4` is reused, so
  `instructionMatches[0].index + 1` is the only index in this method where the sink is
  provably still live. `v0` and `v1` are the two locals; both are reloaded or reassigned by
  the instructions that follow, so nothing the insert writes is read back.

  This was caught by decoding the raw Dalvik and tracking the sink register, not by reading
  the patch. It is recorded here because the same trap will apply to any future patch that
  inserts into a Flutter plugin's `onListen`.
- Both sites additionally `Log.i` under the tag `djezzy-waw`, so a device run shows the
  raw sensor value, the value substituted, and the value pushed on subscribe. The tag is
  filtered out of release builds only if the user removes the patch; it is intentionally
  left in, because without it a device test cannot distinguish "patch applied and the
  Dart layer clamped the number" from "patch applied and the number is wrong".

## The v0.5.0 patch fires but the counter reads 0 (device evidence)

A device run with the v0.5.0 patch produced exactly one relevant line:

```
I flutter : GET https://apim.djezzy.dz/mobile-api/api/v1/services/walk/campaign/213772737646
I djezzy-waw: walk: pushing 10000
```

The tag proves the patch applied and the `onListen` insert executed, so the forced value
reached the Dart `EventSink`. The counter still read 0. **The displayed number is therefore
not the value this patch forces**, and overriding the pedometer stream was the wrong layer.

What the binary shows about where the number comes from instead:

- The screen is `WalkAndWinModal`, and it fetches `GET /services/walk/campaign/{msisdn}`
  on the same view. The log shows that request immediately before the push, so both run.
- The campaign entity exposes `maxSteps` and `isUnlimited`, and `isUnlimited` is reached as
  `dyn:get:isUnlimited` — a **dynamic**, JSON-decoded field, so the campaign is a map
  decoded straight from that response rather than a fixed local constant.
- There is no `currentSteps` field anywhere in the binary, so the current count is not
  read from the campaign response either. The strings that look like candidates are
  `walk_and_win_current_steps` and `walk_and_win_last_pedometer_value`, both
  `SharedPreferences` keys, so the count is local.
- Nothing else in `classes.dex` can produce a step number. The only hook in the whole DEX
  is the pedometer plugin: the single matching string is the registrant's
  `Error registering plugin pedometer, com.example.pedometer.PedometerPlugin`, and the
  only classes with `SensorEventListener` are `Li5/b;` (the plugin) and `Lf7/b;`
  (`sensors_plus`). So there is no second entry point to override.

The unresolved part is how the local accumulator is gated. `walk_and_win_is_walking` is
persisted and `_toggleWalking` / `_onWalkingChanged` / `startWalking` / `pauseWalking` all
exist, so the counter plausibly only accumulates while a walk session is active — which
would explain 0 at the moment of subscribe, before Start Walk is pressed. The Dart is AOT
and its strings are shuffled in the snapshot, so adjacency gives nothing; this has to come
from the campaign response and one session's logs.

## Campaign response captured: the server never sees a step count

Device logcat, same run as the failing counter:

```
I flutter : ╔╣ Response ║ GET ║ Status: 200 OK  ║ Time: 612 ms
I flutter : ║  https://apim.djezzy.dz/mobile-api/api/v1/services/walk/campaign/213772737646
I flutter : ║ Body
I flutter : ║    { "message": "Waw campaign", "status": 200,
I flutter : ║      "data": { "wawLevels": [
I flutter : ║          {steps: 5000, reward: GIFTWALKWIN1GO, donation: null},
I flutter : ║          {steps: 10000, reward: GIFTWALKWIN2GO, donation: null}]}}
```

This settles two questions:

1. The campaign carries **no step count at all**. Only thresholds and reward codes. So the
   displayed number is not server-driven, and the reward threshold is 10000 steps ->
   `GIFTWALKWIN2GO`.
2. `donation: null` and no per-user field means nothing in this response can override a
   local count. Confirmed there is no `currentSteps` string anywhere in `libapp.so`.

Ordering in the same log is the useful part:

```
...677.658  I djezzy-waw: walk: pushing 10000      <- our onListen insert
...677.692  flutter  GET .../walk/campaign/...    <- campaign starts after our push
...678.305  flutter  Response .../walk/campaign/
...680.001  I djezzy-waw: walk: pushing 10000      <- a second onListen
```

Two pushes means `onListen` runs twice: the pedometer plugin has two channels
(`step_detection`, `step_count`) that are both instances of `Li5/c;`, so our single
fingerprint matches both. `stepDetectionStream` is absent from `libapp.so`, so the
detection channel is only subscribed because both share the class.

The campaign response lands **after** the first push, so the campaign load rebuilds the
modal's state over whatever the pedometer had already delivered.

### Why the constant cannot work

`walk_and_win_last_pedometer_value` only makes sense if the app stores the previous raw
sensor value and accumulates the difference, i.e. roughly

```
current += raw - last_raw
last_raw = raw
```

A **constant** 10000 therefore yields `+10000` on the first event and `0` on every event
after that. Our second push cannot move the counter, and the campaign load in between
rebuilds the state. A constant is the wrong shape for a delta accumulator; that is the
bug, and it is a bug in the patch rather than in the fingerprint.

There is no stateless smali edit that makes a delta accumulator jump on demand, because
the first emitted value has to sit ~10000 above an unknown persisted baseline. Two ways
out, both viable:

- **Force the stored value instead of the event stream.** Make the persisted
  `walk_and_win_current_steps` read back as 10000. Pure smali, and independent of the delta
  model and of the `is_walking` gate.
- **Keep emitting increasing values.** Needs a stateful emitter, i.e. a Morphe extension
  (`BytecodePatchBuilder.extendWith`), which means a new Gradle module.

The first is chosen: it is narrower, it needs no new module, and it is the value the UI
actually renders. The pedometer constant is kept so the forced baseline and the emitted
baseline stay equal and the delta stays at 0, pinning the counter at 10000.

### Which SharedPreferences backend is live

`shared_preferences_android` is registered as
`io.flutter.plugins.sharedpreferences.SharedPreferencesPlugin`, and that plugin ships two
independent backends. Which one Dart calls is not readable from the AOT snapshot, so both
are patched, each logging under a distinct tag:

- `Lio/flutter/plugins/sharedpreferences/SharedPreferencesPlugin;->getInt` — suspend fun
  returning a boxed `Long`. Its `invokeSuspend` reads the key out of the `$key` field and
  goes to DataStore (`getSharedPreferencesDataStore` -> `Lu0/h;->getData`), so this
  backend never touches `android.content.SharedPreferences.getInt` and needs its own hook.
- `Lio/flutter/plugins/sharedpreferences/LegacySharedPreferencesPlugin;->getAllPrefs` —
  has no per-key getter at all. It copies every entry into a `HashMap` and hands the whole
  map to Dart, which picks the key itself, so the legacy hook injects into that map before
  the `return-object`.

Either log line appearing on a device run proves which path the app uses, so a wrong guess
is visible instead of silent.

### Ground truth for both hooks (androguard, not the hand decoder)

The scratch decoder written earlier mis-renders some opcodes, so the two new hook sites
were re-read with androguard, which is authoritative.

`LegacySharedPreferencesPlugin.getAllPrefs(String, Set) : Map` — 30 instructions,
`.registers 8`, `.ins 3`, so `v5`-`v7` are `this`/`prefix`/`allowlist` and `v0`-`v4` are
locals:

```
  0 iget-object       v0, v5, L...LegacySharedPreferencesPlugin;->preferences Landroid/content/SharedPreferences;
  1 invoke-interface  v0, Landroid/content/SharedPreferences;->getAll()Ljava/util/Map;
  3 new-instance      v1, Ljava/util/HashMap;
 15 invoke-virtual    v3, v6, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
 25 invoke-direct     v5, v3, v4, L...LegacySharedPreferencesPlugin;->transformPref(...)Ljava/lang/Object;
 27 invoke-virtual    v1, v3, v4, Ljava/util/HashMap;->put(Ljava/lang/Object;, Ljava/lang/Object;)Ljava/lang/Object;
 29 return-object     v1
```

`v1` is the map, all three loop exit branches land on the `return-object` at 29, and
`v0`-`v4` are dead by then, so inserting at 29 is a tail insert and the exits run through
it. The six fingerprint filters resolve to indices `0, 1, 15, 25, 27, 29`, and 29 is the
only `return-object`, which is what `opcode(RETURN_OBJECT)` anchors on.

`SharedPreferencesPlugin.getInt(String, Options) : Long` — 13 instructions,
`.registers 5`, `.ins 3`, no branches at all:

```
  0 const-string      v0, "key"
  1 invoke-static     v3, v0, Lkotlin/jvm/internal/i;->e(Ljava/lang/Object; Ljava/lang/String;)V
  2 const-string      v0, "options"
  3 invoke-static     v4, v0, Lkotlin/jvm/internal/i;->e(...)V
  6 new-instance      v0, L...SharedPreferencesPlugin$getInt$1;
  8 invoke-direct     v0, v3, v2, v4, v1, L...$getInt$1;-><init>(Ljava/lang/String; L...Plugin; Lkotlin/coroutines/Continuation; I)V
 12 return-object     v3
```

The two null-check calls name their argument registers outright, which settles the
allocation: `v2` is `this`, `v3` is the key, `v4` is the options object, `v0`/`v1` are
locals. The two `const-string`s are unique in the method and `"options"` is unique to this
overload, which is what the `string(...)` filters pin.

Injected on the async side, at index 0, so it runs before the null checks and can return
without ever starting the coroutine:

```
const-string v0, "walk_and_win_current_steps"
invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v0
if-eqz v0, :djezzy_waw_prefs_passthrough
const-string v0, "djezzy-waw"
const-string v1, "walk: prefs async injected"
invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String; Ljava/lang/String;)I
const-wide/16 v0, 0x2710
invoke-static {v0, v1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
move-result-object v0
return-object v0
:djezzy_waw_prefs_passthrough
```

`equals` has the local constant as its receiver so a null key cannot throw before the
branch. Because the host method has no branches of its own, an insert at index 0 shifts
nothing that anything jumps to.

A note on the legacy key. The legacy Dart API persists under a `flutter.` prefix and strips
it again on the way back out, so the name Dart looks up is the unprefixed one either way —
which is why injecting `walk_and_win_current_steps` rather than
`flutter.walk_and_win_current_steps` is the correct literal on that path.

### Verified offline

- `tools/checks/patch_smali_checks.py`: 14 files, 0 problems.
- `tools/checks/test_invoke_arity.py`: 19/19, including the new `const-wide/16` pair.
- `.scratch/check_walk_smali.py` assembles every rendered string against smali: 8
  instructions for the legacy insert, 11 for the async one, both PASS at both plausible
  register counts.

Not verified: that either hook is the path this build's Dart actually takes. That is what
the two distinct log lines are for.

## v0.5.1 failed to apply: naming a class in a fingerprint removes the fallback

A device attempt with v0.5.1 aborted:

```
app.morphe.patcher.patch.PatchException: Failed to match the fingerprint:
app.djezzy.patches.walk.LegacyPreferenceMapFingerprint@ffcc140
	at ...ForceWalkStepsPatchKt.forceWalkStepsPatch$lambda$0$0(ForceWalkStepsPatch.kt:130)
```

Both new fingerprints were checked against the real DEX before shipping, filter by
filter, with a Java harness built on the same `smali-dexlib2` Morphe uses:

```
 0 preferences iget-object : [0]
 1 SharedPreferences.getAll : [1]
 2 String.startsWith       : [15]
 3 transformPref           : [25]
 4 HashMap.put             : [27]
 5 return-object           : [29]
```

Every filter matched, at strictly increasing indices, which is the condition
`Fingerprint.matchFilters` requires. The candidate pre-filter was ruled out as well by
reproducing `PatchClasses.findIndexValues` and `getClassesReferencingType` over all 11863
classes:

```
Lio/flutter/.../LegacySharedPreferencesPlugin; -> 2 classes, contains target: true
Landroid/content/SharedPreferences;            -> 32 classes, contains target: true
Ljava/lang/String;                             -> 1441 classes, contains target: true
Ljava/util/HashMap;                            -> 398 classes, contains target: true
```

The smallest candidate set contains the target, so the indexed search would have found it.

The real reason is in `Fingerprint.matchOrNull`, `morphe-patcher/src/main/kotlin/app/morphe/patcher/Fingerprint.kt:291`:

```kotlin
val definingClassLocal = definingClass
if (definingClassLocal != null) {
    val type = patchContext.classDefByOrNull(definingClassLocal)   // classMap[classType]
    if (type != null) { ... }
    if (definingClassComparisonLocal != StringComparisonType.EQUALS) { /* scan classMap */ }
    return null                                                    // <-- unconditional
}
```

A fingerprint that declares `definingClass` is reduced to **one** `classMap` lookup. If that
one lookup fails, or the match against that one class fails, matching returns null with no
fallback — the indexed candidate search and the scan-everything fallback below it are both
unreachable, because the `return null` is unconditional and sits before them. Declaring
`definingClass` therefore costs the fallback and buys nothing that `name`, `returnType`,
`parameters` and the filters do not already give.

So both preference fingerprints drop `definingClass` and matching goes through
`instructionFilterCandidates()`, which is index-driven and works. The `HashMap.put` filter
went with it: the concrete map type is the least stable part of that method and a newer
plugin build can swap it for `LinkedHashMap` without affecting anything this patch needs.
The remaining four filters resolve to `1, 15, 25, 29`, so `instructionMatches[3]` is the
`return-object`.

`StringFilter` is deliberately reduced to the single `options` literal: it is the one
string unique to the two-parameter overload, and every extra literal narrows the string
index for no gain.

### Optional hooks may not fail the patch

A fingerprint miss throws out of `execute`, so an unhandled optional hook takes the whole
patch down with it — which is exactly how v0.5.1 turned an optional refinement into a
patch that would not apply at all. Both preference hooks are now wrapped in `runCatching`
and report to the patcher log instead. The pedometer hooks stay mandatory, since without
them there is no patch at all.

What is still unexplained: the filters match on the DEX in the `.apkv` backup, yet did not
match on the APK the manager patched. The class is present in that DEX and the pedometer
fingerprints in the same patch matched, so it is not a wholesale build mismatch. Until that
is pinned down the honest position is that the relaxed fingerprints are *more* likely to
match, not verified to.

## The real cause of the stuck 0: one value cannot open a window and cross it

The two preference hooks never fired on device, and chasing them was a dead end. Dropping
the SharedPreferences idea entirely and re-reading the v0.5.0 log against both possible
accumulations identifies the actual defect, and it is in the pedometer patch itself.

Two models fit every observation:

```
delta:    current += raw - last_raw
baseline: current  = raw - sessionStartRaw
```

v0.5.0 pushed a **single** `10000` at subscribe. Under the baseline model that lone value
*becomes* the baseline, so the total is `10000 - 10000 = 0`, and every subsequent event
carries the same constant and so contributes a delta of zero. The number can never leave
zero. Under the delta model the same constant pays out once and then contributes nothing.
Same defect either way: **one value cannot both establish the origin and jump away from
it.** This explains the `0` exactly, with no need to assume anything about
`walk_and_win_is_walking` gating the accumulator.

The fix is a pair — `0` first, then `10000` — and it is stateless:

| model | first event `0` | second event `10000` | total |
| --- | --- | --- | --- |
| baseline | baseline := 0 | `10000 - 0` | **10000** |
| delta, fresh | no-op or `+0` | `+10000` | **10000** |
| delta, stale baseline 20000 | rewinds `last_raw` to 0 | `+10000` | **10000** |

If the app discards non-positive readings outright, the stored baseline on a fresh install
is already `0`, so the second event still lands on 10000.

Keeping the `onSensorChanged` constant at 10000 then *locks* the value rather than letting
it drift: a constant `raw` yields a delta of zero under the delta model and a fixed
difference under the baseline model, so the displayed number cannot fall away while the
user stands still.

No timer and no extension are needed for this, which matters because `extendWith` is more
costly than it looks:

```kotlin
inline fun extendWith(extension: String) = apply {
    classLoader.getResourceAsStream(extension) ?: throw PatchException(...)
}
```

The argument is a **resource path to a precompiled DEX**, not a class name, and
`BytecodePatchContext.mergeExtension` merges *every* class in that DEX into the app. So an
extension needs d8 and the Android build-tools, which CI does not have (the workflow sets
up only Java and Node), plus a committed binary artifact. The two-event push avoids all of
that.

### What was wrong with the SharedPreferences attempt, for the record

It was not wrong in principle — forcing the persisted total would have worked — but it was
unreachable in practice, and the diagnosis went wrong twice on the way:

1. `LegacyPreferenceMapFingerprint` failed on device. Re-verifying the DEX filter by filter
   with a harness on the same `smali-dexlib2` Morphe uses showed all six filters matching at
   `0, 1, 15, 25, 27, 29`, and reproducing `findIndexValues` over all 11863 classes showed
   the candidate pre-filter does contain the target. The filters were never the problem.
2. The explanation offered next — that declaring `definingClass` removes the fallback search
   at `Fingerprint.kt:291` — is a true reading of the matcher, but it was **not shown to be
   the cause here**. It was asserted, then shipped as a fix, then disproved by the hooks
   staying silent in v0.5.2.

Two files on the device turned out to settle the APK question and kill the "different build"
theory:

```
Djezzy-v3.0.7-patches-v1.44.0-dev.11.apk  classes.dex  f1c5109239f2cf911c4cb01055cd8e4b93e6c70ccb810adb2fcf32486a875db0
com.djezzy.internet_3.0.9.apkv/base.apk   classes.dex  f1c5109239f2cf911c4cb01055cd8e4b93e6c70ccb810adb2fcf32486a875db0
```

Byte-identical, so the manager patched exactly the DEX that was analysed. Local
verification and the manager still disagreed on the same bytes, which means the model of how
the patcher resolves a fingerprint is wrong somewhere — recorded as unresolved rather than
guessed at again.

## Unresolved risks for Djezzy 3.0.9

- **The Dart delta is not confirmed.** `walk_and_win_last_pedometer_value` and
  `walk_and_win_current_steps` being persisted implies the Dart layer computes
  `current += (newValue - lastStored)`. If a baseline above 10000 is already stored, the
  first delta is negative. This is inferred from the key names, not read from compiled
  Dart. The logcat output is what settles it: if the app shows a number below 10000 while
  the log reports a push of 10000, that is the cause. The fix is to also reset the stored
  pair on subscribe, which is a two-instruction addition to the `onListen` insert.
- **The reward is server-gated.** `/services/walk/activate-reward/` decides the payout and
  the campaign's target comes from `/services/walk/campaign/`. This patch changes what the
  client displays and sends. If the backend recomputes the step count itself, it will
  still refuse the claim, and no client-side patch can change that. The request body has
  not been captured.
- **Whether the displayed number is client-side or server-driven is unknown.** The widget
  set includes both a local counter card and a progress bar, and `WawCampaignDataModel`
  may carry a server-supplied step count. A screenshot of the screen is needed to tell
  them apart.
- **`Li5/a/b/c;` are R8-obfuscated and will churn on the next release.** This matches the
  ADM patches, which also key on obfuscated names, but it means the fingerprint is pinned
  to 3.0.9. The framework-level parts (`onSensorChanged`, the descriptor, the
  `EventSink.success` and `Integer.valueOf` calls) carry most of the matching weight.
- **No compile.** `app.morphe.patches` 1.3.4 cannot be resolved from this device:
  `maven.pkg.github.com` needs a token with `read:packages` and the local Gradle cache is
  empty. Compilation is delegated to CI, as with the ADM and 1DM patches. What was checked
  offline instead: brace and paren balance on all three new sources, and every Morphe and
  dexlib2 symbol against `morphe-patcher` v1.13.0 source and the real
  `smali-dexlib2.jar` (`Opcode.IGET_OBJECT`, `FLOAT_TO_INT` and `IPUT_OBJECT` all exist,
  and `fieldAccess` takes `type:` and not `returnType:`).
- **The smali is verified by assembly, and the verifier is known to be honest about it.**
  `.scratch/check_walk_smali.py` renders both smali strings the way the Kotlin template
  would and assembles them through a Java program that mirrors `InlineSmaliCompiler` v1.13.0
  exactly: the same `METHOD_TEMPLATE`, the same parser/lexer error thresholds, the same tree
  walk into a `DexBuilder`. The JitPack smali fork at commit `d856bad65f` and the Maven
  dependencies listed in the 1DM notes are enough to run it with no Morphe artifacts. The
  check is only meaningful because it was negative-controlled: the two defects this
  repository has already shipped were fed back in and reproduced with the same error counts
  reported at the time (a missing 35c brace gives 2 parser errors, a bare register number
  inside braces gives 1). Both new strings assemble, at `.registers 3` and `.registers 5`.
- **No device test.** Nothing has been applied to 3.0.9. The fingerprints were replayed
  against the real DEX and resolve to the expected indices, and the register allocation was
  read out of the raw bytecode, but the runtime effect is unconfirmed.

## CI failure and the two checker bugs behind it (Djezzy 3.0.9)

The first push of this patch failed `Check patch sources` in 15 seconds, on two lines:

```
FAIL ForceWalkStepsPatch.kt: `invoke-static {v1}, Ljava/lang/String;->valueOf(I)...`
      names 1 register(s) but ...->valueOf declares 1 argument(s) and needs 2 (receiver + arguments)
```

One of those two lines was a real defect and one was the checker being wrong. Both had to
be established before anything could be committed, because the obvious response — add a
register to satisfy the checker — would have broken the build for real.

**The real defect.** `Ljava/lang/String;->concat` and `Landroid/util/Log;->i` were written
as `invoke-static` when both are instance methods: `concat` on a `String` and `i` on
`Log`. Smali assembles either form, because the opcode is not checked against the
descriptor, so this passed every local check and would have failed at class-load time on
a device, exactly as the v0.3.3 release did. Fixed to `invoke-virtual`, which is what the
app's own bytecode uses for `String.concat` at `onListen[6]` and `[9]`.

**The checker bug.** `check_invoke_arity` computed `required = 1 + len(params)` for every
invoke kind, counting a receiver on `invoke-static`, which has none. The patch's two
`invoke-static` calls to `Integer.valueOf(I)` and `String.valueOf(I)` were correct and
were reported as errors. Confirmed against the 3.0.9 DEX, where the app itself writes
`invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;` with a single
register at `onSensorChanged[6]`. Adding a register to "fix" the patch would have put a
stray register in a static call and been rejected immediately. The check now special-cases
`invoke-static` as arguments-only.

**A second checker bug, found by the first.** Splitting the parameter list on whitespace
reported one argument for `Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I`,
which is a single unspaced token. The file already contained a correct descriptor parser,
`parse_descriptor`, which walks the grammar; the arity check now uses it.

**A third, pre-existing bug, found by accident.** The wide-register arithmetic was
backwards in both directions and had never been exercised, because no patch so far
inserts a call taking a `long` or a `double`. AOSP's verifier is the authority:
`MethodVerifierImpl::SetTypesFromSignature` seeds `expected_args` from the instruction's
`ins_size` with the comment *"long/double count as two"*, and
`VerifyInvocationArgsFromIterator` advances
`sig_registers += reg_type.IsLongOrDoubleTypes() ? 2 : 1` and then rejects any invoke
whose encoded register count differs. So a wide argument *adds* a register: `z(J)V` needs
two and `z(JI)V` needs three. The check had `len(params) - wide`, which is right for no
signature a patch is likely to contain — it is now `len(params) + wide`.

The check is now covered by thirteen cases, five of them defects it must catch and eight
that must stay quiet, and `replay_history_check.py v0.3.4 v0.4.1` still reports the
shipped v0.3.4 arity defect as failing and v0.4.1 as clean, so the original purpose of the
check is intact.

## Second CI failure: `$EventSink` read as a Kotlin template (Djezzy 3.0.9)

With the arity check fixed, CI got 88 seconds further and then failed
`:patches:compileKotlin`:

```
e: ForceWalkStepsPatch.kt:94:88 Unresolved reference 'EventSink'.
e: WalkStepsFingerprints.kt:49:69 Unresolved reference 'EventSink'.
e: WalkStepsFingerprints.kt:83:87 Unresolved reference 'EventSink'.
```

`Lio/flutter/plugin/common/EventChannel$EventSink;` is a nested type, so the `$` is
part of the descriptor and has to be written `\$` in a Kotlin string literal. Written
plain, Kotlin resolves `$EventSink` as a template expression over a name the file does
not declare, and the file does not compile. This is a compile error rather than a smali
defect, so nothing else in the pipeline can see it: `check_invoke_arity` reads text that
is never assembled, and the smali assembler is never reached. All three sites are fixed.

The patch is also the first in this repository to put a `$` in a patch string at all, which
is why no earlier check covered it. `check_dollar_in_strings` now reports a `$name` inside
a string literal when the file declares nothing by that name, and stays quiet for a
deliberate template, an escaped `\$`, and a bare `$` that is not followed by an
identifier. Its cases, and the six arity cases added earlier, live in
`tools/checks/test_invoke_arity.py` (nineteen in total) so that a check cannot be "fixed"
by loosening it.

Reading the compiler output rather than assuming the first failure was the only one is what
surfaced this. The arity fix had passed the local suite and still did not build.

## Patcher pitfalls (Djezzy 3.0.9)

- **`fingerprint.method.getInstructions().size - 1` is not a safe tail anchor.** The count
  is right but the reasoning is not: in a method whose parameters are reused as locals, the
  last index is past the point where a parameter still holds its incoming type. Anchoring
  on a *named instruction match* and using `index + 1` is what makes the insert position
  survive a rebuild, because it ties the anchor to the plugin's own structure rather than
  to a count.
- **Decoding the DEX is not optional when a patch touches register allocation.** Androguard
  is sufficient for reading a method, but it does not surface which register a parameter
  *still* holds at a given index. The raw Dalvik does, and it is the only way to catch the
  `v4` sink/SensorManager reuse described above. The decoder used is
  `.scratch/dexdump.py`, which is throwaway but is the thing that found the bug.
# Pinterest 14.38.0 reference notes

Disassembly record for the reference build and the Pinterest patches written against it.
Patch status and remaining work are tracked in `roadmap.md` under
`# Pinterest working plan`.

## Source and target record

- Reference: `~/apks/com.pinterest_14.38.0-14388010_minAPI29(arm64-v8a,armeabi-v7a,x86,x86_64)(nodpi)_apkmirror.com.apk`
- Derived output: `~/apks/dex_files/pinterest_v14.38.0/` — `dex/classes.dex` … `classes8.dex`,
  `dexindex.pickle`, `struse.json.gz`, `refs.json.gz`, and the query scripts `dexidx.py`,
  `struse.py`, `refs.py`, `strings.py`, `pkgs.py`, `mfpkg.py`, `manifest.py`. Its own
  `README.md` explains the query reference and warns that the three caches cost about four
  minutes each to rebuild. **Use this cache; never re-extract 14.38.0.**
- SHA-256: `af6b383adb445cebee1ca43f14ac409f91475c1d62e0e11ef52ef52e29fb0553`
  (re-verified against the file on 2026-10-05).
- **Layout deviation, unresolved.** `AGENTS.md` mandates `~/apks/<package>/<version>/`, but
  this target predates that rule and sits one level up, with the APK loose in `~/apks/` and
  the derived output in `~/apks/dex_files/pinterest_v14.38.0/`. Nothing was moved, because a
  half-finished move of a cache that must never be rebuilt is worse than the deviation. The
  Substack and 1DM caches do follow the newer layout. Worth settling before another target
  is added; do not "fix" it by re-extracting.
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
- Build verified: compiles clean in CI (`v0.6.0-dev.1`). Not verified on device, and
  `extensions/` was the first extension module in this repository, so its wiring is unproven
  beyond compiling. The extension also has never been run on a
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
- Build verified: compiles clean in CI (`v0.6.0-dev.1`). Not verified on device. `init` is the method most likely to change
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
- Build verified: compiles clean in CI (`v0.6.0-dev.1`). Not verified on device.

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
with the correct answer. That validates the logic; the Kotlin file itself compiles clean in CI
(`v0.6.0-dev.1`).

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
  Whether the rebuilt manifest still parses cannot be checked here: CI compiles the patch
  code but does not apply it to an APK, so resource recompilation is still unverified.
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

## Email confirmation dialog

The prompt is gated by an experiment flag, not by user state. `Lfq0/r0;->b()Z` reads the key
`android_settings_email_verification`, compares it against the literal `"enabled"`, then reads
the key again through `Lfq0/a0;->i(String)Z`, and returns the conjunction. Five call sites all
take the prompt path on `true` and the normal path on `false`:

```
Lak1/k;.z9()Liu1/k;                                     ins 182
Lvj1/v;.onCreateView(LayoutInflater, ViewGroup, Bundle)  ins 330
Lvj1/v;.z9()Liu1/k;                                     ins 154
Lvj1/u0;.F1(Z)V                                         ins 8
Lvj1/u0;.dismiss()V                                     ins 8
```

`Lvj1/u0;.F1(Z)V` was read in full to confirm the polarity: on `false` it branches past the
email-verification UI block straight to `super.F1(Z)V`. `has_confirmed_email`, by contrast, is a
protobuf field on the user model (`api/model/cq`), not a gate, so it was not used.

## Patch 7 — Disable email confirmation dialog

- Compatibility: `com.pinterest`, version `14.38.0`, version code `14388010`, regular APK.
- `EmailVerificationGateFingerprint` pins the return type `Z`, an empty parameter list, and the
  two literals `android_settings_email_verification` and `enabled`. Neither the obfuscated
  defining class `Lfq0/r0;` nor the method name `b` is used.
- Verified in the reference: the key literal occurs in three code methods plus one interning
  `<clinit>`; the other two return `Object`, so the return type isolates the target exactly.
- The replacement is `const/4 v0, 0` plus `return v0` at index 0. Five registers with one
  incoming parameter, so `v0` is a free local.
- Static validation passed: `tools/checks/patch_smali_checks.py` reports 0 problems across 26
  files including the two new ones, and `tools/checks/test_invoke_arity.py` passes 19/19.
- Build verified: compiles clean in CI. Not verified on device.

## Settings entry: live hierarchy and resource IDs (from device dump)

A `uiautomator` dump of the live Account Settings screen, confirmed as `com.pinterest`, gives
ground truth the static analysis could not:

- The list is a real `androidx.recyclerview.widget.RecyclerView` with id
  `com.pinterest:id/recycler_adapter_view` (`0x7F0A1143`), bounds `[0,308][1080,2311]`.
- Rows are `ViewGroup` with id `com.pinterest:id/page_list_action` (`0x7F0A0E9D`), each holding
  `list_action_header` (`0x7F0A0C4C`, title), `list_action_end_text` (trailing value) and
  `list_action_subheader` (subtitle). Section headers use `settings_section_header_text`.
- The screen also shows the email-verification banner (`banner_message` +
  `banner_primary_action_button` with "Confirm email"). If that banner is visible on a patched
  install, patch 7 is not suppressing it at runtime — noted, not concluded, since the dump may be
  from a stock install.
- `res/layout/lego_fragment_settings_menu.xml` is **not** this screen. It hosts a
  `com.pinterest.ui.grid.PinterestRecyclerView` (which extends `LinearLayout`, not
  `RecyclerView`) with a different id (`0x7F0A0E91`). Do not fingerprint against that layout.

Resource-ID tracing:

- `recycler_adapter_view` is read as `Lxu1/k;->recycler_adapter_view` from four methods, none in
  a settings context (`ideaPinCreation`, `Lqr1/f;.onCreateView`, `Ld11/g;.run`, and
  `PinterestRecyclerView.<init>` itself). It is a shared generic list-container id, so it does not
  isolate the settings screen. `Lqr1/f;.onCreateView` makes 61 invokes and 31 field reads with zero
  settings-related targets — a generic host, not the settings fragment.
- `page_list_action` is read from exactly one place,
  `SettingsListActionItemView.<init>` via `Lnr2/c;->page_list_action`. It is settings-specific.
- `list_action_header` is read via `Lh02/b;->list_action_header` from eight places including
  `GestaltListAction` and the unified inbox. Shared, not settings-specific.
- No `RecyclerView.setAdapter`, `swapAdapter`, `setLayoutManager` or `onBindViewHolder` appears in
  the call index under those names, because the `androidx.recyclerview.widget` classes are
  R8-renamed in this build. Adapter calls cannot be found by those names.

Static analysis ends here. The remaining step is dynamic: trace which adapter serves the live
`recycler_adapter_view` while Account Settings is open, and read back its class name. That single
class name is the anchor the entry patch needs.

## Patch 8 — Morphe settings entry

- Compatibility: `com.pinterest`, version `14.38.0`, version code `14388010`, regular APK.
- Mechanism adapted from `browzomje/browzomje-patches` (older Pinterest versions), whose comments
  document two failures that shaped it: anchoring on a conditional section misses accounts, and
  anchoring on the first `<init>(int)` in program order can land on a conditional spacer. Both are
  avoided here by requiring the header class to be built at least twice.
- `SettingsMenuListBuilderFingerprint` matches `Object invoke(Object)` with a `custom` matcher
  requiring at least five `invoke-direct <init>` in `menu/model/`, including one `(int)` and one
  `(String)`. No class or method name is pinned. Uses the patcher 1.13.0 `custom` API, confirmed
  present in that version.
- On 14.38.0 this resolves to `labs/s;.invoke`: 17 model constructions, header `f1` built 4x,
  external-link `k1` built 1x, the only single-`String` constructor in the package.
- Two injections: `appendMorpheSettingsEntry(list)` after the header's `List.add` (register read
  from the matched invoke via `FiveRegisterInstruction.registerC`, no scratch register needed),
  and `setSettingsRowClass(name)` at index 0 where `v0` is certainly free.
- The extension holds `MorpheRuntimeNames` (resolved names + `morphe://settings`), `SettingsEntry`
  (reflection row construction with full error handling), and a minimal `MorpheSettingsActivity`
  (framework widgets only, `SharedPreferences`-backed placeholder toggles, `isEnabled()` helper
  for future settings-toggled patches).
- The label and manifest patches were rewritten to the same proven design: the label renames the
  existing `settings_menu_teen_safety_resources` (confirmed present in 14.38.0's ARSC) across 48
  locales instead of adding a resource, and the manifest uses a framework theme with
  `exported=true` plus the `morphe://` intent-filter. The earlier versions (new string, inherited
  theme, no intent-filter) would have failed repackaging or crashed on open.
- Static validation passed, and the build compiles clean in CI. Not verified: the fingerprint
  matches the live builder, the row appears, or the activity opens on device.

## Release workflow: recovering a wedged semantic-release run

Observed on `dev` while releasing the three UI-hiding patches. The failure is not in our code;
it is a self-inflicted wedge in the Release workflow, and it is worth writing down because
`gh run rerun` cannot clear it.

The order inside the `Release` step matters:

1. `generatePatchesList` runs `./gradlew generatePatchesList`, which compiles `:patches` only.
2. semantic-release `prepare` commits and pushes `chore: Release v<X> [skip ci]` to `dev`,
   then creates and pushes the tag, then pushes `refs/notes/semantic-release-v<X>`.
3. `publish` creates the GitHub release.
4. Only after `Release` does the workflow run `Attest` and `Verify project compiles`.

Two consequences:

- If `Release` fails at step 2 or 3, `Verify project compiles` is **skipped**, so a green
  `:patches:compileKotlin` is *not* evidence that `extensions` or the bundle built. Read the
  step conclusions, not just the run conclusion.
- If `Release` fails *after* step 2, `dev` has advanced but no tag or GitHub release exists.
  semantic-release derives the next version from the last **GitHub release**, not from tags, so
  the next run recomputes the same version and `git tag` fails with exit 128.

Recovery, in order:

1. Check `gh release list` and `git ls-remote --tags origin` together. A tag with no matching
   GitHub release is the orphan to remove:
   `git push origin :refs/tags/v<X>`.
2. Push a **new** commit. Do not use `gh run rerun`. A re-run re-checkouts the original
   triggering SHA, while the remote is now ahead by the release commit from the failed attempt,
   so semantic-release logs `The local branch dev is behind the remote one, therefore a new
   version won't be published` and exits green having published nothing.
3. Watch for that exact message. A green run containing it is a no-op, not a release.

## The inline smali dialect: `->member:Type`, and never a doubled `;`

Found the hard way. `v0.6.0-dev.12` compiled in CI, released cleanly, and then threw on device
while applying patches:

```
PatchException: Encountered 2 parser syntax errors and 2 lexer syntax errors!
  at InlineSmaliCompiler$Companion.compile
  at HideNotificationsNavButtonPatch.kt:47
```

Two independent faults, both in the two nav patches, which were the first patches in this project
to inject **field** references. Every earlier patch injects only `invoke-*` method references, which
is why nothing else was affected.

1. **Field references need a colon.** This grammar wants `Lae0/o;->a:Lde0/a;`. The
   space-separated `Lae0/o;->a Lde0/a;` that baksmali prints and that every disassembly listing and
   every fingerprint comment in `reference/` shows is rejected with `missing COLON`. Method
   references are unaffected. Verified against the parser, not inferred:

   | field reference | result |
   | --- | --- |
   | `->a Lde0/a;` space | fails |
   | `->a:Lde0/a;` colon | parses |
   | either, with `;;` | fails |

2. **A doubled `;` is invalid.** The `private const val` descriptors already end in `;`, so the
   smali template must not add another. `$DESCRIPTOR_TYPE->a:$TAB_ENUM;` expands to
   `Lae0/o;->a:Lde0/a;;` and fails with `Invalid text` on the `;`.

So a baksmali listing is the right place to *read* an instruction and the wrong place to *copy* one
from. The dialect difference only shows up at patch time.

`tools/checks/check_inline_smali.py` now guards this. It pulls each triple-quoted block out of the
patch sources, substitutes the file's `private const val`s exactly as Kotlin would, wraps it in a
stub carrying the target method's real `.registers` and parameter count, and runs it through
`SmaliTestUtils.compileSmali` — the same entry point `InlineSmaliCompiler` uses. Reintroducing the
space form makes it report the same `missing COLON` the device did. Run it before pushing any patch
that injects smali; Kotlin compiling is not evidence that the smali parses.

## Both action-bar constructors have to be patched, not just the three-parameter one

`v0.6.0-dev.13` applied cleanly on a device and the navbar toggles worked, but the comments button
was still there. Not a failed fingerprint and not a wrong seam: the patch matched the constructor
that never runs.

`UnifiedPinActionBarView` has two constructors and they are **siblings, not a delegating pair**.
Neither calls the other. Each one runs its own super constructor, builds every child view, and
stores field `d` (the comments wrapper) and `e` (the icon) itself:

| constructor | regs / ins | stores `d` | returns at |
| --- | --- | --- | --- |
| `<init>(Context, AttributeSet)` | 9 / 3 | ins 134 | byte 380 |
| `<init>(Context, AttributeSet, int)` | 9 / 4 | ins 124 | byte 368 |

`LayoutInflater` inflates a custom view from XML through the **two-parameter** constructor, so on a
real pin that is the one that executes. The three-parameter overload is never reached from
inflation. Patching only it produced a patch that matched, applied, reported success and did
nothing.

Confirmed there is no later reset: the class contains no `setVisibility` call at all, so nothing
undoes a constructor-time `GONE`. Both constructors are now patched, with a separate fingerprint
each.

The register maps differ between them, which is the second trap. The three-parameter constructor
has four declared parameters, so `this` is `v5` and `v0`/`v1` are free. The two-parameter one has
three and passes six registers to its super constructor with `invoke-direct/range`, so `this` is
**`v0`** and the free scratch registers are `v1`/`v2`. Reusing the same block for both would
overwrite `this` and crash the constructor at inflation. In both, the wrapper is in `v6` at the
insertion point and the insertion index is the same: the `action_module_comments_icon` `sget`, which
comes immediately after the store into `d`.

## What the inline smali check does not do

`compileSmali` is a parser, not a verifier. Measured, not assumed:

| input | result |
| --- | --- |
| unknown opcode | rejected |
| `->a Lde0/a;` space form, doubled `;` | rejected |
| write to a declared parameter register (`v8`) | **accepted** |
| out-of-range register (`v12`) in a nine-register method | **accepted** |

So `tools/checks/check_inline_smali.py` guarantees syntax only. It will not tell you that a block
writes over `this` or over a value the target method still needs. That has to be reasoned out, which
is why the register map is recorded per block in the checker's `LAYOUTS` rather than left implicit.

## The comments wrapper is not the comments button

`v0.6.0-dev.14` applied cleanly, the toggle was on, and the comments button was still there. A
uiautomator dump of the patched pin settles it:

```
com.pinterest:id/action_bar_root                 LinearLayout  [0,0][1080,2388]
  com.pinterest:id/action_module_react_icon_sab   ImageView     [11,1179][143,1311]
  com.pinterest:id/reaction_count                 TextView      [127,1225][206,1265]
  com.pinterest:id/action_module_comments_icon    LinearLayout  [206,1157][358,1333]
  com.pinterest:id/action_module_share_icon_sab    LinearLayout  [358,1157][490,1333]
```

`action_module_comments_wrapper` is **absent from the tree**, while
`action_module_comments_icon` is present, visible, and sitting exactly where a comments button
belongs — between react and share.

So the patch *ran*; it was hiding the wrong view. A `GONE` view is dropped from a uiautomator dump,
which is why the wrapper's absence proves the earlier patch worked as written. The wrapper is a
**sibling** of the icon, not its parent, so hiding it changed nothing visible.

The constructors corroborate this. Each one does, in sequence:

```
sget  action_module_comments_wrapper -> findViewById -> check-cast ViewGroup -> iput ->d
sget  action_module_comments_icon    -> findViewById -> check-cast GestaltIcon -> iput ->e
```

`d` is the wrapper and `e` is the icon. The patch now hides `e`. It reads the field off `this`
rather than reusing `v6`, because `v6` has already been reused for the id by the time the icon
lookup runs, and `e` is written by the immediately preceding `iput-object`.

Note the dump reports `action_module_comments_icon` as a `LinearLayout` while the constructor casts
it to `GestaltIcon`. The accessibility class in a dump is not always the runtime type, and the cast
in the constructor is what the app itself relies on, so `e` is the right handle regardless.

Lesson worth keeping: a patch can match, apply, report success, and still be a no-op. The only
thing that settled two separate wrong-target bugs here was a view dump from the running app.

## Retraction: the wrapper's absence proved nothing

An earlier entry in this file claims the wrapper being missing from the uiautomator dump is "itself
evidence the earlier patch ran". **That reasoning is wrong and is retracted.**

It assumed the wrapper would appear in that dump if the patch had not run. There is no basis for
that. The dump is of a *different view*, so the wrapper may never have been in it. Absence of an
expected node is not evidence unless you have independently established that it should be present.

## The real target: `LegacyPromotedCloseupActionButtonModule`

The dump's ids end in `_sab`:

```
action_module_react_icon_sab      in the dump
action_module_share_icon_sab      in the dump
action_module_comments_wrapper    what the patch targets   (no _sab)
```

Those are a different, smaller action bar. The dump reports `action_bar_root` and
`action_module_comments_icon` as `android.widget.LinearLayout`, but `UnifiedPinActionBarView` is a
custom ViewGroup and would appear under its own class name. So the view being screenshotted was
never the one being patched.

Indexing every read of the two R fields gives five classes. Only two constructors read the wrapper,
and both belong to `UnifiedPinActionBarView` — the wrong class:

| class | reads |
| --- | --- |
| `UnifiedPinActionBarView.<init>` x2 | wrapper + comments_icon |
| `LegacyPromotedCloseupActionButtonModule.createView` | `action_module_comment_icon` (singular "comment") |
| `EducationNewContainerView.e`, `Lho0/c;.<init>`, `Lsa1/i;.<init>` | comments_icon only |

`LegacyPromotedCloseupActionButtonModule.createView` is the real target. It owns
`action_buttons_center`, `promote_button`, `menu_react`, `menu_send`, `overflow_button` — which is
exactly the set of views in the dump — and it is named for the *promoted* closeup action bar, i.e.
the legacy variant. In it:

```
registers=6 ins=1
102  sget                     v1, Lvf0/c;->action_module_comment_icon I
103  invoke-virtual           v5, v1, Landroid/view/View;->findViewById(I)Landroid/view/View;
104  move-result-object       v1
105  check-cast               v1, Lcom/pinterest/gestalt/iconbutton/GestaltIconButton;
106  iput-object              v1, v5, ...->l Lcom/pinterest/gestalt/iconbutton/GestaltIconButton;
```

So the comments button is field **`l` : `GestaltIconButton`**, not `e : GestaltIcon`, and the id is
`action_module_comment_icon` — singular, no `s`, and no `_sab`.

Three separate near-misses stacked up here, and each was individually plausible:

1. `action_module_comments_wrapper` and `action_module_comments_icon` read like the comments module,
   and they are — of the *other* action bar.
2. `action_module_comments_icon` appears in the dump, so it looks like the right anchor, but the dump
   shows the accessibility class rather than the runtime type, and this field is a
   `GestaltIconButton` while the dump reports a `LinearLayout`.
3. Two sibling constructors that both had to be patched, which was real work that changed nothing.

A patch applied cleanly against the wrong class will do exactly this forever. The only reliable
discriminator was the class name in the dump, and it was the one thing I did not check first.

## Correction: the on-screen comments cell is `Lsa1/i`, legacy kept alongside

The entry above concluded `LegacyPromotedCloseupActionButtonModule.createView` is the
real target. A fuller dump (`~/storage/downloads/pinterest.xml.txt`) plus the prebuilt
`refs.json.gz` index overturn that for organic pins, while leaving the legacy path
plausible for promoted pins — so both are now patched, pending device proof of which
bar each pin type uses.

Dump facts (uiautomator, patched pin, closeup bar):

- `action_module_react_icon_sab` → `ImageView`
- `action_module_comments_icon` (plural, no `_sab`) → `LinearLayout [218,1448][398,1624]`
  holding an `ImageView` plus a `TextView '84'` (both id-less)
- `action_module_share_icon_sab` → `LinearLayout` holding `send_btn ViewGroup` →
  `uab_share_button ImageView`
- `GestaltIconButton` in the same dump is `android.widget.Button` with
  `icon_button_container` children (`carousel_flashlight_button`, `overflow_button`).
  The comment cell is not one of those, so field `l : GestaltIconButton` cannot be it.

Index facts (`python` over `refs.json.gz`, 14.38.0):

| id | readers |
| --- | --- |
| singular `action_module_comment_icon` (`Lvf0/c`) | only `LegacyPromotedCloseupActionButtonModule.createView` |
| plural `action_module_comments_icon` | `EducationNewContainerView.e` + `Lho0/c;.<init>` (both `Lpc0/o`, education mappings, not inflation), `UnifiedPinActionBarView.<init>` x2 (wrong class: custom ViewGroup, dump root is plain `LinearLayout`), `Lsa1/i;.<init>` (`Lsf0/b`) |
| `react_icon_sab` | `Ld11/g;.run`, `Lay2/f00;.<init>` — neither is the legacy module |
| `share_icon_sab` | `Lbb1/y1;.<init>`, `Lya1/x;.<init>` — neither is the legacy module |

`Lsa1/i;.<init>(Context)V` (`registers=15 ins=2`, `this=v13`, single `return-void`
at ins 108, zero try blocks) is byte-for-byte the dump node:

```
34  sget   v1, Lsf0/b;->action_module_comments_icon I
35  setId(v13, v1)            <- this IS the LinearLayout
40  new GestaltIcon / 78 new GestaltText (count, initially GONE at ins 103-104)
106 addView(icon) / 107 addView(count)
```

`Lya1/x;.<init>` is the sibling proof: `LinearLayout` + `setId(share_icon_sab)` +
`UABAnimatedShareButton` with `send_btn` id — the dump's share branch. Parent
`Lbb1/u0;.<init>` news `Lya1/x` into field `f`, `Lsa1/i` into field `g`, and
`addView`s `g` at ins 152 between react and share. The legacy module inflates
`pin_closeup_lego_action_button_module`, a different layout, and is therefore at
best the promoted-pin variant.

Patch consequence: `Lsa1/i` never calls `findViewById`, so there is no lookup to
hook after — the patch sets `GONE` on `this` (`v13`) before the sole `return-void`,
using dead `v1`/`v2` as scratch. `View` (not `GestaltIconButton`) is the call type,
`const/16 0x8` as before. All three names (`Lsa1/i`, `Lbb1/u0`, `Lsf0/b`) are
obfuscated and version-pinned to 14.38.0/14388010.

## Why the constructor hook lost the race (device, 14.38.0)

The patch hid the cell in `Lsa1/i.<init>` and it still appeared on screen. Not a
fingerprint failure and not the `GONE` encoding this file already documents twice: the
host's presenter re-shows the cell after the constructor has returned.

`Ltt/b2;.i(Lyt/b;)V` (`classes4.dex`, `registers=46`, the UAB presenter, a
Dagger-generated class so the name is version-pinned too) at ins 255-266:

```
255  iget-object       v1, v2, Lbb1/u0;->g Lsa1/i;      // the comments cell
256  invoke-virtual    v1, Object;->getClass()Ljava/lang/Class;
258  iget-boolean      v3, v10, Lsa1/a;->a Z
259  if-nez            v20, +009h
260  const/16          v4, 8                              // logged out -> GONE
261  invoke-virtual    v1, v4, View;->setVisibility(I)V
263  const/4           v11, 0                             // logged in  -> VISIBLE
264  invoke-virtual    v1, v11, View;->setVisibility(I)V  <- restores the cell
266  iput-boolean      v4, v1, Lsa1/i;->h Z
```

Found by intersecting the prebuilt `refs.json.gz`: the methods that both call
`View.setVisibility` and read `Lbb1/u0;->g` are `Lbb1/u0;.<init>` (which sets
visibility on the *sibling* `Lua1/h`, not on the cell), `Ltt/b2;.i`, `Lr31/f;.invoke`
and `Lsa1/i;.<init>` itself. `Lbb1/u0;.a(Lbb1/u0;I)V` looks like a binder by name and
is not: it is a width-measurement helper for the count.

Only ins 264 undoes the patch, and only for a signed-in user, which matches the
report: the count `84` renders, so the cell is up.

Patching that call site was rejected on purpose. There are 2,305
`View.setVisibility` call sites app-wide, the presenter is reached through an
obfuscated Dagger component, and any other caller added by a later release would
re-show the cell the same way. The choke point is the method itself, so the patch
now injects a `setVisibility(I)V` override into `Lsa1/i` that rewrites the argument
to `GONE` when the toggle is on and then re-dispatches with `invoke-super`. The
constructor hook stays, because an override cannot hide a view that nobody calls
`setVisibility` on, and `Lsa1/i` is `VISIBLE` by default with nothing in its own
constructor changing that.

`Lsa1/i` is `public final` and declares exactly one method (`<init>`, verified in
`classes6.dex`), so nothing can shadow the override and there is no existing
definition to collide with. The patch `check`s for a pre-existing
`setVisibility(I)V` anyway, since a duplicate definition makes dexlib2 reject the
class at write time.

## Injecting a method with morphe-patcher 1.13.0

No `addMethod` helper exists, and the ones that look like it are write-time traps.
Verified by a standalone harness against the real `classes6.dex` with
`morphe-patcher-1.13.0.jar`, `smali-dexlib2-d92701d947.jar` and the same
`internClass` → `writeTo(FileDataStore)` path `DexReadWrite` uses:

- `BytecodePatchContext.mutableClassDefBy(String)` returns Morphe's `MutableClass`
  proxy (cached per type in `PatchClasses.ClassDefWrapper`, so repeated calls return
  the same instance the fingerprint's method came from).
- `MutableClass.getMethods()` is a `LinkedHashSet` (`toMutableSet()` in
  `_methods_delegate`), so `add` is the only way to add a method. It is typed
  `Set<MutableMethod>`, so an `ImmutableMethod` needs a cast.
- **It must be Morphe's `MutableMethod`.** `getDirectMethods` casts every element of
  that set while `PoolClassDef` pools the class, so an `ImmutableMethod` throws
  `ClassCastException: ImmutableMethod cannot be cast to MutableMethod` from
  `MutableClass._directMethods_delegate` at write time, not at patch time. Wrap it:
  `MutableMethod(ImmutableMethod(...))`, no setters needed since the immutable
  method already carries the right name, parameters, flags and implementation.
- Instructions come from `InlineSmaliCompiler.compile(instructions, parameters,
  registerCount, isStatic)`, the same ANTLR parser `addInstructionsWithLabels` uses.
  It wraps the text in `.method %s dummyMethod(%s)V` / `.registers %d`, which is what
  makes `p0`/`p1` resolve — with `.registers 4` and `(I)` they are `v2` and `v3`, not
  `v3`/`v2`. Both forms assemble, and a `p`-register mistake here writes the
  visibility argument into `this`.
- `MutableMethodImplementation(registerCount)` plus `addInstruction` each parsed
  instruction; the constructor takes no instruction list.
- Verified output, re-read from a written dex:

```
public setVisibility(I)V          registers=4 ins=2 outs=2
   0 invoke-virtual    v2, View;->getContext()Landroid/content/Context;
   1 move-result-object v0
   2 const-string      v1, "morphe_hide_comments"
   3 invoke-static     v0, v1, MorpheSettingsActivity;->isEnabled(Context;String;)Z
   4 move-result       v0
   5 if-eqz            v0, +004h
   6 const/16          v3, 8
   7 invoke-super      v2, v3, Lqc1/b;->setVisibility(I)V
   8 return-void
```

`Lqc1/b` (`Lsa1/i`'s direct superclass, `classes6.dex`) is `public abstract` and
declares no `setVisibility`, so `invoke-super` resolves through it to
`View.setVisibility` — the same target d8 would emit.

Not verified: the Gradle build. `./gradlew :patches:compileKotlin` cannot run in
this environment — `SettingsPlugin.kt:48` throws `IllegalArgumentException` while
configuring the GitHub Packages credentials because `GITHUB_TOKEN`/`GITHUB_ACTOR`
are unset. The module was compiled instead with `kotlinc -jvm-target 11` against the
Gradle-cached jars, and the dex behaviour was proven by the harness above. Neither
replaces applying the patch to the pinned APK on a device.

## Phase 3 — pin download already exists upstream

> **Read `### RETRACTED` below before using anything in this section.** The facts
> about the menu, the row factory and the downloader are sound. The conclusions
> about eligibility were wrong in three separate ways, and a patch built on them
> shipped and was reverted. The section is kept because the disassembly is still
> the map; the reasoning about what to change is not.

The transcription for Phase 3 asks to *add* a download option to the pin
long-press menu. It does not need adding: Pinterest 14.38.0 ships the whole
feature, including the network write and the storage-permission flow. What is
missing is only *eligibility* — the row is withheld from plain image pins.

### The long-press menu, and which of three builders is it

Long-pressing a pin opens the pin overflow modal, not the "closeup drawer".
Two different surfaces exist and only one of them is the long-press menu:

- The **closeup drawer** is the bottom sheet on the pin closeup screen. Its view
  package is unobfuscated — `com/pinterest/feature/pin/closeup/view/drawer/`
  holds `CloseupDrawerBottomSheetView`, `CloseupDrawerMediaHost`,
  `CloseupDrawerRecyclerView`, `CloseupMediaScrollView` (all `classes6.dex`) —
  and its resource ids are `closeup_drawer_bottom_sheet`,
  `closeup_drawer_collapsed_prompt`, `closeup_drawer_drag_handle`,
  `closeup_drawer_media_host`, `closeup_drawer_peek_scrolled_top_background_stub`
  (`Lpf0/c;`), `fragment_pin_closeup_drawer` (`Lpf0/d;`) and
  `pin_closeup_drawer_{hidden,visible}_collapsed_height` (`Lpf0/a;`).
  `Lbb1/t;` is its fragment (`M9()` returns `fragment_pin_closeup_drawer`;
  `onCreateView` reads `closeup_drawer_media_host`). Do not confuse this with the
  long-press menu: it has no action rows of its own.
- The **long-press overflow menu** is `GridActionsLocation$PIN_OVERFLOW_MENU_MODAL_FRAGMENT`
  (`classes5.dex`), hosted by `com/pinterest/feature/gridactions/modal/view/`:
  `PinOverflowMenuModalImpl` (38 fields, 35 methods, extends `Ls11/l;`),
  `OverflowMenu` (a `LinearLayout` row view), `PinFeedbackModalContentView`
  (extends `com/pinterest/ui/view/BaseRecyclerContainerView`),
  `OverflowMenuModalProviderImpl`.

The row list is assembled by `Lfn1/f;` — a 34-field, 22-method presenter in
`classes6.dex` whose constructor takes
`Lcom/pinterest/sendshare/model/SendableObject;` and thirty-odd
other collaborators. Three methods append rows to a caller-supplied
`java.util.ArrayList`:

| Method | Registers | What it adds |
| --- | --- | --- |
| `F3(ArrayList)V` | 4 | `Lnj1/i0;.d(Context)` at index 0, then Instagram / Facebook rows if those apps are installed |
| `G3(ArrayList)V` | 23 | the real builder: download, share, report, hide, save-to-board, separators |
| `H3(ArrayList)V` | 6 | "Add to story" at index 0 (`sharesheet_add_to_story`, `Lis2/g;`) and download appended last |

`G3` is the long-press menu and it says so in unobfuscated code: `ins 80-82`
compares its location field against `Lin1/t1;->PIN_OVERFLOW_FEED_MODAL`.

### The row model, and the row factory

`Lcom/pinterest/adapter/e;` is the menu row, and it lives in `classes4.dex`,
not next to its factory: `a` `Drawable` (icon), `b` `String`
(title), `c` `String` (analytics id), `d` `Lkx1/o0;` (action enum), `e` `boolean`,
`f` `boolean`. `Lkx1/o0;` is a merged R8 enum that holds both icon names and
action names (`DOWNLOAD`, `BELL`, `BOOKMARK_CHECK`, …), so a filter on it is not
specific; the id string is.

The download row is built by `Lnj1/i0;.d(Landroid/content/Context;)Lcom/pinterest/adapter/e;`
in `classes6.dex` — 8 registers, 13 instructions. It reads `Lnm0/d;->download_icon`
(`classes5.dex`) for the icon, `Lgi0/b;->download` (`classes.dex`) for the title, and
constructs the row with `Lkx1/o0;->DOWNLOAD` and the id **`"DOWNLOAD_IDEA_PIN"`**.
That id is the only `const-string` in the factory, which makes it the cheapest
possible anchor for this exact row, and it is also a warning: upstream built this
row with idea pins in mind.

`Ls11/b;.a(LinearLayout, Lkj1/c;, Lkj1/b;, Lx30/b;, String, Z, Z, Function0, Function0)Z`
(`classes5.dex`) is the second entry point. It takes the eligibility object and a
`LinearLayout` and returns whether it injected anything, which is how
`com/pinterest/feature/pin/closeup/sba/downloadaction/DownloadActionView` (`classes6.dex`)
reaches the closeup action bar. That class is inflated from XML — nothing in the dex
instantiates it, so it has no callers and `refs.py classrefs` returns nothing for
it. Its two constructors are the only members.

### The downloader itself

Not a stub. `La21/c;` (`classes5.dex`) is the failure-reason enum and its constants
are still named: `NETWORK_REQUEST_FAILURE`, `PERMISSION_DENIED_BY_USER`,
`EXTERNAL_STORAGE_SPACE_NOT_AVAILABLE`, `EXTERNAL_STORAGE_MEDIA_NOT_MOUNTED`,
`EXTERNAL_STORAGE_DIRECTORY_CAN_NOT_BE_CREATED`, `PIN_OR_URL_NULL`,
`SAVE_TO_STORAGE_FAILED`. `La21/b;` is the Kibana logger
(`KibanaMetrics`), called from `Luq1/a;.onError` (`classes6.dex`), `Lt03/a;`,
`Lyj2/c;.invoke` and `Lz11/o;.invoke`. `Luq1/a;` is an R8-merged interface
collector carrying `onSuccess`, `onError`, `onComplete`,
`onRequestPermissionsResult(I, String[], int[])` and `v(Lv0/b;)V`, i.e. the
runtime storage-permission request; `f(Landroid/net/Uri;)V` shows
`pin_image_download_success` (`Lgi0/b;`). `Lgs2/a;.b(Z)V` (`classes7.dex`) reads
both `pin_image_download_success` and `pin_video_download_success`, so the
completion handler branches on media type rather than assuming video. Related
strings: `downloading_video_spinner` / `downloading_video_modal_view` (`Lis2/d;`,
`Lis2/e;`, `classes7.dex`), `downloaded_to_camera_roll` (`Lgi0/b;`),
`pin_more_download_fail` (`Lgf0/b;`, `classes5.dex`),
`storage_permission_download_explanation` (`Lic2/b;`, `classes3.dex`).

So the roadmap's Phase 3 warning — "media handling may cross native code, check
whether the save path is Java or JNI" — is answered for *this* path: the download
entry point, the HTTP call, the MediaStore write and the permission request are
all DEX-side. No `libx_media_handler.so` involvement was found on the download
route. (`libx_media_handler.so` is about decoding and rendering, not saving.)

### Why the row is withheld from a plain image pin

`Lfn1/f;.G3` gates the block on `ins 118-128`:

```
118  iget-boolean      v6, v0, Lfn1/f;->o Z          // a pre-set flag
120  if-nez            v6, +00bh                       // flag set -> eligible
121  invoke-virtual    v10, v2, Lkj1/c;->e(Lcom/pinterest/api/model/pe;)Z
122  move-result       v6
128  if-eqz            v6, +048h                       // not eligible -> skip row
```

`Lkj1/c;->e(pe)Z` (5 registers, 44 instructions) is the eligibility predicate:

```
 3  if-eqz        v4, +046h      // null pin -> false
 4  invoke-virtual v4, pe;->Y5()Ljava/lang/Boolean;   // reads field C1
 8  if-nez        v2, +03ch      // idea pin -> false
 9  invoke-static  v4, Lcom/pinterest/api/model/ye;->p0(Lcom/pinterest/api/model/pe;)Z
11  if-eqz        v4, +036h      // not video/product -> false
12  invoke-virtual v3, Lkj1/c;->d()Z                 // experiment
15  invoke-virtual v3, Lkj1/c;->f()Z                 // variant select
```

The load-bearing line is `ins 9`. `ye.p0(pe)Z` is:

```
1  invoke-static  O0(pe)Z     // product/shopping pin (domain + Q7().g() map)
3  if-nez         v0, +025h
4  invoke-static  I0(pe)Z     // pe.z7() != null  -> video pin
6  if-eqz         v0, +008h
7  invoke-static  H0(pe)Z     // I0 && Q3[216] && !pe.q7()
10 iget-object    v0, pe->Q3 [Z
12 const/16       v2, 108
13 if-le           v1, v2, +010h
14 aget-boolean    v0, v0, v2
16 invoke-virtual v3, pe;->K5()Ljava/lang/Boolean;
21 const/4        v3, 1  /  23 const/4 v3, 0
```

So upstream shows Download exactly when the pin is a **video pin or a product
pin**, and never for a plain image pin. That looked like the whole gap. It was
not: `G3` calls `ye.p0(pin)` itself before the eligibility gate, and the download
event has no subscriber outside the closeup and promoted-pin surfaces. See
`### RETRACTED`.

`e(pe)Z` has exactly two callers: `Lfn1/f;.G3` (the menu we want) and
`com/pinterest/ads/feature/owc/collageads/s;.a(Ljb0/z;, Lqw2/d;)Lqw2/o;` (a
collage-ads presenter). Any edit inside `e()` therefore changes collage ads too;
an edit at the `G3` call site does not.

### Phase 2 "Hide ad tags": there is no ad-tag view to hide

Searched and not written. Recorded so the search is not repeated, and because the
finding contradicts the transcription rather than merely complicating it.

The transcription says "removes Pinterest's ad loop views so visual elements no
longer reveal Promoted or Shop labels". Across the **3,111 unobfuscated
app-owned classes** in the reference there is nothing that is an ad tag:

| Searched | Hits |
| --- | --- |
| `adtag`, `adlabel`, `adinfo`, `adbadge`, `byline` | **0** |
| `promoted` | 21, but data models (`PromotedQuizPinData...`) and `PromotedPinCloseupFloatingActionBarBehavior` |
| `sponsor` | 3: `AdsIdeaPinCreatorAndSponsorView`, `BoardSponsoredCuratorView`, one more |

Where the label actually comes from:

- `Ljv/e;` is a resource-id holder, not a view. It owns the label strings:
  `promoted_by`, `promoted_by_prefix`, `promoted_reason`, `sponsored_by`,
  `sponsored_by_prefix`, `sponsored_pins_prefix`, `sponsored_pins_eu_prefix`,
  `sponsored_pins_simple_prefix`.
- `com/pinterest/activity/pin/view/modules/util/AvatarWithTitleAndSubtitleView`
  is the unobfuscated view that **composes** them, in
  `.b(Lcom/pinterest/api/model/cq;)V` — it reads `Ljv/e;->promoted_by` and
  `sponsored_by` and writes them into its subtitle `GestaltText`.

**The blocker is that this view is not ad-specific.** Its parameter is `cq;`, a
general attribution model, and the same view renders ordinary "by <creator>"
attribution. It is 214 lines doing avatar binding, title, subtitle, an icon
(`Laq1/c;`) and accessibility text in one method. Suppressing it removes
legitimate creator attribution too, and there is no single instruction inside it
that touches only the ad case.

`Lis1/b;.b(Lpe; Z)Z` does return a boolean over the sponsored prefixes, so it
looks like the label decision, but it is a 96-line `ordinal()` dispatch over
`Lis1/b;.a(pe;)Lsz2/u;` and nothing establishes which of its branches is the ad
case. Editing it would be a guess of exactly the kind that produced the two
reverted patches.

So the options are: suppress attribution for ad pins specifically, which needs the
ad-versus-not decision and runs into the same `p0` / `is_promoted` family that the
download patch died on; or accept losing creator attribution as collateral. Neither
is a small patch. Left for a deliberate decision rather than written speculatively.

### RETRACTED: "Download pin from long press" -- written, shipped, then reverted

This patch was written, shipped in `v0.6.0-dev.24` enabled by default, reported
broken on device, and reverted. Everything below supersedes the reasoning that
produced it. The short version: **the row's presence was made to work, and its
behaviour cannot be made to work from that surface**, for two independent reasons.

#### Error 1 -- the trace was wrong

The original trace said the tap goes `Lnj1/o0;.h(View, String)` -> `Lnj1/q;.b(...)`
-> a modal at `Lin1/x1;->DOWNLOAD`. That is the idea-pin branch and it is not the
path a plain pin takes.

`Lnj1/o0;.e(View, String)` runs first. Its dispatch keys on
`instance-of Lnj1/k;` over the sendable, and when that holds and the row id is
`"DOWNLOAD_IDEA_PIN"` it does three things and returns:

```smali
invoke-virtual {v3}, Lkj1/c;->f()Z
const-string     v4, "previous_downloader"
sget-object      v3, Lqo2/s1;->PIN_DOWNLOAD_BUTTON:Lqo2/s1;
...
sget-object      v0, Lnc0/q;->a:Lnc0/s;
new-instance     v1, Lm02/e;
invoke-direct    {v1, v2}, Lm02/e;-><init>(Ljava/lang/String;)V     // pinId
invoke-virtual   {v10, v8}, Liw2/d0;->h(Ljava/lang/String;)V       // "DOWNLOAD_IDEA_PIN"
```

`Lm02/e;` is a one-field event, the pin id. **`Lnj1/q;.b(...)` is the idea-pin
video download**, with its `DownloadManager` progress modal -- not the plain-image
path.

Two file names in the original brief were wrong too, in a way that made step 4
unsupported by the files actually shipped:

| Class | What it actually is |
| --- | --- |
| `Lnj1/r;` | a three-value **enum**: `DOWNLOAD`, `INSTAGRAM_STORIES_SHARE`, `FACEBOOK_STORIES_SHARE` |
| `Lnj1/m;` | `(String pinId, Lnj1/r; type, Long, String)` -- an in-progress-download record |

The fragment the trace meant to cite is `Ls11/r;` (`s11/r.smali`, extends
`Ls11/d;`) with `Ls11/m;` (`s11/m.smali`) holding `i()Lpe;`.

One inference in the review that does *not* hold: `SendableObject.k()` is not an
idea-pin flag. `i()` is `c == 0` and `k()` is `c == 0 && field i` -- the same int
`c` in both, so they are share state, not pin type.

#### Error 2 -- there is a second, independent gate

`Lfn1/f;.G3` checks eligibility twice, and the patch only covered one of them.
Resolved offsets, `classes6.dex`:

```
@0x019c ins 105  invoke-static {v2}, Lcom/pinterest/api/model/ye;->p0(Lpe;)Z
@0x01a2 ins 106  move-result v6
@0x01a4 ins 107  if-nez v6, +0004h   -> @0x01ac ins 109
@0x01a8 ins 108  goto/16 +00f1h      -> @0x038a ins 235   PAST the download block

@0x01c0 ins 114  invoke-virtual {v6}, Lhn1/a;->getShowInSharesheet()Z
@0x01c8 ins 116  if-nez v6, +0004h   -> @0x01d0 ins 118
@0x01cc ins 117  goto/16 +00dfh      -> @0x038a ins 235   PAST the download block

@0x01d0 ins 118  iget-boolean v6, v0, Lfn1/f;->o Z
@0x01dc ins 121  invoke-virtual v10, v2, Lkj1/c;->e(Lpe;)Z      <-- what the patch forced
@0x01e2 ins 122  move-result v6
```

All three failure paths land on ins 235, past the block that adds the row at
ins 234. Forcing `Lkj1/c;.e()` is therefore necessary but not sufficient: a pin
where `ye.p0` is false never gets the row no matter what the patch does.

**And the row did appear on device, which proves `p0` was true for that pin.**
`p0` is `O0(pin)` (product pin) or `I0(pin)` (video) or (`Q3[108]` and `K5()`).
So the pin used for the test was a video or product pin -- exactly where the row
is native -- and **the patch was never exercised.** The reported symptom was stock
behaviour on a pin the patch did not touch.

#### Error 3 -- the event has no subscriber for a feed pin

Forcing both gates would make the row appear and still not download. The tap
posts `Lm02/e` on the bus (`Lnc0/q;->a`), and only two classes subscribe:

| Subscriber | Its pin comes from | Surface |
| --- | --- | --- |
| `Ltt/a2;` | `PinCloseupBaseModule.getPin()` | the **pin closeup screen** |
| `Lbb1/x1;` | `Lbb1/y1;` field `g` | the **promoted/ad pin cell** |

Both compare the event's pin id with their own pin's `pe;.c()` and return on
mismatch. A feed pin that is neither open in the closeup nor promoted matches
neither subscriber, so the request goes nowhere -- which is what the share sheet
on screen was.

Both subscribers do resolve a URL, via
`Lcom/pinterest/feature/f;->getCurrentImageUrl()Ljava/lang/String;` -- so the
accessor recorded as untraced does exist, but only on those two surfaces. That is
the answer to the open question this section used to carry.

#### Corroborated by a second review: DOWNLOAD is a share-sheet MODE

An external read of `Ljy/nj1/t0` settles what the old trace called a "download
modal". It is not a separate screen.

`Lin1/x1;` is a four-value enum -- `NONE`, `SHARE`, `DOWNLOAD`, `SCREENSHOT` --
and it declares `isDownloadOrScreenshot()Z`. `Lnj1/t0;.a(...)` takes one and, at
its tail, reads `SCREENSHOT`, `DOWNLOAD`, then the strings `"shouldAddSaveIcon"`
and `"upsellTypes"`. The object `t0.a` builds with that mode is `Lin1/e1;`, which
declares `createModalView(...)Lcom/pinterest/component/modal/BaseModalViewWrapper;`
and `createPresenter()Liu1/k;` -- i.e. the in-app share sheet.

`Lnj1/t0;.h(...)` branches on `x1.SCREENSHOT` three times and on `x1.DOWNLOAD`
once before reaching `t0.a`.

**So tapping Download opens the share sheet in download mode, with a save icon.**
That is the designed behaviour, and it is the whole explanation of the device
report. It also means the first half of this retraction is not merely a wrong
trace: the surface the roadmap wanted does not exist as a separate screen, and
the share sheet is where upstream put it.

#### What actually works, and it is stock

`DownloadActionView` is injected into the pin closeup action bar by
`Ls11/b;.a(LinearLayout, Lkj1/c;, Lkj1/b;, Lx30/b;, String, Z, Z, Function0, Function0)Z`,
called from `Ls11/a0;` and `Ls11/r;`. Pinterest already downloads pins from the
closeup, for pins that qualify.

#### Verdict

A patch here can make a row appear; it cannot make that row download from the feed
menu without also building the subscriber, which is new logic rather than a
fingerprint. Reverted rather than shipped with a description it does not honour.
If the feature is wanted later, the tractable shape is "surface the closeup's
existing download", not "extend the feed row".

### RETRACTED: "Disable in-app share sheet" -- same error, found in time

The first version of this patch forced `Lhn1/a;.getShowInSharesheet()` false.
That was caught on device because it silently disabled the download row: `G3` at
ins 114 reads the same accessor and jumps to ins 235, past the download block.
It was rewritten to force the answer at three presentation sites instead --
`N3`, `Lr11/a;.a`, `Lnj1/t0;.a` -- which removed that conflict.

**The rewrite fixed the conflict and kept the original mistake.** None of the
four sites decides whether the in-app sheet appears. All four feed a boolean
*into* something:

- `Lnj1/t0;.a` (site 3) uses the result as one conjunct of a guard deciding
  whether to construct `Lnj1/z;`, the share-sheet config object:

  ```
  invoke-static  {v0}, Lcom/pinterest/api/model/ye;->p0(Lpe;)Z
  move-result    v6
  if-eqz         v6, :skip
  invoke-static  {v5, v6}, Lhn1/b;->a(Lfq0/v0; Lfq0/w0;)Lhn1/a;
  move-result-object v5
  invoke-virtual {v5}, Lhn1/a;->getShowInSharesheet()Z
  move-result    v5
  if-eqz         v5, :skip
  new-instance   v6, Lnj1/z;
  invoke-direct  {v6, v0, v11, v5, v1}, Lnj1/z;-><init>(...)
  ```

  Skipping the config object does not stop `t0.a` building `Lin1/e1;`, the sheet.
  Note that `ye.p0` is in the same guard -- the predicate that gates the download
  row appears here too.

- `Lfn1/f;.N3` is a refresh that skips a selection observer.
- `Lr11/a;.a` builds the social-app list inside the sheet.

So the patch changed what the sheet *contains*, not whether it *appears*, while
its description promised "sharing goes through the system sheet instead". That
promise was never verified either -- the notes said so at the time, and the
description said it anyway.

**The pattern, since it happened twice.** Both reverted patches found a boolean
that feeds a decision and treated it as the decision. The check that would have
caught both: ask what the method *returns* or what it *constructs*, not what it
reads. `Lvu2/d1;.b()Lvu2/e1;` passes that test -- it returns the rendition
object its callers go on to load -- which is why the force-original patch survives
and these two do not.

**Where the real decision lives, for whoever picks this up.** Not in the accessor
and not in its four callers. The callers of `Lnj1/t0;.h` are `Lnc0/m;`,
`Lnj1/s0;` and `Lza1/i;`; the choice between the in-app sheet and a platform
`ACTION_SEND` is made in one of those, upstream of `t0.h`/`t0.a`. The obvious
candidate inside the row handler is `Liw2/d0;.j(Context, SendableObject, String,
Lzo2/c;, Integer)V`, reached from `Lnj1/o0;.h` -- but it is **not** a system
chooser: it carries `"more_apps"`, calls `queryIntentActivities` and `"com.pinterest"`,
so it is Pinterest's own multi-app share flow. Finding the real branch means
reading those three callers.

### Unused

`Savable`, `pinImageDownloaderFactory` and `gif_pin_drawer_context` are dead
ends worth recording so they are not re-searched:

- `pinImageDownloaderFactory` is not a Dagger module. It occurs twice, both as an
  analytics event name: `Lcv/q;->onEventMainThread(Li52/d;)V` (`classes.dex`
  ins 178) and `Lbb1/x1;->onEventMainThread(Lm02/e;)V` (`classes6.dex` ins 156).
- `gif_pin_drawer_context` and `ic_pin_drawer_button_nonpds` exist in the string
  table but are referenced by no field read. Pinterest's own resources are not
  reached through an `R$string;` class — the app inlines them as `sget` from
  per-package holders such as `Lgi0/b;`, `Lpf0/c;`, `Lnm0/d;`. Searching for
  `R$` field references in this APK finds only `net/quikkly/android/R$string;`,
  which is why a resource-name search has to go through `fielduse.py` (see
  below) rather than `refs.py`.
- `Save image` (`classes3.dex`) is an **ad SDK** string, used once by
  `Lads_mobile_sdk/ip1;->a(Lads_mobile_sdk/ip1;Ljava/util/Map;Lads_mobile_sdk/lw0;)Lkotlin/Unit;`
  ins 328. Unrelated to pins.
- `quick_swap_option_item_download_image` (`Lnh0/f;`) is the create-mode quick
  swap, not the pin menu.

### Tooling added for this spike

`refs.py` cannot answer "which methods read field X", which is the only question
that matters in a package whose resources are inlined as `sget` constants, and
`pkgs.py` prints only a prefix histogram. Two throwaway helpers were added:

- `.scratch/fielduse.py <regex> [--names]` — reads `refs.json.gz` and prints the
  methods that read or write a matching field, or just the distinct field names.
  This is what found the drawer resource ids and the download strings.
- `.scratch/pkg.py <prefix> [needle]` — prints superclass, interfaces and member
  counts for every class under a prefix. In a fully obfuscated package, shape is
  the only usable first filter; this is what identified `Lfn1/f;` as the menu
  presenter.

Both read the prebuilt caches and re-parse nothing.

### Phase 3 item 3, "Download board": a feature build, not a patch

The third Phase 3 item asks for an option in the board "..." menu that
bulk-downloads every image and video in a board grid. Unlike items 1 and 2,
nothing of it exists upstream, and the gap is not a gate — it is missing code.

Evidence, in the order it was checked:

- **The pin overflow menu has no such row.** `Lnj1/i0;` is the complete row-factory
  class for that menu: eight static factories, one per row, each building one
  `Lcom/pinterest/adapter/e;`. Their ids are `copy_link` (`c`), `download` (`d`),
  `FACEBOOK_STORIES` (`e`), `INSTAGRAM_STORIES` (`f`), `internal_send` (`g`),
  `more_apps` (`h`), `pin_messaging` (`i`) and `SAVE_LINK` (`j`). There is no
  board-level or bulk variant, and no second factory class feeds the menu.
- **No literals.** `download_board`, `Download board`, `bulk_download`,
  `save_all_pins` and `board_download` all return nothing, via
  `morphe-helpers scripts/find-string` and via the raw string-table scan.
- **No resources either**, which is the part a literal search cannot see, since
  Pinterest inlines its own resource ids as `sget` constants: `fielduse.py` finds
  `bulk_move_pins_success`, `multi_pin_sharing_*` and no download field anywhere.
- **No code at all.** `rg -ril download` over the whole of
  `com/pinterest/feature/gridactions/` and `com/pinterest/feature/board/` — every
  pin-action and board class in the app — returns **zero files**. The download code
  that does exist lives entirely under the single-pin path found above
  (`gridactions/utils/logging/`, `La21/`, `Luq1/`, `Lgs2/`).
- **Multi-select has bulk actions, but not this one.**
  `com/pinterest/feature/gridactions/multipin/` and
  `com/pinterest/feature/multipinsharing/` implement select-many-then-act with
  share, collage, vote and move-to-board (`bulk_move_pins_success`). Nothing there
  downloads.

So the item cannot be delivered as a fingerprint plus a smallest safe edit. Even
the most favourable version — a new row that loops the existing single-pin
downloader over the pins the grid already holds — needs all of: a new row factory,
a click handler wired into `Laa1/b;.e(...)`, whose dispatch is a 42-register
method switching over a merged R8 enum; an N-item download loop with its own
progress and per-item failure handling; a MediaStore write per asset; and, for the
video half, the container question the roadmap already flags as a possible
`libx_media_handler.so` crossing. That is new logic measured in hundreds of lines,
which is the definition of a feature build, and it is the same verdict the Substack
offline section already reaches for the same reason.

Recorded and left unstarted. If it is wanted later it should be scoped as its own
project with its own risk note, not smuggled in as a Phase 3 patch.

### morphe-helpers against this target

`~/morphe-helpers` works here — `scripts/setup --check` passes 13/13 (apktool,
smali, baksmali, java, rg, jq all run; only `adb` is absent, so device steps exit
3). It is a better instrument than the ad-hoc scripts for fingerprint work, with
one caveat on this APK.

`scripts/decompile` cannot be used as-is: its identity step runs `apktool d -s`
and that OOMs on a 133 MB, 73,720-class APK on this device (about 2 GB of free
RAM), so `scripts/target-init --apk` fails at "could not read package/version".
Worked around without touching the extraction:

1. Registered the target in `~/morphe-helpers/targets.json` from the identity
   already recorded above (package, version, versionCode, SHA-256), so no script
   needs to re-derive it from the APK.
2. Disassembled the eight already-extracted dex files with `baksmali` directly into
   `~/apks/com.pinterest/14.38.0/smali/` — 73,720 classes, 582 MB, about four
   minutes — and wrote a matching `meta.json`. This also moves the Pinterest cache
   onto the `~/apks/<package>/<version>/` layout `AGENTS.md` asks for, so the
   earlier recorded deviation is now resolved for Pinterest rather than deferred.
3. `MORPHE_CACHE=~/apks` points the helpers at it. `scripts/class-outline`,
   `scripts/get-method`, `scripts/find-string`, `scripts/dex-stats` and
   `scripts/xrefs` all work against it.

`resources: false` in that `meta.json` is accurate and deliberate: the apktool
resource decode never completed, so `scripts/find-resource` and anything else
needing decoded `res/` has no data. Resource *ids* are still visible in smali as
`sget` from holders like `Lgi0/b;`, which is what `fielduse.py` searches.

### Phase 4 item 6, "Use the system share sheet": one gate, four callers

Pinterest's custom share sheet is not a class you can suppress; it is a
boolean. `Lhn1/a;` is a two-constant enum, `CONTROL` and `FRONT`, and the
whole feature hangs off one accessor:

```smali
.method public final getShowInSharesheet()Z
    .registers 2
    sget-object v0, Lhn1/a;->CONTROL:Lhn1/a;
    if-eq p0, v0, :cond_6
    const/4 p0, 0x1
    return p0
    :cond_6
    const/4 p0, 0x0
    return p0
.end method
```

Six instructions, and the answer is decided entirely by which constant the
caller holds. The constant is assigned by `Lhn1/b;.a(Lfq0/v0;, Lfq0/w0;)Lhn1/a;`,
which walks an `Lfq0/a0;.h(String, String, Lfq0/w0;)` experiment list — the
entries are built in `Lhn1/b;.<clinit>` and include
`android_closeup_download_in_sharesheet`; `sg_android_sharesheet_holdout`
lives in `Lfq0/v0;` and `Lfq0/q;` — and returns `FRONT` if any entry matches,
`CONTROL` otherwise.

**This is why forcing the answer is the correct patch rather than a hack.**
`CONTROL` is not an error state; it is the variant the app already ships to
the holdout group. Forcing `false` pins an existing supported behaviour rather
than inventing a third one, which is the same argument that made the Phase 3
download edit acceptable.

Four call sites, all asking the same question and all doing less when the
answer is no:

| Caller | What it is |
| --- | --- |
| `Lfn1/f;.G3(Ljava/util/ArrayList;)V` | the pin overflow row builder — the class the download patch edits |
| `Lfn1/f;.N3(Lcom/pinterest/feature/sharesheet/view/SharesheetModalAppListView;)V` | share-sheet refresh; the false branch goes to `:goto_80` |
| `Lr11/a;.a(Lr11/m;)Ljava/util/List;` | builds the social-app list; false jumps to `:cond_165` and takes the simpler list |
| `Lnj1/t0;` | share-sheet helper |

No other code in the app branches on it, so the blast radius is the share
sheet and not four unrelated features.

`getShowInSharesheet` is the only method in the APK with that name, and its
surviving obfuscation is the useful signal: R8 does not keep a method name by
accident, so this one is referenced across an obfuscation boundary or
explicitly kept. The patch therefore leaves `definingClass` unset on the
fingerprint and anchors on the method name plus the `CONTROL` field read,
rather than on `Lhn1/a;`, which will not survive a release.

**Verification beyond compiling.** The replacement body was applied to the real
`hn1/a.smali` and reassembled with `smali assemble`, then disassembled again
with `baksmali`; the round trip returns exactly:

```smali
.method public final getShowInSharesheet()Z
    .registers 2
    const/4 p0, 0x0
    return p0
.end method
```

So the class still assembles to a valid dex after the edit, which compiling
Kotlin would not have shown. The injected block also passes the repo's own
`check_inline_smali.py`, which needed a layout entry added for it — the checker
only extracts `addInstructionsWithLabels` blocks, so the patch uses that call
rather than `addInstructions` for that reason.

**Not verified, and the honest limit of this one.** There is no device here,
so whether the platform chooser actually appears is unconfirmed. What is
confirmed statically is only that the four callers stop building the custom
sheet. The send execution itself lives further down the pipeline
(`Lc43/m;.w(...)` versus `.A(...)`, both of which just create coroutine
scopes), so the fallback to an `ACTION_SEND` chooser was *not* traced. The
patch description therefore promises the chooser only as the intent, and the
device pass should confirm it before the description is trusted.

### Phase 4 item 1, "Copy direct link": the option is already there

Like Phase 3 item 1, this one is half-built upstream. The menu row exists:
`Lnj1/i0;.c(Landroid/content/Context;)` builds it with icon
`copy_link_with_background` (`Lis2/c;`), title `copy_link` (`Lgi0/b;`), action
`Lkx1/o0;->LINK`, id `"copy_link"`. Nothing needs adding.

The transcribed "known gap" is about *which URL* it copies, and that is not
reachable from the menu. The click path, traced from the row:

1. `Lfn1/f;.G3` adds the row; `Ls11/a0` also builds one directly at ins ~2847
   with the same `LINK` action.
2. `Ls11/x;.onClick(View)` dispatches through the interface `Ln11/e;`
   (`o0`, `h2`, `Y1`, `E0`), which has two implementors, `Lr11/w;` and
   `Lr11/e0;` — both of which delegate again and neither touches a clipboard.
3. `Lnj1/o0;.h(View, String)` compares the row id against `"pincode"` and
   `"copy_link"`; for the latter it calls
   `Lnj1/i0;.b(Context, SendableObject, Lzo2/c;, Lpr/z0;)V`.
4. That does not copy anything. It asks `Lpr/z0;.a(...)` for a builder,
   receives an `androidx/recyclerview/widget/l`, and calls
   `builder.m(SendableObject, Lzo2/c;, Lzo2/f;->COPY_LINK)` — it configures
   the *send pipeline* with a send type.
5. `Landroidx/recyclerview/widget/l;.m(...)` runs
   `Ldr2/d;.a(String, Lzo2/c;, Lzo2/i;, Lzo2/f;, Ldr2/a;)` and continues
   through a `Lc43/v;` coroutine. The URL comes from
   `Lcom/pinterest/sendshare/model/SendableObject;->e()Ljava/lang/String;`,
   which is shared by every send target.

So the string that reaches the clipboard for a pin is chosen inside a generic
key-value send pipeline, not in the menu. Changing what Copy link emits means
either hooking `SendableObject.e()` — which every send target reads, so email
and WhatsApp change too — or finding the per-send-type clipboard write. The
one `ClipData.newPlainText` reached by following this path,
`Landroidx/recyclerview/widget/l;.j(Lpl0/c;)V`, copies `invite_url` out of an
`Lpl0/c;` payload and belongs to the multi-pin *invite* flow, not to a pin;
the other five app-wide `newPlainText` sites are two-factor codes, a messaging
reply, an ads debugger and merged androidx code.

Verdict: not patch-shaped at the menu, and the "add an option" half of the item
is already shipped. Recorded rather than forced. It becomes tractable if the
work is reframed as "sanitize what every send target emits", which is Phase 4
items 2 and 3 — and those have the same problem, since they would also have to
hook `SendableObject.e()` or the share intent. That is the next thing to spike,
and it should be spiked deliberately rather than discovered mid-patch.

### Phase 4 item 5, "Open links in the default browser": partially mapped

Partial, and the open question is named so it is not re-walked.

What is confirmed:

- Both browser surfaces are **internal and unexported**, so neither is a
  system default handler and neither is reached by an incoming intent:
  `com.pinterest.componentBrowser.ComponentBrowserActivity` is
  `exported=false` with no intent filters, and
  `com.pinterest.activity.web.WebViewActivity` likewise. They are launched by
  the app, which means a patch has to change a launch decision, not a filter.
- `ComponentBrowserActivity` is a Hilt `@AndroidEntryPoint` with a Compose UI
  (`u(Lib/e0;Lck0/k;Landroidx/compose/runtime/p;I)V`, `w(Bundle)`) delegating
  to `Lck0/k;`. `com/pinterest.componentBrowser.viewModel` is a large
  obfuscated package; `componentBrowser` is referenced from `Lpr/ra;`,
  `Lad1/t;`, `Lad1/p;`, `Lck0/c;`, `Lck0/b;`, `Lcf1/h;` and `La32/a;`.
- The Custom Tabs side is `com.pinterest.browser.customTabs.chrome.ChromeTabBroadcastReceiver`
  plus `Lke0/c;`, an app-lifetime **connection**: it is constructed with
  `Application, Lie0/g;, Lhe0/c;, Lri0/b;, Lfq0/a;, Lmm/f;`, implements
  `onCustomTabsServiceConnected(ComponentName, Lw/g;)` and `onServiceDisconnected`,
  and exposes `a(ResolveInfo)Z` — "is this package a Custom Tabs provider". The
  service action is `android.support.customtabs.action.CustomTabsService`
  (`Lke0/a;`, `Lke0/c;`, `Lw/g;`). Custom Tabs is therefore opt-in per device,
  based on whether a provider is bound.
- A third, system-level path plausibly exists: `Lad2/b;` carries
  `no_browsers_found`, which is the message you get when an implicit web intent
  resolves to nothing. That is evidence a plain `ACTION_VIEW` fallback is
  reached in some branch, but the branch was not located.

What is **not** established, and is the whole patch:

- Which method decides between Custom Tabs, `ComponentBrowserActivity` and a
  plain external `ACTION_VIEW`. The likely shape is a predicate over the
  `Lke0/c;` connection plus a screen-location route
  (`Lz72/a;->COMPONENT_BROWSER_ACTIVITY`, `WEB_HOOK_ACTIVITY`), but "likely" is
  not a fingerprint.
- Whether the decision is a single boolean, as the share sheet was. If the app
  only ever has two options — Custom Tabs or its own browser — then "use the
  default browser" is a *third* option and this becomes new code rather than a
  gate flip, exactly like the board bulk-download verdict.

Do not write this patch from the notes above. Next step is to read the
`Lck0/k;` presenter and the router that owns `COMPONENT_BROWSER_ACTIVITY`, and
to find the `ACTION_VIEW` construction site; `open_external` is a red herring —
it is a deeplink query parameter in `Lxu/l;`, not a launch mode.

### P1.4 and P4.14: rendition selection is client-side, and there is one chooser

The roadmap gated both of these on the same question — does the client choose
image renditions, or are the URLs sealed server-side? **Client-side, and the
choice funnels through a single seven-instruction method.** That makes P4.14 a
two-instruction patch and reframes P1.4.

**Not sealed.** A pin's `images` field is a map keyed by size, and the app reads
four keys out of it in `Lau2/w;.d(Ljava/util/Map;)Lvu2/d1;` (`classes*.dex`):

```smali
const-string v0, "736x"
invoke-interface {p0, v0}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;
check-cast v0, Lcom/pinterest/api/model/la;      # one ImageDomain per size
...
const-string v0, "originals"                     # and the same again
```

The server sends the whole ladder and the client picks. So the roadmap's
fallback — "if URLs are server-sealed, downgrade to don't preload
full-resolution" — does not apply.

**The field mapping is derived, not guessed.** `Lau2/w;.d` reads `736x` into
`v2`, `345x` into `v3`, `236x` into `v4` and `originals` into `v1`, then ends
with

```smali
new-instance p0, Lvu2/d1;
invoke-direct {p0, v2, v3, v4, v1}, Lvu2/d1;-><init>(Lvu2/e1;Lvu2/e1;Lvu2/e1;Lvu2/e1;)V
```

and the constructor `iput`s in order `a`, `b`, `c`, `d`. Therefore:

| Field | Size |
| --- | --- |
| `Lvu2/d1;.a` | `736x` |
| `Lvu2/d1;.b` | `345x` |
| `Lvu2/d1;.c` | `236x` |
| `Lvu2/d1;.d` | `originals` |

**The chooser.** `Lvu2/d1;.b()Lvu2/e1;` walks `a`, `d`, `b`, `c` and returns
the first non-null, else the EMPTY singleton `Lvu2/e1;.e`. Stock behaviour is
therefore *736x first, originals only as a fallback* — which is sensible, since
`originals` is absent for videos and for pins the server only rendered small.

`b()` is the **only** reader of field `a` in the entire APK; the sole other hit
is the constructor's own `iput`. So this is a genuine single point, and forcing
originals is a swap of two `iget-object` reads — same opcode, same format 22c,
same width, so no width-mismatched method can result. Null safety is structural
rather than checked: the chain still falls through to every other field and then
to the singleton, so a pin with no `originals` renders exactly as before.

**Verification.** The swap was applied to the real `vu2/d1.smali`, reassembled
with `smali assemble` and disassembled again; it round-trips with `d` tested
first. Fingerprint resolves to the only method named `b()` returning
`Lvu2/e1;`.

**Known fragility, recorded rather than glossed.** Both the class and the method
name are obfuscated, so this is the AppsFlyer fragility class, not the Google
Engage pattern. The fingerprint's filters are field reads on the defining class,
and `b()` is the sole reader of `a`, so a rename fails resolution rather than
landing on an unrelated method. `Lau2/w;.d` — with its `736x`/`originals`
literals — is the place to look if it ever breaks.

### P1.4 is not that patch, and the seam does not change that

Forcing originals is P4.14. P1.4 asks for a *selector* — Cellular Low/Med,
Wi-Fi Med/High — which is a different shape of work:

- It needs a policy input (network type), which means reading
  `ConnectivityManager` at render time or caching it, plus a setting the user
  can change. That is the Phase 0 screen plus persistence, not a fingerprint.
- It needs a decision *per render site*, and the app has many: `b()` has eight
  callers (`Lay0/k;`, `Lc2/g;`, the shuffles composer, `Lax2/k3;`, `Lxg/r2;`,
  the language picker, `Lph0/e;`, `Lvu2/z1;`) plus the direct `d1.d` readers in
  the shuffles composer, `La50/m;`, `Lax2/p2;` and `Lac0/l0;`. One chooser swap
  cannot express "cellular vs wifi" across all of them without deciding whether
  the selector belongs in `b()` or at each call site — and putting it in `b()`
  means `b()` needs a network argument, which changes a signature eight call
  sites depend on.

So P1.4 stays a spike. What this investigation *does* hand it is the two facts
it was blocked on: the ladder is client-side, and the whole decision is
concentrated in `Lvu2/d1;.b()`. The honest cheap subset, if one is wanted, is
the inverse of P4.14 — cap the ladder (never prefer `originals`) — which is the
same two-instruction swap and is the direction that actually saves bandwidth.
It is deliberately **not** written, because it would be the exact opposite
preference to P4.14 on the same method, and two patches fighting over one
preference order is a design decision, not an implementation detail: they would
need mutual exclusion or a documented precedence.

# Djezzy HTTP interceptor investigation (no patch — negative result)

Request: a generic Morphe HTTP interceptor for Djezzy 3.0.9 logging
requests/responses with method, headers and URL. Investigated via
morphe-helpers against `base.apk` extracted from
`~/storage/0/Documents/VInstall/Backups/com.djezzy.internet_3.0.9.apkv`
(`com.djezzy.internet`, 3.0.9/40076; helpers cache
`~/.cache/com.djezzy.internet/3.0.9`, 12,214 classes). Verdict: **not
implementable as a Dalvik `bytecodePatch` on this target**, so no patch
was written and no target was declared. What follows is the evidence and
the capture recipe that replaces the patch.

## The app has no Java HTTP stack

- `find-class --package okhttp3`: no results. `find-string` for
  `cronet`/`Cronet`, `volley`/`Volley`, `retrofit`/`Retrofit`,
  `HttpClient`/`httpClient`: no results. The only `okhttp` strings are
  two `com.android.okhttp.internal.http.HttpTransport$...` references in
  `q1/o.smali` (framework-internal transport names, not an app client).
- `find-string HttpURLConnection`: no `const-string` results — nothing in
  app code names it as a string. Type references to `HttpURLConnection`
  exist in exactly five smali files: `FirebaseInstallationServiceClient`
  (Firebase Installations API client), `zzlm`/`zzgx`/`zbb`/`zzc` (GMS
  measurement/auth/ads internals), `Util`/`ContentBlockerHandler`
  (flutter_inappwebview plugin), and obfuscated `l5/b`, `q1/o`, `t5/h`
  (transport/data-store internals, not app API code).
- `find-string apim.djezzy` and `walk/campaign`: no results in DEX. No
  API URL, no endpoint path, no auth header name lives in Dalvik code.

## All API traffic is Dart dio in libapp.so

- `split_config.arm64_v8a.apk` → `lib/arm64-v8a/libapp.so`
  (14,681,008 bytes) contains: `package:dio` x24, `DioMixin` x3,
  `InterceptorsWrapper` x1, `pretty_dio_logger` x2, `PrettyDioLogger`
  x2, `apim.djezzy.dz` x11, `/services/walk/campaign/` x1.
- Dio's default `IOHttpClientAdapter` runs on `dart:io`'s VM-native
  sockets (BoringSSL/POSIX), never passing through `java.net`. There is
  therefore no Dalvik instruction to fingerprint: no call site, no
  choke point, nothing `matchFilters` could resolve. A "generic"
  Dalvik interceptor here would at best log Firebase/GMS/WebView
  internals and silently miss every Djezzy API call — the exact
  confident-but-wrong outcome this repo's ground rules exist to prevent.
- The closest readable generic helper,
  `Util;->makeHttpRequest(String, String, Map)HttpURLConnection`
  (unobfuscated, method+headers-map+URL signature), serves only the
  content-blocker list download: its only two callers (per `xrefs
  --callers`) are in `ContentBlockerHandler`. Hooking it would log
  ad-block-list fetches, not app traffic. Deliberately not patched.

## The requested logging already exists: PrettyDioLogger

The app bundles `package:pretty_dio_logger`, and its box-drawing output
is already visible in device logcat from the earlier Walk & Win run:

```
I flutter : GET https://apim.djezzy.dz/mobile-api/api/v1/services/walk/campaign/213772737646
I flutter : ╔╣ Response ║ GET ║ Status: 200 OK  ║ Time: 612 ms
I flutter : ║  https://apim.djezzy.dz/mobile-api/api/v1/services/walk/campaign/213772737646
I flutter : ║ Body
I flutter : ║    { "message": "Waw campaign", ... }
```

That is method + URL + status + body, emitted per request/response by
the app's own Dart interceptor. No patch is needed to obtain it.

## Capture recipe (replaces the patch)

```sh
adb logcat -c
# exercise the Djezzy flow on device, then:
adb logcat -d -v brief | rg 'flutter|PrettyDio|apim\.djezzy' > djezzy-http.log
```

`scripts/logcat-filter` in morphe-helpers is crash-oriented (filters by
package PID plus FATAL/VerifyError); for HTTP capture the `rg` line
above is the right filter because PrettyDioLogger writes through
Flutter's `print`, tagged `flutter`, not through the app PID pattern.
Request lines start with the method (`GET`/`POST`), response blocks
with `╔╣ Response`, each carrying the full URL; headers print inside
the `║ Headers` block when the logger's `requestHeader` flag is on. If
a future build stops emitting these lines, that means the logger was
compiled out or gated — which is a Dart-build change no Dalvik patch
can reverse, and the correct response is a proxy/VPN capture, not
another fingerprint.

## Cache note

`~/apks/` holds no `com.djezzy.internet/` cache (only Pinterest, 1DM,
Substack artifacts). The helpers decompile cache above is the working
copy; per `AGENTS.md` nothing derived was committed to this repo beyond
this note, and no `~/apks` extraction was created or overwritten.
