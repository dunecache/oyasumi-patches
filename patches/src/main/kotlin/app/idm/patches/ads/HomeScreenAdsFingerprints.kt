package app.idm.patches.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

/**
 * The banner that is on the home screen footer is not one of 1DM's own banner classes.
 * It is Appodeal's `com.appodeal.ads.BannerView`, declared in
 * `res/layout/activity_main_bottom.xml` as the last child of the drawer's content column,
 * `android:visibility="gone"` and `android:layout_height="wrap_content"`, with the id
 * `appodealBannerView` (`0x7f0a018f`). A uiautomator dump of 18.2 shows that view populated
 * and on screen with the SDK's own `WebView` inside it, so nothing reached through
 * `BannerManager` or through the `Li/ru;` promo object can suppress it.
 *
 * **1DM brings Appodeal up twice, by two unrelated methods, and both register that same
 * banner view id.** Getting this wrong is what made v0.6.0-dev.8 ineffective on a device:
 * it suppressed the second one, the banner stayed. The two are:
 *
 *  - `Li/rm;->ۦۖۢ(Lacr/browser/lightning/activity/MyAppCompatActivity;Li/m15;)V` at start-up,
 *    which is `AppodealStartupInitFingerprint` below. It calls `setAutoCache(false, 7)`,
 *    `muteVideosIfCallsMuted(true)`, `setBannerViewId(2131362191)`, `initialize` with the
 *    publisher key, and installs both the banner and the interstitial callbacks.
 *  - `Lidm/internet/download/manager/e;-><init>()V`, reached only when Appodeal's consent
 *    flow completes -- its sole caller is `MyAppCompatActivity$2`, the
 *    `ConsentManagerError` callback. That is `AppodealAdInitFingerprint`.
 *
 * `fetch()` below is the third piece: it is the only caller of `Appodeal.cache`, and it does
 * so four times, with the banner, the interstitial or both.
 *
 * A scan of every method outside the SDK's own `com.appodeal` package for `Appodeal.initialize`,
 * `setBannerViewId`, `setAutoCache`, `setBannerCallbacks`, `cache`, `show` and `destroy`
 * returns these three methods and five others, and the five are all read-only or interstitial:
 * `Lidm/internet/download/manager/d;->ۦۡۗ`/`ۦۡۚ` ask `isLoaded(4)` and `show(Activity, 64)`,
 * where `64` is `BANNER_VIEW`, purely to tell 1DM's own `BannerManager` via
 * `setNetworkAdShowingAndNotify` that a network ad is on screen;
 * `d;->ۦۤۥ(...)V` and `Li/rm;->ۦۗۤ`/`ۦۗۡ` do the same for the interstitial. None of them can
 * bring the SDK up or register the view.
 *
 * That scan has to cover the whole APK and not just `Lidm/` and `Lacr/`. `Li/rm;` is in the
 * obfuscated `Li/` package, and a scan filtered to the app's own package prefixes reported
 * that no code registered the banner -- which is what the first version of this file asserted.
 */

/**
 * `Li/rm;->ۦۖۢ(Lacr/browser/lightning/activity/MyAppCompatActivity;Li/m15;)V` is 1DM's
 * start-up bring-up of the Appodeal SDK, and the only code path that makes the footer banner
 * appear. It is a `BannerCallbacks` and `InterstitialCallbacks` implementation held in the
 * singleton `Li/rm;`, reached through `Li/rm;->ۦۖۗ()`.
 *
 * The class, the method and both parameter types are obfuscated and change between releases,
 * so none of them is pinned; only `returnType = "V"` is. The chain is what identifies it, and
 * every anchor in it is an Appodeal member name, the publisher's own key, or the banner view
 * id literal `2131362191` (`0x7f0a018f`), all fixed for this app. The chain is in increasing
 * instruction order: `setAutoCache` at 20, the view id at 23, `setBannerViewId` at 24, the key
 * at 33, `initialize` at 39, `setBannerCallbacks` at 40, `setInterstitialCallbacks` at 41.
 *
 * The last two filters are what keep this apart from
 * `Lidm/internet/download/manager/e;-><init>()V`, which repeats the key, the view id,
 * `initialize` and `setBannerCallbacks` but calls neither `setAutoCache` nor
 * `setInterstitialCallbacks`.
 *
 * The patch returns from index 1 rather than index 0 on purpose. Index 0 stores the callback
 * argument into `Li/rm;->ۦۖ۠Li/m15;`, and every one of `onBannerLoaded`, `onBannerShown`,
 * `onBannerClicked`, `onBannerFailedToLoad`, `onInterstitialLoaded`, `onInterstitialShown` and
 * `onInterstitialFailedToLoad` reads that field and calls through it. Leaving the store in
 * place and stopping before the SDK is touched costs nothing and keeps those callbacks safe
 * even if Appodeal were initialised by some path this patch does not see.
 */
