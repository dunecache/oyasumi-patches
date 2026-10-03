package app.pinterest.patches.comments

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch

/** `View.GONE`. Needs `const/16`: `const/4` has a signed 4-bit literal and cannot encode 8. */
private const val GONE = "0x8"

/** Reads this patch's toggle from the Morphe settings. */
private const val EXTENSION_CLASS = "Lapp/oyasumi/extension/MorpheSettingsActivity;"

private const val SETTINGS_KEY = "morphe_hide_comments"

@Suppress("unused")
val hideCommentsPatch = bytecodePatch(
    name = "Hide comments",
    description = "Hide the comments button on a pin, so comments cannot be opened from the pin.",
    default = false
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    extendWith("extensions/extension.mpe")

    execute {
        // The wrapper is already in `v6` when it is stored into field `d`, and the next
        // instruction reuses `v6` for the icon lookup, so the insertion goes at the index of the
        // icon lookup: after the store, before `v6` is overwritten.
        //
        // No null guard is added. The constructor already dereferences the result of the
        // wrapper's `findViewById` two instructions earlier, at the `getClass()` call on the
        // value it returns, so a missing wrapper would have thrown before reaching here. Adding
        // a guard would widen a method that has branches in it for no behavioural gain.
        //
        // Nine registers with four declared parameters leaves `v0` through `v4` free; `v0` holds
        // the visibility constant and the call needs only the receiver and that constant.
        //
        // `const/16`, not `const/4`: `const/4` encodes a signed nibble, so `const/4 v0, 0x8`
        // assembles without complaint but decodes as `-8`, which stores `0xFFF8` in the visibility
        // bits. The view would then be neither VISIBLE, INVISIBLE nor GONE: it is not drawn, but
        // it keeps its layout slot, so the user sees a blank gap instead of a removed button.
        // Static checks cannot see this; it is the same trap the 1DM ads patch documents.
        val iconLookup = CommentsModuleWrapperFingerprint.instructionMatches[1]

        CommentsModuleWrapperFingerprint.method.addInstructionsWithLabels(
            iconLookup.index,
            """
            invoke-virtual {v6}, Landroid/view/View;->getContext()Landroid/content/Context;
            move-result-object v0
            const-string v1, "$SETTINGS_KEY"
            invoke-static {v0, v1}, $EXTENSION_CLASS->isEnabled(Landroid/content/Context;Ljava/lang/String;)Z
            move-result v0
            if-eqz v0, :morphe_end_hide_comments
            const/16 v0, $GONE
            invoke-virtual {v6, v0}, Landroid/view/View;->setVisibility(I)V
            :morphe_end_hide_comments
            nop
            """.trimIndent()
        )
    }
}