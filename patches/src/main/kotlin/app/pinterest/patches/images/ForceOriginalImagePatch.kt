package app.pinterest.patches.images

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.images.RENDITION_CLASS
import app.pinterest.patches.images.RENDITIONS_CLASS
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference

/** Field `a`, the 736x rendition, which stock prefers. */
private const val FIELD_MEDIUM = "a"

/** Field `d`, the `originals` rendition, which stock only falls back to. */
private const val FIELD_ORIGINAL = "d"

private fun fieldOf(name: String) = ImmutableFieldReference(RENDITIONS_CLASS, name, RENDITION_CLASS)

@Suppress("unused")
val forceOriginalImagePatch = bytecodePatch(
    name = "Force original image download",
    description = "Load the original full-resolution asset for pin images instead of the 736x " +
        "rendition, at the cost of considerably more data.",
    default = false
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    execute {
        // The whole patch is a swap of two field reads. Stock:
        //
        //   iget-object v0, p0, Lvu2/d1;->a   // 736x
        //   if-nez      v0, :return_it
        //   iget-object v0, p0, Lvu2/d1;->d   // originals
        //   if-nez      v0, :return_it
        //   iget-object v0, p0, Lvu2/d1;->b   // 345x
        //   if-nez      v0, :return_it
        //   iget-object p0, p0, Lvu2/d1;->c   // 236x
        //   ...                             // else the EMPTY singleton
        //
        // Swapping the two reads makes it try originals, then 736x, then the rest. The branch
        // targets do not move and no instruction changes width — both are `iget-object`, format
        // 22c — so this cannot produce a width-mismatched method, which is the failure mode this
        // bundle has hit before.
        //
        // Null safety is structural rather than checked: `originals` is frequently absent for
        // videos and for pins the server only rendered small, so the swap is only safe because
        // the chain falls through to every other field and finally to the EMPTY `Lvu2/e1;`
        // singleton. Forcing originals can therefore never turn a rendered image into a crash or
        // a null url — it can only pick a different member of the same set.
        //
        // Both registers are carried over from the instructions being replaced rather than written
        // out. `iget-object` is format 22c, where A is the destination and B the object register;
        // here that is `v0` and `p0` in both positions. Copying them rather than hardcoding `v0`
        // keeps the patch correct if the method is recompiled with a different allocation.
        val method = ForceOriginalImageFingerprint.method
        val instructions = method.implementation!!.instructions

        fun indexOfField(name: String): Int = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.IGET_OBJECT &&
                (instruction as? ReferenceInstruction)?.reference == fieldOf(name)
        }

        val mediumIndex = indexOfField(FIELD_MEDIUM)
        val originalIndex = indexOfField(FIELD_ORIGINAL)
        check(mediumIndex != -1 && originalIndex != -1) {
            "The chooser no longer reads fields $FIELD_MEDIUM and $FIELD_ORIGINAL " +
                "($mediumIndex, $originalIndex); this patch only reorders those two reads."
        }
        check(mediumIndex < originalIndex) {
            "Field $FIELD_ORIGINAL is already tested before $FIELD_MEDIUM (indexes " +
                "$originalIndex and $mediumIndex); the preference has already changed and swapping " +
                "them again would invert the intent."
        }

        val medium = instructions[mediumIndex] as TwoRegisterInstruction
        val original = instructions[originalIndex] as TwoRegisterInstruction
        method.replaceInstruction(
            mediumIndex,
            BuilderInstruction22c(
                Opcode.IGET_OBJECT,
                medium.registerA,
                medium.registerB,
                fieldOf(FIELD_ORIGINAL)
            )
        )
        method.replaceInstruction(
            originalIndex,
            BuilderInstruction22c(
                Opcode.IGET_OBJECT,
                original.registerA,
                original.registerB,
                fieldOf(FIELD_MEDIUM)
            )
        )
    }
}