package app.drdriving.patches.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

/**
 * `DrDrivingActivity;->ShowAd()V` reveals the AdMob banner. It null-checks `gadView`,
 * reads its visibility, and only then sets it visible and calls `resume()`.
 *
 * The `BaseAdView.resume()` call is the distinctive anchor: it is the only place in
 * app code that resumes a banner after a pause cycle, and it sits inside the reveal
 * branch of this method.
 */
object ShowAdFingerprint : Fingerprint(
    definingClass = "Lcom/ansangha/drdriving/DrDrivingActivity;",
    name = "ShowAd",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/google/android/gms/ads/BaseAdView;",
            name = "resume",
            parameters = listOf(),
            returnType = "V"
        )
    )
)

/**
 * `DrDrivingActivity;->showInterstitial(Z)V` shows an AdMob interstitial. It is the
 * only app code path that calls `InterstitialAd.show`, and it is rate-limited through
 * `lastInterstitialTime` before doing so.
 */
object ShowInterstitialFingerprint : Fingerprint(
    definingClass = "Lcom/ansangha/drdriving/DrDrivingActivity;",
    name = "showInterstitial",
    returnType = "V",
    parameters = listOf("Z"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/google/android/gms/ads/interstitial/InterstitialAd;",
            name = "show",
            parameters = listOf("Landroid/app/Activity;"),
            returnType = "V"
        )
    )
)

/**
 * `DrDrivingActivity;->showVideo()V` shows an AdMob rewarded video. It null-checks
 * `rewardedAd` and otherwise returns immediately, so the only live call is
 * `RewardedAd.show` with a `OnUserEarnedRewardListener`.
 */
object ShowVideoFingerprint : Fingerprint(
    definingClass = "Lcom/ansangha/drdriving/DrDrivingActivity;",
    name = "showVideo",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/google/android/gms/ads/rewarded/RewardedAd;",
            name = "show",
            parameters = listOf(
                "Landroid/app/Activity;",
                "Lcom/google/android/gms/ads/OnUserEarnedRewardListener;"
            ),
            returnType = "V"
        )
    )
)

/**
 * `DrDrivingActivity;->createAdMobBannerView()V` builds the banner AdView, loads it,
 * and triggers the first interstitial and rewarded ad loads. It is called from
 * `initAds()` at startup, so the banner is fetched in the background even if the user
 * never opens a menu. `BaseAdView.loadAd` is the ad request itself and is unique to
 * this method in app code.
 */
object CreateBannerFingerprint : Fingerprint(
    definingClass = "Lcom/ansangha/drdriving/DrDrivingActivity;",
    name = "createAdMobBannerView",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/google/android/gms/ads/BaseAdView;",
            name = "loadAd",
            parameters = listOf("Lcom/google/android/gms/ads/AdRequest;"),
            returnType = "V"
        )
    )
)