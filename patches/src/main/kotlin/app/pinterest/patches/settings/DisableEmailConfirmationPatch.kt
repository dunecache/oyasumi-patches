package app.pinterest.patches.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch

@Suppress("unused")
val disableEmailConfirmationDialogPatch = bytecodePatch(
    name = "Disable email confirmation dialog",
    description = "Hide the \"Confirm your email\" prompt and related screen, whether in home " +
        "or settings.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    execute {
        // `false` is the hide path, not an assumption: the gated method's own callers branch
        // past their prompt blocks on a false result. Five registers with one incoming, so `v0`
        // through `v3` are locals and `v0` is free for the constant.
        EmailVerificationGateFingerprint.method.addInstructions(
            0,
            "const/4 v0, 0\n" +
                "return v0"
        )
    }
}
