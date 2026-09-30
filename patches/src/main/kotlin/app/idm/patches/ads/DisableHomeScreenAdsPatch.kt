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

/**
 * `Lidm/internet/download/manager/BannerView;->ۦۖۤ()V` is `.registers 8` with `this` in
 * `v7` and no parameters, so `v0` is a free local there and the receiver is `v7`.
 */
private const val UPSELL_RECEIVER = "v7"

@Suppress("unused")
val disableHomeScreenAdsPatch = bytecodePatch(
    name = "Disable home screen ads",
    description = "Keep 1DM's home screen banner from loading, rotating, or rendering, " +
        "and hide the built-in \"install 1DM+\" upsell strip.",
    default = true
) {
    compatibleWith(COMPATIBILITY_1DM)

    execute {
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

        // The "install 1DM+" strip is a separate view in a separate class, and neither of
        // the edits above can touch it. `onFinishInflate()` calls `ۦۖۤ()` unconditionally,
        // and nothing in that path consults the ad configuration, so redirecting `load()`
        // to `disable()` and hiding the ad container both leave the upsell on screen.
        //
        // It is hidden rather than merely emptied. `res/layout/banner_view.xml` gives this
        // view a fixed `layout_height` of 55dp, so returning early without hiding it
        // would trade a visible strip for an empty one. GONE is also what the view's own
        // timer task already treats as "stop": `BannerView$a.run()` reads `getVisibility()`
        // and calls `Timer.cancel()` when it is 8, so hiding it also disarms the 250 ms
        // click-through timer that would otherwise keep re-posting itself.
        BannerViewUpsellFingerprint.let { fingerprint ->
            fingerprint.method.addInstructions(
                0,
                "const/16 $VISIBILITY_REGISTER, $GONE\n" +
                    "invoke-virtual {$UPSELL_RECEIVER, $VISIBILITY_REGISTER}, " +
                    "Landroid/view/View;->setVisibility(I)V"
            )
        }
    }
}
