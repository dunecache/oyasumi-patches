package app.pinterest.patches.sharesheet

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

/**
 * The sharesheet-variant accessor.
 *
 * Left alone by this patch. It is the accessor that *decides* whether Pinterest uses its own
 * share sheet, and it has a fourth caller that gates the Download row in the pin overflow menu —
 * see the patch for why overriding it was wrong. Filtered on instead, so each target still proves
 * it calls the accessor.
 */
private val ACCESSOR_CALL = ImmutableMethodReference(
    SHARE_SHEET_ACCESSOR,
    SHARE_SHEET_ACCESSOR_NAME,
    listOf(),
    "Z"
)

@Suppress("unused")
val disableInAppShareSheetPatch = bytecodePatch(
    name = "Disable in-app share sheet",
    description = "Answer no to Pinterest's own share-sheet UI at its three presentation " +
        "sites, so sharing goes through the system sheet instead.",
    default = false
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    execute {
        // Rewritten after a device report. The first version forced
        // `Lhn1/a;.getShowInSharesheet()` — the accessor that *decides* whether Pinterest uses its
        // own sheet — to return false. That was wrong, because the accessor has a fourth caller
        // with nothing to do with presenting a sheet:
        //
        //   Lfn1/f;.G3(ArrayList)V, at ins 114 (classes6.dex, offsets resolved with androguard):
        //     invoke-virtual {v6}, Lhn1/a;->getShowInSharesheet()Z
        //     if-nez          v6, +004h      -> @0x01d0 = ins 118, the eligibility gate
        //     goto/16         +0dfh         -> @0x038a = ins 235, past the download block
        //
        // With the accessor forced false the branch took the goto and `Download pin from long press`
        // silently stopped working: the row was never added, because the jump landed after the
        // block that adds it. Two patches cancelling each other, recorded nowhere.
        //
        // This version leaves the accessor alone, so it keeps returning whatever the experiment
        // says and the Download row is untouched, and forces the answer at the three sites that
        // actually build the custom sheet:
        //
        //   Lfn1/f;.N3(SharesheetModalAppListView)V   the sheet refresh
        //   Lr11/a;.a(Lr11/m;)Ljava/util/List;         the social-app list inside the sheet
        //   Lnj1/t0;.a(...)NavigationImpl;              the share-navigation helper
        //
        // All three have the same two-instruction shape, so the edit is uniform.
        val targets = listOf(
            ShareSheetRefreshFingerprint,
            ShareSheetAppListFingerprint,
            ShareSheetNavigationFingerprint
        )

        targets.forEach { target ->
            val method = target.method
            val instructions = method.implementation!!.instructions

            val callIndex = instructions.indexOfFirst { instruction ->
                instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                    (instruction as? ReferenceInstruction)?.reference == ACCESSOR_CALL
            }
            check(callIndex != -1) {
                "${target.definingClass}->${target.name} no longer calls " +
                    SHARE_SHEET_ACCESSOR_NAME + "(); the site this patch edits has moved."
            }
            check(instructions[callIndex + 1].opcode == Opcode.MOVE_RESULT) {
                "ins ${callIndex + 1} after the accessor call in ${target.definingClass} is " +
                    "${instructions[callIndex + 1].opcode}, not MOVE_RESULT. The result is no " +
                    "longer consumed immediately, so replacing it would not answer the question " +
                    "this patch means to answer."
            }

            // `move-result` and `const/4` are both one code unit, so no width changes and no
            // branch offset moves. The invoke stays, its result simply discarded, which is legal.
            // The register comes off the instruction being replaced rather than being hardcoded.
            val result = instructions[callIndex + 1] as OneRegisterInstruction
            method.replaceInstruction(callIndex + 1, "const/4 v${result.registerA}, 0x0")
        }
    }
}
