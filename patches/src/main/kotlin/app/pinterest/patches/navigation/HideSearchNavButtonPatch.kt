package app.pinterest.patches.navigation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch

/** The tab identity enum the bottom bar keys every tab off. Readable, never obfuscated. */
private const val TAB_ENUM = "Lde0/a;"

/** The descriptor's identity field: `Lae0/o;->a`. */
private const val DESCRIPTOR_TYPE = "Lae0/o;"

/** `View.GONE`. Needs `const/16`: `const/4` has a signed 4-bit literal and cannot encode 8. */
private const val GONE = "0x8"

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
        // Hide the tab's view, do NOT skip creating the tab.
        //
        // Returning early from this method is the obvious approach and it is wrong. The tab's
        // index is passed in from a `forEachIndexed` loop over the descriptor list, and it is
        // used positionally twice: to insert the tab into the bar's `h : ArrayList`, and as the
        // child index handed to `addView`. Skipping one tab does not shrink that counter, so the
        // next tab would call `h.add(2, ...)` on a list of size 1 and throw
        // `IndexOutOfBoundsException` on every app start.
        //
        // Letting the tab be created and then hiding its view keeps every index, the list, and
        // the layout consistent, and a `GONE` child in a horizontal `LinearLayout` takes no space,
        // so the remaining tabs still reflow into it. Per-tab layout weights come from `E1`,
        // which assigns `1.0f` to every tab and hardcodes no tab count.
        //
        // Instruction 3 leaves the tab's `View` in `v6`, and nothing between there and the
        // insertion point overwrites it. Eight registers with six declared parameters puts the
        // descriptor in `v3` and `this` in `v2`, leaving only `v0` and `v1` free, so `v1` is
        // reused for the visibility constant once the comparison has consumed it.
        //
        // The tab stays in the bar's lookup list, so navigation by identity still resolves and no
        // other code path loses a tab it expected to find.
        BottomNavTabAdderFingerprint.method.addInstructionsWithLabels(
            4,
            """
            iget-object v0, v3, $DESCRIPTOR_TYPE->a $TAB_ENUM;
            sget-object v1, $TAB_ENUM->$SEARCH_TAB $TAB_ENUM;
            if-ne v0, v1, :morphe_not_search_tab
            const/16 v1, $GONE
            invoke-virtual {v6, v1}, Landroid/view/View;->setVisibility(I)V
            :morphe_not_search_tab
            nop
            """.trimIndent()
        )
    }
}