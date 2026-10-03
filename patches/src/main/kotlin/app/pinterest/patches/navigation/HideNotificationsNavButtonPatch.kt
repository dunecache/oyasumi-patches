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

private const val NOTIFICATIONS_TAB = "NOTIFICATIONS"

@Suppress("unused")
val hideNotificationsNavButtonPatch = bytecodePatch(
    name = "Hide Notifications nav button",
    description = "Hide the notifications button in the bottom navigation bar.",
    default = false
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    execute {
        // Same seam, same shape and same reasoning as the search-button patch, keyed on a
        // different enum constant. Both patches insert into this one method, so they compose:
        // each block is self-contained, uses only `v0` and `v1`, and ends by falling through to
        // the `nop` at its own label.
        //
        // They are separate patches rather than one because they are independent toggles:
        // hiding one button must not require enabling the other.
        //
        // See the search patch for why the tab is hidden rather than skipped: the tab index
        // comes from a loop counter, so skipping a tab desynchronises it from the bar's list.
        BottomNavTabAdderFingerprint.method.addInstructionsWithLabels(
            4,
            """
            iget-object v0, v3, $DESCRIPTOR_TYPE->a $TAB_ENUM;
            sget-object v1, $TAB_ENUM->$NOTIFICATIONS_TAB $TAB_ENUM;
            if-ne v0, v1, :morphe_not_notifications_tab
            const/16 v1, $GONE
            invoke-virtual {v6, v1}, Landroid/view/View;->setVisibility(I)V
            :morphe_not_notifications_tab
            nop
            """.trimIndent()
        )
    }
}