object AppodealStartupInitFingerprint : Fingerprint(
    // `parameters` is deliberately not declared. One of the two declared types is
    // `Li/m15;`, which is obfuscated and changes between releases, and a partial parameter
    // list is not a constraint Morphe can express; the chain below is what has to carry the
    // specificity, and it resolves to exactly one method in all eleven DEX files.
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = "Lcom/appodeal/ads/Appodeal;",
            name = "setAutoCache",
            parameters = listOf("I", "Z"),
            returnType = "V"
        ),
        literal(2131362191, listOf(Opcode.CONST)),
        methodCall(
            definingClass = "Lcom/appodeal/ads/Appodeal;",
            name = "setBannerViewId",
            parameters = listOf("I"),
            returnType = "V"
        ),
        string("b1eafec41c5ab762a5acc356f9526305d05536819d4d0184"),
        methodCall(
            definingClass = "Lcom/appodeal/ads/Appodeal;",
            name = "initialize",
            parameters = listOf(
                "Landroid/content/Context;",
                "Ljava/lang/String;",
                "I",
                "Lcom/appodeal/ads/initializing/ApdInitializationCallback;"
            ),
            returnType = "V"
        ),
        methodCall(
            definingClass = "Lcom/appodeal/ads/Appodeal;",
            name = "setBannerCallbacks",
            parameters = listOf("Lcom/appodeal/ads/BannerCallbacks;"),
            returnType = "V"
        ),
        methodCall(
            definingClass = "Lcom/appodeal/ads/Appodeal;",
            name = "setInterstitialCallbacks",
            parameters = listOf("Lcom/appodeal/ads/InterstitialCallbacks;"),
            returnType = "V"
        )
    )
)

/**
 * `Li/rm;->fetch()V` is the only caller of `Appodeal.cache` in the whole APK, and it is what
 * turns a cached ad into a request for one. It picks the mask from three gates -- whether the
 * banner is loaded and can be shown, whether the premium check passed, and the persisted
 * `AppodealNetwork` network preference -- and then calls `cache` with `4` (`BANNER`), `3`
 * (`INTERSTITIAL`) or `7` (both).
 *
 * Suppressing it is redundant while `AppodealStartupInitFingerprint` holds, because an
 * uninitialised SDK ignores `cache`, and it is kept anyway for the same reason the ADM patch
 * suppresses both its initialiser and its display routines: the two claims are independent, and
 * only one of them being wrong should not be enough to bring the banner back.
 *
 * The `AppodealNetwork` preference key and the two consecutive `cache` calls identify it. The
 * obfuscated gate helpers `ۦۖۤ()Z` and `ۦۖۛ()Z` and the obfuscated preference accessors are
 * deliberately not used.
 */
object AppodealFetchFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("AppodealNetwork"),
        methodCall(
            definingClass = "Lcom/appodeal/ads/Appodeal;",
            name = "cache",
            parameters = listOf("Landroid/app/Activity;", "I"),
            returnType = "V"
        ),
        methodCall(
            definingClass = "Lcom/appodeal/ads/Appodeal;",
            name = "cache",
            parameters = listOf("Landroid/app/Activity;", "I"),
            returnType = "V"
        )
    )
)

/**
 * `Lidm/internet/download/manager/e;-><init>()V` is the *second* Appodeal bring-up, reached
 * only after Appodeal's consent flow completes, and it repeats the whole sequence: the same
 * publisher key, the same `setBannerViewId(2131362191)`, `initialize`, `setBannerCallbacks` and
 * a `cache`. Suppressing only the start-up path is not enough, because this one re-registers
 * the banner view afterwards; suppressing only this one, as v0.6.0-dev.8 did, is what left
 * the banner on screen.
 *
 * It is also where 1DM starts `AmazonService` and initialises Smaato, so returning early stops
 * those two as well. That is deliberate and in scope for an ad patch: `AmazonService`'s only
 * two readers are `BannerView.setAd` and `d;->ۦۤۥ`, both already suppressed or guarded here,
 * and `ActivityLifecycleListener.onStateChanged` tests `AmazonService.isInitialized()` before
 * it calls `start`/`stop`.
 *
 * Returning at index 0 is safe because this singleton's only caller,
 * `Lidm/internet/download/manager/d;->ۦۜ۟(...)V`, constructs it solely on the ads-enabled
 * branch and otherwise calls `Li/a6;->ۦۖ۟(...)`, and the class's two other instance methods
 * are empty list callbacks, so the `Random` field left unwritten is never read.
 *
 * The chain omits the `setBannerViewId` literal that `AppodealStartupInitFingerprint` uses,
 * and ends at `cache` rather than `setInterstitialCallbacks`, which is what separates the two.
 */
object AppodealAdInitFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/appodeal/ads/Appodeal;",
            name = "setBannerViewId",
            parameters = listOf("I"),
            returnType = "V"
        ),
        string("b1eafec41c5ab762a5acc356f9526305d05536819d4d0184"),
        methodCall(
            definingClass = "Lcom/appodeal/ads/Appodeal;",
            name = "initialize",
            parameters = listOf(
                "Landroid/content/Context;",
                "Ljava/lang/String;",
                "I",
                "Lcom/appodeal/ads/initializing/ApdInitializationCallback;"
            ),
            returnType = "V"
        ),
        methodCall(
            definingClass = "Lcom/appodeal/ads/Appodeal;",
            name = "setBannerCallbacks",
            parameters = listOf("Lcom/appodeal/ads/BannerCallbacks;"),
            returnType = "V"
        ),
        methodCall(
            definingClass = "Lcom/appodeal/ads/Appodeal;",
            name = "cache",
            parameters = listOf("Landroid/app/Activity;", "I"),
            returnType = "V"
        )
    )
)

/**
 * `Lacr/browser/lightning/view/BannerManager;->load(Z)V` fetches the banner ad
 * configuration into `bannerInfoList` and flips `mLoaded` on. It is the only
 * method that fills the list: `resume()` refuses to start the rotation timer
 * while `mLoaded` is false or the list is empty, and the banner is only ever
 * posted to the view after the timer runs.
 *
 * The whole method is `monitor-enter` synchronised and never leaves the state it
 * creates, so the ad manager is identified by its own field traffic rather than
 * by the obfuscated `Lidm/...;d;` calls that supply the ad list.
 */
object BannerManagerLoadFingerprint : Fingerprint(
    definingClass = "Lacr/browser/lightning/view/BannerManager;",
    name = "load",
    returnType = "V",
    parameters = listOf("Z"),
    filters = listOf(
        opcode(Opcode.MONITOR_ENTER, location = InstructionLocation.MatchFirst()),
        fieldAccess(
            definingClass = "this",
            name = "mDisabled",
            type = "Ljava/util/concurrent/atomic/AtomicBoolean;",
            opcode = Opcode.IGET_OBJECT
        ),
        methodCall(
            definingClass = "Ljava/util/concurrent/atomic/AtomicBoolean;",
            name = "set",
            parameters = listOf("Z"),
            returnType = "V"
        ),
        fieldAccess(
            definingClass = "this",
            name = "mLoaded",
            type = "Ljava/util/concurrent/atomic/AtomicBoolean;",
            opcode = Opcode.IGET_OBJECT
        ),
        fieldAccess(
            definingClass = "this",
            name = "mTimer",
            type = "Ljava/util/Timer;",
            opcode = Opcode.IGET_OBJECT
        ),
        methodCall(
            definingClass = "Ljava/util/Timer;",
            name = "cancel",
            parameters = listOf(),
            returnType = "V"
        ),
        // The stored ad is nulled before the list is refilled, and its type is an
        // obfuscated `Li/ru;` that changes between releases, so only its name is
        // pinned here.
        fieldAccess(
            definingClass = "this",
            name = "currentBannerInfo",
            opcode = Opcode.IPUT_OBJECT
        ),
        fieldAccess(
            definingClass = "this",
            name = "bannerInfoList",
            type = "Ljava/util/List;",
            opcode = Opcode.IGET_OBJECT
        ),
        methodCall(
            definingClass = "Ljava/util/List;",
            name = "clear",
            parameters = listOf(),
            returnType = "V"
        ),
        methodCall(
            definingClass = "Ljava/util/List;",
            name = "addAll",
            parameters = listOf("Ljava/util/Collection;"),
            returnType = "Z"
        ),
        methodCall(
            definingClass = "Ljava/util/List;",
            name = "add",
            parameters = listOf("Ljava/lang/Object;"),
            returnType = "Z"
        )
    )
)

