package app.idm.patches.ads

import app.idm.patches.shared.Constants.COMPATIBILITY_1DM
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

/**
 * `BannerView.setAd` holds `this` in `v5` and its two arguments in `v6` and `v7`, so
 * `v0` is an int scratch local that is only ever read after the method has returned.
 * `const/4` cannot encode `View.GONE` (8), so the constant is written with `const/16`,
 * the same width the app itself uses to load it.
 */
private const val VISIBILITY_REGISTER = "v0"

private const val GONE = "0x8"

@Suppress("unused")
val disableHomeScreenAdsPatch = bytecodePatch(
    name = "Disable home screen ads",
    description = "Keep the home screen banner from loading, rotating, or rendering. The banner " +
        "in the footer is Appodeal's, so the ad SDK is never brought up; 1DM's own promo " +
        "banner, including the built-in \"install 1DM+\" ad, is suppressed at its source.",
    default = true
) {
    compatibleWith(COMPATIBILITY_1DM)

    execute {
        // The banner on the home screen footer is Appodeal's, so these three are the edits
        // that actually remove what is on screen. 1DM brings the SDK up by two unrelated
        // methods and both register the same banner view id, so both have to go: the
        // start-up path, the consent-completion path, and the one caller of `Appodeal.cache`.
        //
        // Suppressing only the second of those is what left the banner on screen in
        // v0.6.0-dev.8, so the start-up one is listed first and returns from index 1. That
        // index is after the store into `Li/rm;->ۦۖ۠`, which every `onBanner*` and
        // `onInterstitial*` callback in that class reads and calls through, so the callbacks
        // stay safe even though the SDK is never brought up.
        AppodealStartupInitFingerprint.method.addInstructions(1, "return-void")
        AppodealFetchFingerprint.method.addInstructions(0, "return-void")
        AppodealAdInitFingerprint.method.addInstructions(0, "return-void")

        // The app already has a no-ads state: `BrowserApp` calls `disable()` instead of
        // `load()` when the ad configuration says the banner is off, and `disable()` sets
        // `mDisabled`, empties `bannerInfoList`, drops the current ad, and cancels the
        // rotation timer. `load()` is redirected to that same state, so the banner is
        // never populated and `resume()` never starts the timer, while every other entry
        // point keeps working normally. The receiver register is read from the
        // `monitor-enter` that opens the method rather than hardcoded, because the method
        // is `synchronized` and the inserted call runs before the lock is taken.
        //
        // The braces around the register are mandatory, and so is the register's `v`
        // prefix: smali's grammar requires
        // `OPEN_BRACE register_list CLOSE_BRACE` for a 35c invoke, and a register list
        // holds register names, not bare numbers. So this renders `{v1}` and not `{1}`.
        // `addInstructions` builds the dummy method from the matched method's own
        // parameters and register count, so the register numbers written here are the
        // ones the target method already uses.
        BannerManagerLoadFingerprint.let { fingerprint ->
            val entry = fingerprint.instructionMatches[0]
            val receiver = entry.getInstruction<OneRegisterInstruction>().getRegisterA()

            fingerprint.method.addInstructions(
                0,
                "invoke-virtual {v$receiver}, Lacr/browser/lightning/view/BannerManager;->disable()V\n" +
                    "return-void"
            )
        }

        // `setAd` is the only renderer for the banner: it is called once from
        // `onFinishInflate()` and once per bus event that `postAd()` publishes, and the
        // app's own "a network ad is on screen" branch is exactly this call with `GONE`.
        // Replacing the body with that call also covers a banner event that arrives from
        // a path outside the banner manager.
        BannerViewSetAdFingerprint.let { fingerprint ->
            // A 22c field read names its registers A (destination) and B (object), and
            // the object here is the view itself.
            val firstFieldRead = fingerprint.instructionMatches[0]
            val receiver = firstFieldRead.getInstruction<TwoRegisterInstruction>().getRegisterB()

            // `setVisibility(I)V` takes one argument, so the 35c register list holds the
            // receiver *and* the visibility int. Passing only the receiver is not a
            // narrower encoding, it is a different arity: the verifier rejects it with
            // "expected 1 argument registers, method signature has 2 or more" and the
            // class is rejected outright, taking the whole activity's layout down with
            // it. The earlier braces-and-`v`-prefix fix satisfied smali's grammar but not
            // the arity, so it compiled and still crashed on launch.
            fingerprint.method.addInstructions(
                0,
                "const/16 $VISIBILITY_REGISTER, $GONE\n" +
                    "invoke-virtual {v$receiver, $VISIBILITY_REGISTER}, " +
                    "Landroid/view/View;->setVisibility(I)V\n" +
                    "return-void"
            )
        }

        // The "install 1DM+" banner is not a view problem. 1DM builds it as an ordinary
        // banner ad -- an `Li/ru;` object carrying the copy, the Play Store package id and
        // a 30 s click-through -- from `Lidm/internet/download/manager/d;->ۦۜۡ()Li/ru;`.
        // Suppressing that factory is what removes it, because the promo reaches the
        // screen through four call sites and only one of them is `BannerManager.load()`:
        // `getNewBannerInfo` feeds the rotation, and `Li/s82;->ۦۖۢ`/`ۦۖۦ` drive
        // `manager/NewBannerView` directly. The first attempt hid a view instead and the
        // prompt survived, because the class it patched was never the one on screen.
        //
        // Every one of those four sites null-checks the result and skips the banner, so
        // returning null is a state the app already handles. The method is static with
        // `.registers 3`, so `v0` is the return slot; `return-void` would be illegal on
        // a reference-returning method.
        IdmPlusBannerFingerprint.let { fingerprint ->
            fingerprint.method.addInstructions(
                0,
                "const/4 v0, 0\n" +
                    "return-object v0"
            )
        }
    }
}
