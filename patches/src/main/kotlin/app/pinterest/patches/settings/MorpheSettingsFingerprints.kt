package app.pinterest.patches.settings

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Package of the settings-screen row models. The package itself is not obfuscated; only the
 * class names inside it are (`f1`, `k1`, … on 14.38.0). Every anchor in the entry patch is
 * derived from this package at patch time, so no obfuscated name is pinned anywhere.
 */
internal const val SETTINGS_ROW_PACKAGE = "Lcom/pinterest/feature/settings/menu/model/"

/**
 * The method that builds the `ArrayList` of Account Settings rows.
 *
 * It is a Kotlin synthetic that merges many lambdas into one `Function1`, so both its name and
 * its package change between releases and neither is pinned. What identifies it is its shape:
 * `Object invoke(Object)` that constructs several rows from the settings model package,
 * including at least one section header (`<init>(int)`) and at least one external-link row
 * (`<init>(String)`). No other method in the app has that profile.
 *
 * On 14.38.0 this resolves to `labs/s;.invoke`, which builds 17 rows with 4 section headers and
 * the single external-link row. The names are recorded here only as the verified instance, not
 * as something the fingerprint depends on.
 */
object SettingsMenuListBuilderFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    custom = { method, _ -> method.buildsSettingsMenu() }
)

/** Minimum settings rows constructed for a method to be the list builder. */
private const val MIN_SETTINGS_ROWS = 5

private fun Method.buildsSettingsMenu(): Boolean {
    val instructions = implementation?.instructions ?: return false

    var rowConstructions = 0
    var hasSectionHeader = false
    var hasExternalLink = false

    for (instruction in instructions) {
        if (instruction.opcode != Opcode.INVOKE_DIRECT) continue
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            ?: continue
        if (reference.name != "<init>") continue
        if (!reference.definingClass.startsWith(SETTINGS_ROW_PACKAGE)) continue

        rowConstructions++
        when (reference.parameterTypes.map { it.toString() }) {
            listOf("I") -> hasSectionHeader = true
            listOf("Ljava/lang/String;") -> hasExternalLink = true
        }
    }

    return rowConstructions >= MIN_SETTINGS_ROWS && hasSectionHeader && hasExternalLink
}

/**
 * The class descriptor being constructed, if this instruction is an `invoke-direct` on a
 * `<init>` in the settings row package with exactly these parameter types, else null.
 */
internal fun Instruction.settingsRowConstructorWithParameters(
    parameters: List<String>
): String? {
    if (opcode != Opcode.INVOKE_DIRECT) return null
    val reference = (this as? ReferenceInstruction)?.reference as? MethodReference ?: return null
    if (reference.name != "<init>") return null
    if (!reference.definingClass.startsWith(SETTINGS_ROW_PACKAGE)) return null
    if (reference.parameterTypes.map { it.toString() } != parameters) return null
    return reference.definingClass
}
