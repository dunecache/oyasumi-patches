package app.pinterest.patches.download

import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.download.DOWNLOAD_ELIGIBILITY_CLASS
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

/**
 * The exact call whose result this patch replaces.
 *
 * Written out rather than assembled from parts so that the fingerprint filter and the
 * edit cannot drift apart: both compare against this one reference. `MethodReference`
 * is an interface in dexlib2, so the comparison needs the immutable implementation,
 * whose `equals` is the same value comparison either way.
 */
private val ELIGIBILITY_CALL = ImmutableMethodReference(
    DOWNLOAD_ELIGIBILITY_CLASS,
    "e",
    listOf("Lcom/pinterest/api/model/pe;"),
    "Z"
)

/**
 * The register the eligibility result lands in.
 *
 * `G3` has 23 registers and 2 declared parameters, so `this` is `v21` and the
 * `ArrayList` is `v22`; `v6` is a plain scratch int that the surrounding code reuses
 * for every predicate result in this method.
 */
private const val RESULT_REGISTER = "v6"

@Suppress("unused")
val downloadPinFromMenuPatch = bytecodePatch(
    name = "Download pin from long press",
    description = "Add a Download action to the pin long-press menu, so a pin's image can be " +
        "saved without opening the pin.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    execute {
        // Pinterest 14.38.0 already ships this feature and withholds it from image pins.
        // There is nothing to build here and nothing to inject: the row, the icon, the
        // title, the click dispatch, the HTTP call, the MediaStore write, the runtime
        // storage-permission request and the success/failure toasts are all upstream.
        // The only thing missing is eligibility.
        //
        // The gate, with real offsets from `classes6.dex` (ins 118-128):
        //
        //   118  iget-boolean   v6, v0, Lfn1/f;->o Z                 // a pre-set flag
        //   119  iget-object    v10, v0, Lfn1/f;->A Lkj1/c;          // the checker
        //   120  if-nez         v6, +00bh                             // flag set -> eligible
        //   121  invoke-virtual v10, v2, Lkj1/c;->e(Lpe;)Z           // <-- replaced
        //   122  move-result    v6
        //   123  if-eqz         v6, +003h                             // ineligible -> 125
        //   124  goto           +3h                                   // eligible   -> 127
        //   125  move           v6, v3        // v3 = 0, set at ins 4
        //   126  goto           +2h
        //   127  move           v6, v5        // v5 = 1, set at ins 14
        //   128  if-eqz         v6, +048h                             // 0 -> skip the row
        //
        // Ins 125/127 normalise the result to 0 or 1, so ins 128 is the real test. Replacing
        // ins 121-122 with `const/4 v6, 1` therefore produces exactly the state the
        // already-set-flag path at ins 120 produces: ins 123 sees a non-zero, takes the
        // `goto` at 124, ins 127 rewrites `v6` to `v5` (1), and ins 128 falls through
        // into the block that builds and inserts the row at ins 234.
        //
        // Both `v3` and `v5` were confirmed to be constants at the point of use — `v3 = 0`
        // at ins 4 and `v5 = 1` at ins 14, re-set at 324 and 491 — so the normalisation
        // cannot undo the forced value.
        //
        // Register safety: `v6` is written at 118, read at 120, and not touched again
        // before 122, so it is dead at the edit site and is the correct target.
        // `v10` must survive, because ins 132, 135 and 199 still call `Lkj1/c;.d()` and
        // `Lkj1/c;.f()` on it, and `v2` must survive because it is the pin those calls
        // and the row builder read. Replacing only ins 121 leaves both untouched. Dropping
        // the invoke also drops an unconditional virtual call on the `Lkj1/c;` field,
        // which upstream only reaches when the `o` flag is clear.
        //
        // Two instructions in, one out, so the method shrinks by six bytes. Nothing here
        // needs a nop pad: `const/4` is a complete statement and the following `if-eqz`
        // supplies the branch, which is why this is a `replaceInstruction` plus a
        // `removeInstruction` and not a `replaceInstructions` block.
        val method = PinOverflowMenuFingerprint.method
        val instructions = method.implementation!!.instructions

        val gateIndex = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                (instruction as? ReferenceInstruction)?.reference == ELIGIBILITY_CALL
        }
        check(gateIndex != -1) {
            "No invoke of ${ELIGIBILITY_CALL.definingClass}->${ELIGIBILITY_CALL.name} in the pin " +
                "overflow menu builder; this is the instruction the patch overrides, so without " +
                "it there is no eligibility gate left to force."
        }

        // The move-result is what makes the replacement one instruction wide. If the
        // signature ever changes so the result is not immediately consumed, removing the
        // next instruction would delete unrelated code.
        check(instructions[gateIndex + 1].opcode == Opcode.MOVE_RESULT) {
            "ins ${gateIndex + 1} after the eligibility call is " +
                "${instructions[gateIndex + 1].opcode}, not MOVE_RESULT; the single-instruction " +
                "replacement no longer lines up and the patch must be re-derived."
        }

        method.replaceInstruction(gateIndex, "const/4 $RESULT_REGISTER, 1")
        method.removeInstruction(gateIndex + 1)
    }
}