package app.pinterest.patches.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/oyasumi/extension/SettingsEntry;"
private const val RUNTIME_NAMES_CLASS = "Lapp/oyasumi/extension/MorpheRuntimeNames;"

@Suppress("unused")
val morpheSettingsEntryPatch = bytecodePatch(
    name = "Morphe settings entry",
    description = "Add the \"Morphe\" entry to the Account Settings list, opening the " +
        "Morphe settings screen.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch, morpheSettingsManifestPatch, morpheSettingsLabelPatch)

    extendWith("extensions/extension.mpe")

    execute {
        val method = SettingsMenuListBuilderFingerprint.method
        val instructions = method.implementation!!.instructions

        // Anchor: the construction of the first section header, `new <header>(int)`. This is the
        // "Settings"/"Account" header, always present. An earlier anchor on a conditional section
        // failed for accounts where that section is not built, so the header is required to be
        // unconditional. A bare "first <init>(int) in program order" is not enough either: other
        // rows share that signature, including spacers built inside conditionals. The header is
        // distinguished because it is the only row class used for MULTIPLE sections, so the
        // class constructed most often with `<init>(int)` is the header, and its first
        // occurrence is the anchor. On 14.38.0 that is `f1` (4 sections).
        val intConstructorCounts = instructions
            .mapNotNull { it.settingsRowConstructorWithParameters(listOf("I")) }
            .groupingBy { it }
            .eachCount()
        check(intConstructorCounts.isNotEmpty()) {
            "No settings row with an (int) constructor in the list builder."
        }

        val headerClass = intConstructorCounts.maxByOrNull { it.value }!!
        check(headerClass.value >= 2) {
            "Section-header candidate ${headerClass.key} built only once: likely a conditional " +
                "spacer, not the unconditional header. The Morphe entry would miss accounts."
        }

        val anchorIndex = instructions.indexOfFirst {
            it.settingsRowConstructorWithParameters(listOf("I")) == headerClass.key
        }
        check(anchorIndex != -1) { "Settings section header not found in the list builder." }

        // From the anchor, the first `List.add(Object)` adds the header itself, and its receiver
        // register is the list to append the Morphe row to as well.
        val addIndex = instructions.withIndex().drop(anchorIndex + 1).first { (_, instruction) ->
            (instruction.opcode == Opcode.INVOKE_VIRTUAL || instruction.opcode == Opcode.INVOKE_INTERFACE) &&
                instruction is ReferenceInstruction &&
                (instruction.reference as? MethodReference)?.let {
                    it.name == "add" && it.parameterTypes.size == 1
                } == true
        }.index

        val listRegister = (instructions[addIndex] as FiveRegisterInstruction).registerC
        val registerCount = method.implementation!!.registerCount

        // The external-link row class: the only constructor in the rows package taking a single
        // String (the destination URL). Clicking such a row makes the app fire
        // `Uri.parse(url)` into a generic `ACTION_VIEW` intent, which resolves to the
        // `morphe://` intent-filter and opens the settings activity. Reusing Pinterest's own
        // row means the Morphe entry looks and behaves like every other row. On 14.38.0 this
        // is `k1`, but the name is resolved here, not pinned.
        val rowClassType = instructions
            .firstNotNullOfOrNull { it.settingsRowConstructorWithParameters(listOf("Ljava/lang/String;")) }
        check(rowClassType != null) {
            "Settings external-link row not found: without it the Morphe entry cannot be built."
        }

        // JVM descriptor (Lcom/pinterest/...;) to Class.forName name (com.pinterest....).
        val rowClassName = rowClassType.removePrefix("L").removeSuffix(";").replace('/', '.')

        // Inject the row append AFTER the header's add. Order matters: this edit sits further
        // into the method, so it must go first, before the head injection below shifts every
        // later index. One invoke, one register (the list), so no scratch register is needed
        // in the middle of a large method where every low register may be live.
        method.addInstructions(
            addIndex + 1,
            "invoke-static/range { v$listRegister .. v$listRegister }, " +
                "$EXTENSION_CLASS->appendMorpheSettingsEntry(Ljava/lang/Object;)V"
        )

        // Tell the extension the resolved row class name, injected at index 0. The head is the
        // only point where a low register is certainly free, because no instruction precedes it
        // that could have put something live there. Mid-method there is no such guarantee
        // without a liveness analysis.
        method.addInstructions(
            0,
            "const-string v0, \"$rowClassName\"\n" +
                "invoke-static { v0 }, " +
                "$RUNTIME_NAMES_CLASS->setSettingsRowClass(Ljava/lang/String;)V"
        )
    }
}
