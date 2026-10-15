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
        "banner, including the built-in \"install 1DM+\" ad and the server-driven fallback " +
        "banner (defaultBannerViewNew), is suppressed at its source, never rendered, " +
        "and its footer slot is collapsed.",
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

        // The fallback banner is 1DM's own view, not Appodeal's, and none of the above
        // touches it: the dump shows `defaultBannerViewNew` -> `default_banner` with an
        // `icon`, a `title` ("Play fun Quizzes and Get Rewards") and an `action` ("PLAY")
        // button, while Appodeal's banner would hold a `WebView`. `BannerManager.load()`
        // -> `disable()` does not stop it either, because `Li/s82` drives
        // `manager.NewBannerView` directly, bypassing `BannerManager`, and
        // `BannerViewSetAdFingerprint` patches `acr.browser...BannerView`, a different
        // class. So the view that is on screen is hidden here, with the same trick as
        // the `setAd` patch.
        //
        // Hiding the banner alone leaves an empty slot: the banner is `<include>`d into
        // a vertical `LinearLayout` column (`activity_main.xml`, `activity_main_bottom.xml`
        // and `activity_torrent_details.xml` all share the identical column) that carries
        // `android:minHeight="60dip"`, so the column keeps a 60dp strip even with every
        // child `GONE`. The column holds nothing but ad views (the two banner includes,
        // two already-`GONE` promo slots, the `GONE` Appodeal view), and it is the direct
        // parent of `defaultBannerViewNew` in all three layouts, so hiding the parent
        // collapses the slot without touching any legitimate view.
        //
        // `NewBannerView.ۦۖۦ(Li/ru;)V` is the renderer: 122 instructions, `.registers 6`
        // with `this` in `v4` and the ad in `v5`, measured against the on-device 18.2
        // `classes11.dex`. Index 0 writes `v0` (`iget-object v0, v4`) and `v1` is still
        // unassigned there, so both are safe scratch locals before the original body runs.
        // `GONE` (8) needs `const/16`, the same width the app itself uses.
        // `getParent()` takes no argument, so its 35c list names only the receiver
        // (`{v4}`); `setVisibility(I)V` takes one argument, so its list names the
        // receiver *and* the int (`{v0, v1}`); naming only the receiver is the arity
        // crash that took down v0.3.4. `this` is never null and an inflated layout child
        // always has a parent, so no null check is needed; the parent is a `ViewGroup`,
        // hence a `View`, so the `check-cast` cannot fail.
        FallbackBannerRendererFingerprint.method.addInstructions(
            0,
            "invoke-virtual {v4}, Landroid/view/View;->getParent()Landroid/view/ViewParent;\n" +
                "move-result-object $VISIBILITY_REGISTER\n" +
                "check-cast $VISIBILITY_REGISTER, Landroid/view/View;\n" +
                "const/16 v1, $GONE\n" +
                "invoke-virtual {$VISIBILITY_REGISTER, v1}, " +
                "Landroid/view/View;->setVisibility(I)V\n" +
                "const/16 $VISIBILITY_REGISTER, $GONE\n" +
                "invoke-virtual {v4, $VISIBILITY_REGISTER}, " +
                "Landroid/view/View;->setVisibility(I)V\n" +
                "return-void"
        )
    }
}
