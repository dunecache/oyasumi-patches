package app.pinterest.patches.navigation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch

/** The tab identity enum the bottom bar keys every tab off. Readable, never obfuscated. */
private const val TAB_ENUM = "Lde0/a;"

/** The descriptor's identity field: `Lae0/o;->a`. */
private const val DESCRIPTOR_TYPE = "Lae0/o;"

private const val SEARCH_TAB = "SEARCH"

@Suppress("unused")
val hideSearchNavButtonPatch = bytecodePatch(
    name = "Hide Search nav button",
    description = "Hide the search button in the bottom navigation bar.",
    default = false
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    execute {
        // Skip this tab entirely rather than hiding its view afterwards.
        //
        // The descriptor carries its own identity in `Lae0/o;->a`, which is an enum constant, so
        // the comparison is against a name Pinterest never renames. `p1` is the descriptor:
        // eight registers with six declared parameters puts it in `v3`.
        //
        // The bar's own tab list and the per-tab layout weights are both derived from the
        // descriptors that survive this method, so dropping one here lets the remaining tabs
        // take its place instead of leaving a gap.
        BottomNavTabAdderFingerprint.method.addInstructionsWithLabels(
            0,
            """
            iget-object v0, v3, $DESCRIPTOR_TYPE->a $TAB_ENUM;
            sget-object v1, $TAB_ENUM->$SEARCH_TAB $TAB_ENUM;
            if-ne v0, v1, :morphe_not_search_tab
            return-void
            :morphe_not_search_tab
            nop
            """.trimIndent()
        )
    }
}