/**
 * `Lacr/browser/lightning/view/BannerView;->setAd(Ljava/lang/Integer;Li/ru;)V` is the
 * single point where the home screen banner is rendered. It is reached from
 * `onFinishInflate()` through `setupAdView()` and from the bus event that
 * `BannerManager.postAd()` publishes, so no other path can reveal the banner.
 *
 * The method returns early when the app's ads-disabled flag is set, and its own
 * "hide the banner" branch is `setVisibilityIfChanged(this, GONE)`. The
 * `AmazonService.getBannerBackfillAd("any")` call is the only consumer of the
 * `any` slot string and the only place the Amazon banner is inflated, which makes
 * the string and the call a stable pair for this method.
 */
object BannerViewSetAdFingerprint : Fingerprint(
    definingClass = "Lacr/browser/lightning/view/BannerView;",
    name = "setAd",
    returnType = "V",
    parameters = listOf("Ljava/lang/Integer;", "Li/ru;"),
    filters = listOf(
        fieldAccess(
            definingClass = "this",
            name = "icon",
            type = "Landroid/widget/ImageView;",
            opcode = Opcode.IGET_OBJECT
        ),
        fieldAccess(
            definingClass = "this",
            name = "title",
            type = "Landroid/widget/TextView;",
            opcode = Opcode.IGET_OBJECT
        ),
        fieldAccess(
            definingClass = "this",
            name = "action",
            type = "Landroid/widget/Button;",
            opcode = Opcode.IGET_OBJECT
        ),
        methodCall(
            definingClass = "Landroid/view/View;",
            name = "getContext",
            parameters = listOf(),
            returnType = "Landroid/content/Context;"
        ),
        methodCall(
            definingClass = "Lacr/browser/lightning/view/BannerManager;",
            name = "isNetworkAdShowing",
            parameters = listOf("Landroid/app/Activity;"),
            returnType = "Z"
        ),
        string("any"),
        methodCall(
            definingClass = "Lidm/internet/download/manager/amazon/AmazonService;",
            name = "getBannerBackfillAd",
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Lcom/amazon/device/ads/DTBAdResponse;"
        ),
        fieldAccess(
            definingClass = "this",
            name = "aps_banner",
            type = "Landroid/view/ViewGroup;",
            opcode = Opcode.IGET_OBJECT
        ),
        // The banner's only click target is installed on the same view, immediately
        // before it is revealed.
        methodCall(
            definingClass = "Landroid/view/View;",
            name = "setOnClickListener",
            parameters = listOf("Landroid/view/View\$OnClickListener;"),
            returnType = "V"
        )
    )
)

/**
 * `Lidm/internet/download/manager/d;->ۦۜۡ()Li/ru;` is the single source of the "install
 * 1DM+" banner ad, and it is a static factory that builds the `Li/ru;` ad object from
 * literals: a base64 PNG icon, the copy "Install <b>1DM+</b> for an Ad free experience
 * and support developement of the app", the label "Install", the Play Store package
 * `idm.internet.download.manager.plus`, a `utm_` campaign tag, the accent colour
 * `#43A047`, and a 30 000 ms click-through delay.
 *
 * It is worth being precise about what this is not, because the first attempt at this
 * patched the wrong class and did nothing. 1DM has four app-owned banner classes --
 * `Lacr/browser/lightning/view/BannerView`, `Lidm/internet/download/manager/BannerView`,
 * `manager/NewBannerView` and a fourth -- and none of them is the view that is on screen:
 * the footer banner is Appodeal's `com.appodeal.ads.BannerView`, which draws whatever the
 * ad SDK hands it and never an `Li/ru;`. This factory is what the app-owned classes draw,
 * so suppressing it covers 1DM's own promo path, and `AppodealAdInitFingerprint` is what
 * covers the banner that is actually visible.
 *
 * Suppressing the factory rather than any one renderer is what makes this complete: the
 * promo reaches the screen through four separate call sites --
 * `BannerManager.load(Z)V` and `BannerManager.getNewBannerInfo(...)`, which feed the ad
 * rotation, and `Li/s82;->ۦۖۢ(...)V` and `Li/s82;->ۦۖۦ(...)Z`, which reach
 * `manager.NewBannerView` directly. Redirecting `BannerManager.load()` to `disable()`
 * leaves the other three, which is why the prompt survived it.
 *
 * All four call sites null-check the result and skip the banner when it is null, so
 * returning null here is the behaviour the app already has a path for. The method is
 * `static` with `.registers 3`, so `v0` is the return slot and the replacement is
 * `const/4 v0, 0` followed by `return-object v0` -- `return-void` is not legal on a
 * method that returns a reference.
 *
 * The chain identifies the method by its own literals: the `PlayStore` package name is
 * unique to this factory, and the copy, the label and the click-through delay sit either
 * side of it. None of the obfuscated member names are used.
 */
