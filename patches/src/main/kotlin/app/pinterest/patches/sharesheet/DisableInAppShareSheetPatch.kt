package app.pinterest.patches.sharesheet

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val disableInAppShareSheetPatch = bytecodePatch(
    name = "Disable in-app share sheet",
    description = "Use the Android share sheet instead of Pinterest's own, so shares go through " +
        "the system chooser rather than a Pinterest-drawn menu.",
    default = false
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    execute {
        // The whole patch. `getShowInSharesheet()` is eight bytes of Dalvik and answers a
        // question about one enum constant, so there is nothing to narrow:
        //
        //   sget-object  v0, Lhn1/a;->CONTROL:Lhn1/a;
        //   if-eq       p0, v0, :return_zero
        //   const/4      p0, 0x1
        //   return       p0
        //  :return_zero
        //   const/4      p0, 0x0
        //   return       p0
        //
        // `p0` is both the receiver and the return slot — `.registers 2`, no declared
        // parameters — so `const/4 p0, 0x0` followed by `return p0` is a complete and
        // type-correct replacement, and it is the same value `CONTROL` produces, which is
        // the point: this pins the variant upstream already serves its holdout group rather
        // than inventing a third behaviour.
        //
        // The old instructions are removed and the new ones added at index 0 rather than
        // patched in place. Rewriting the body in place would mean matching the `sget-object`
        // and the branch target, and a nop-padded variant of those is exactly the shape that
        // has crashed this bundle before; removing the body outright leaves nothing to keep
        // in sync. `check` guards the count so a future build that grows the method fails
        // loudly instead of leaving a partial body behind.
        val method = InAppShareSheetGateFingerprint.method
        val instructions = method.implementation!!.instructions

        check(instructions.size == EXPECTED_INSTRUCTION_COUNT) {
            "getShowInSharesheet() has ${instructions.size} instructions, expected " +
                "$EXPECTED_INSTRUCTION_COUNT. The body is being replaced wholesale, so a " +
                "different count means the method gained logic this patch does not understand."
        }
        check(instructions.any { it.opcode == Opcode.SGET_OBJECT }) {
            "getShowInSharesheet() no longer reads a static field; it is not the enum " +
                "comparison this patch replaces."
        }

        method.removeInstructions(0, instructions.size)
        method.addInstructionsWithLabels(
            0,
            """
            const/4 p0, 0x0
            return p0
            """.trimIndent()
        )
    }
}

/**
 * `sget-object`, `if-eq`, `const/4`, `return`, `const/4`, `return`.
 */
private const val EXPECTED_INSTRUCTION_COUNT = 6