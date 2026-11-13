package app.drdriving.patches.ads

import app.drdriving.patches.shared.Constants.COMPATIBILITY_DRDRIVING
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val disableAdsPatch = bytecodePatch(
    name = "Disable ads",
    description = "Skip AdMob banner creation and all three AdMob reveal paths, so no " +
        "banner, interstitial or rewarded video is loaded or shown.",
    default = true
) {
    compatibleWith(COMPATIBILITY_DRDRIVING)

    execute {
        // Four early `return-void`s, one per ad object. Every target is a small public
        // method on `DrDrivingActivity` with no parameters, so the payload is a single
        // instruction and no register or width work is involved.
        //
        // `createAdMobBannerView` is the one that matters most: it is called from
        // `initAds()` at startup, so without it the banner is fetched in the background
        // even if the user never opens a menu. It also triggers the first
        // `requestNewInterstitial` and `requestNewReward` calls, so suppressing it
        // leaves `interstitial` and `rewardedAd` null as well.
        //
        // The three reveal methods are patched too because `requestNewInterstitial`
        // has a second caller (`startOnlineGame`) that is not reachable from
        // `createAdMobBannerView`, so a loaded interstitial could still be shown if
        // only the banner creation were suppressed. Each reveal null-checks its own
        // ad object first, so the early return is safe: nothing after index 0 depends
        // on a value computed before it.
        //
        // `initAds()` itself is deliberately left alone. It also starts the
        // `MobileAds.initialize` thread and backs `lastInterstitialTime` off by 30 s;
        // cutting it would skip unrelated work and the banner creation is the narrower
        // target. `DestroyAds`, `HideAd`, `ShowLeaderboards`, `CheckAd` and the `p`
        // AdListener are untouched.

        CreateBannerFingerprint.method.addInstructions(0, "return-void")
        ShowAdFingerprint.method.addInstructions(0, "return-void")
        ShowInterstitialFingerprint.method.addInstructions(0, "return-void")
        ShowVideoFingerprint.method.addInstructions(0, "return-void")
    }
}