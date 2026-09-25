package app.adm.patches.ads

import app.adm.patches.shared.Constants.COMPATIBILITY_ADM
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val disableAdsPatch = bytecodePatch(
    name = "Disable ads",
    description = "Skip ADM's app-level ad initialization and display routines.",
    default = true
) {
    compatibleWith(COMPATIBILITY_ADM)

    execute {
        AppBrainBannerFingerprint.method.addInstructions(0, "return-void")
        AppodealInitFingerprint.method.addInstructions(0, "return-void")
        BannerDisplayFingerprint.method.addInstructions(0, "return-void")
        InterstitialDisplayFingerprint.method.addInstructions(0, "return-void")
    }
}
