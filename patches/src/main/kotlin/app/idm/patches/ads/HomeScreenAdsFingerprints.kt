package app.idm.patches.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

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
 * `Lidm/internet/download/manager/BannerView;->ۦۖۤ()V` builds the app's own "install
 * 1DM+" strip, and is the one thing on the home screen that the ad-SDK patches above
 * cannot reach.
 *
 * This is not an ad. It is a house upsell for the paid edition, with the text
 * `1DM+: Fastest download manager (<b>$1.99</b>)` and an `INSTALL` button baked into
 * `res/layout/banner_view.xml` as literals, inflated by this app's own
 * `onFinishInflate()`. It lives in a different class from the ad banner
 * (`Lacr/browser/lightning/view/BannerView;`) and a different dex (`classes9.dex`),
 * never consults the ad configuration, and is started by a 250 ms `Timer` rather than
 * by the ad rotation. That is why redirecting `BannerManager.load()` to `disable()`
 * and hiding the container from `setAd()` both leave it on screen, and why the prompt
 * survives the whole "Disable home screen ads" patch.
 *
 * The method is identified by the upsell text it feeds to `Html.fromHtml()` and
 * `TextView.setText()`, and by the `Timer.schedule()` that drives the click-through, so
 * the chain does not depend on the obfuscated member names `ۦۖ۠`/`ۦۖۡ`/`ۦۖۦ`/`ۦۖۧ` or on
 * the view ids, all of which are release-specific.
 */
object BannerViewUpsellFingerprint : Fingerprint(
    definingClass = "Lidm/internet/download/manager/BannerView;",
    name = "ۦۖۤ",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            definingClass = "this",
            name = "ۦۖۚ",
            type = "Z",
            opcode = Opcode.IGET_BOOLEAN
        ),
        // The upsell copy is the only string this class ever loads through `Html`.
        methodCall(
            definingClass = "Landroid/text/Html;",
            name = "fromHtml",
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Landroid/text/Spanned;"
        ),
        methodCall(
            definingClass = "Landroid/widget/TextView;",
            name = "setText",
            parameters = listOf("Ljava/lang/CharSequence;"),
            returnType = "V"
        ),
        fieldAccess(
            definingClass = "this",
            name = "ۦۖۚ",
            type = "Z",
            opcode = Opcode.IPUT_BOOLEAN
        ),
        methodCall(
            definingClass = "Ljava/util/Timer;",
            name = "schedule",
            parameters = listOf(
                "Ljava/util/TimerTask;",
                "J",
                "J"
            ),
            returnType = "Ljava/util/Timer;"
        )
    )
)