object IdmPlusBannerFingerprint : Fingerprint(
    definingClass = "Lidm/internet/download/manager/d;",
    name = "ۦۜۡ",
    returnType = "Li/ru;",
    parameters = listOf(),
    filters = listOf(
        // The cached singleton this factory fills in and hands back.
        fieldAccess(
            definingClass = "Lidm/internet/download/manager/d;",
            name = "ۦۗۥ",
            type = "Li/ru;",
            opcode = Opcode.SGET_OBJECT
        ),
        string("Install <b>1DM+</b> for an Ad free experience and support developement of the app"),
        string("Install"),
        // The 30 s click-through delay. It is written *before* the Play Store id in the
        // method body, so the chain has to visit it here; filters match in increasing
        // instruction order, and putting it last would leave it unreachable.
        literal(30000, listOf(Opcode.CONST_16)),
        // The Play Store id of the paid edition. Unique to this method in the app.
        string("idm.internet.download.manager.plus"),
        string("utm_source=1DM&utm_medium=App&utm_campaign=DefaultBanner")
    )
)

/**
 * `Lidm/internet/download/manager/manager/NewBannerView;->ۦۖۦ(Li/ru;)V` is the renderer
 * for 1DM's own fallback banner -- the view that is actually on screen in the
 * uiautomator dump (`defaultBannerViewNew` -> `default_banner` with `icon`, `title`
 * "Play fun Quizzes and Get Rewards" and an `action` "PLAY" button). It is not
 * Appodeal's `WebView`, not the `Lacr/.../BannerView` that `BannerViewSetAdFingerprint`
 * hides, and not the "Install 1DM+" promo that `IdmPlusBannerFingerprint` nulls: that
 * copy comes from the server-side ad config, and `Li/s82` drives this renderer directly,
 * bypassing `BannerManager.load()` entirely.
 *
 * Measured against the on-device 18.2 build (`classes11.dex`): 122 instructions,
 * `.registers 6`, `this` in `v4` and the `Li/ru;` ad in `v5`. The first three
 * instructions read the three child views whose ids `ۦۖۡ()V` binds with `findViewById`
 * (`2131362838` icon, `2131364059` title, `2131361850` action), then the method paints
 * the bitmap, the text and the button, installs the click listener (index 116) and
 * reveals itself.
 *
 * Neither the method name (`ۦۖۦ`) nor the field names (`ۦۖۡ`/`ۦۖۦ`/`ۦۖۧ`) nor the
 * `Li/ru;` accessor names are pinned beyond the 18.2 parameter declaration: all are
 * obfuscated and change between releases. The chain is four unobfuscated SDK calls in
 * increasing instruction order -- `setImageBitmap` (29), `TextUtils.isEmpty` (34),
 * `TextView.setText` (54), `setOnClickListener` (116) -- which resolves to exactly this
 * method inside `NewBannerView` and to nothing else there.
 */
object FallbackBannerRendererFingerprint : Fingerprint(
    definingClass = "Lidm/internet/download/manager/manager/NewBannerView;",
    returnType = "V",
    parameters = listOf("Li/ru;"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/widget/ImageView;",
            name = "setImageBitmap",
            parameters = listOf("Landroid/graphics/Bitmap;"),
            returnType = "V"
        ),
        methodCall(
            definingClass = "Landroid/text/TextUtils;",
            name = "isEmpty",
            parameters = listOf("Ljava/lang/CharSequence;"),
            returnType = "Z"
        ),
        methodCall(
            definingClass = "Landroid/widget/TextView;",
            name = "setText",
            parameters = listOf("Ljava/lang/CharSequence;"),
            returnType = "V"
        ),
        methodCall(
            definingClass = "Landroid/view/View;",
            name = "setOnClickListener",
            parameters = listOf("Landroid/view/View\$OnClickListener;"),
            returnType = "V"
        )
    )
)
