package app.pinterest.patches.comments

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

/** `View.GONE`. Needs `const/16`: `const/4` has a signed 4-bit literal and cannot encode 8. */
private const val GONE = "0x8"

/** Reads this patch's toggle from the Morphe settings. */
private const val EXTENSION_CLASS = "Lapp/oyasumi/extension/MorpheSettingsActivity;"

private const val SETTINGS_KEY = "morphe_hide_comments"

/** The module holding the comments button, whose field `l` is the button itself. */
private const val MODULE_CLASS = "Lcom/pinterest/activity/pin/view/modules/LegacyPromotedCloseupActionButtonModule;"

/**
 * Field `l` is the comments button. Note `iconbutton`, not `iconcomponent`; the latter is the
 * other action bar's type and is the near-miss that misidentified the class in the first place.
 * No trailing semicolon, because the descriptor constant already ends in one and a second would
 * be a doubled `;`, which the inline parser rejects.
 */
private const val BUTTON_TYPE = "Lcom/pinterest/gestalt/iconbutton/GestaltIconButton"

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
        // Hides field `l`, the comments button, in
        // `LegacyPromotedCloseupActionButtonModule.createView`.
        //
        // Three earlier attempts patched `UnifiedPinActionBarView` instead and shipped in
        // v0.6.0-dev.13 through .15. Each applied cleanly and changed nothing, because a pin's
        // closeup screen uses the legacy "promoted" action bar. See the fingerprint for how the
        // uiautomator dump identified the right class.
        //
        // The button is read off `this` rather than reused from a register. `createView` has six
        // registers and one declared parameter, so `this` is `v5` and only `v0` through `v4` are
        // free. `v1` is the register the surrounding code uses for every `findViewById` result; the
        // block below runs after the last read of `v1`, and re-initialises `v1` itself.
        //
        // `const/16`, not `const/4`: `const/4` encodes a signed nibble, so `const/4 v1, 0x8`
        // assembles without complaint but decodes as `-8`, which stores `0xFFF8` in the visibility
        // bits. The button would then be neither VISIBLE, INVISIBLE nor GONE: not drawn, but still
        // holding its layout slot, so the user sees a blank gap instead of a removed button.
        //
        // The insertion index is the crux, and picking it wrong is what made the first three
        // attempts no-ops. Field `l` is written by this `iput-object`:
        //
        //   102  sget           v1, action_module_comment_icon
        //   103  findViewById
        //   104  move-result-object v1
        //   105  check-cast     v1, GestaltIconButton
        //   106  iput-object    v1 -> l          <- insert immediately after this
        //   107  invoke-virtual v5, ->l()V
        //
        // Anchoring on either `fieldAccess` match is wrong. `action_buttons_center` is resolved at
        // ins 58, long *before* `l` exists, and `action_module_comment_icon` at ins 102 is four
        // instructions before the store. Reading `l` at either point yields null, and a null guard
        // would turn that into a silent no-op rather than a crash. So the anchor is found by
        // scanning for the store itself, which is the only position where `l` is guaranteed live.
        val instructions = CommentsButtonFingerprint.method.implementation!!.instructions
        val storeIndex = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.IPUT_OBJECT &&
                (instruction as? ReferenceInstruction)?.reference?.let {
                    it is FieldReference && it.name == "l" && it.definingClass == MODULE_CLASS
                } == true
        }
        check(storeIndex != -1) {
            "Comments button field 'l' store not found in createView; the anchor this patch " +
                "depends on is gone, so inserting earlier would read a null field."
        }

        CommentsButtonFingerprint.method.addInstructionsWithLabels(
            storeIndex + 1,
            """
            iget-object v0, v5, $MODULE_CLASS->l:$BUTTON_TYPE;
            invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;
            move-result-object v1
            const-string v2, "$SETTINGS_KEY"
            invoke-static {v1, v2}, $EXTENSION_CLASS->isEnabled(Landroid/content/Context;Ljava/lang/String;)Z
            move-result v1
            if-eqz v1, :morphe_end_hide_comments
            const/16 v1, $GONE
            invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
            :morphe_end_hide_comments
            nop
            """.trimIndent()
        )
    }
}