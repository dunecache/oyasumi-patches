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

private const val CREATE_TAB = "CREATE"

/** Reads this patch's toggle from the Morphe settings. */
private const val EXTENSION_CLASS = "Lapp/oyasumi/extension/MorpheSettingsActivity;"

private const val SETTINGS_KEY = "morphe_hide_create_nav"

@Suppress("unused")
val hideCreateNavButtonPatch = bytecodePatch(
    name = "Hide Create nav button",
    description = "Hide the create (+) button in the bottom navigation bar.",
    default = false
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    extendWith("extensions/extension.mpe")

    execute {
        // Same seam as the search and notifications patches, and for the same reason: hide the
        // tab's view, never skip creating the tab.
        //
        // The tab index arrives from a `forEachIndexed`-style loop (the caller increments its
        // counter with `add-int/lit8 v11, v7, 0x1` immediately before invoking `Q1`) and is used
        // positionally, both for `h.add(index, tab)` and as the child index for `addView`.
        // Returning early does not shrink that counter, so the next tab would call `h.add(2, ..)`
        // on a list of size 1 and throw on every app start. Hiding the view keeps every index
        // consistent, and a `GONE` child of the horizontal `LinearLayout` takes no space so the
        // rest reflow into it.
        //
        // CREATE is an ordinary member of that loop, not a special floating button. The bar's
        // descriptor table (`Lae0/k;.<clinit>`) builds five of them in order -- HOME, SEARCH,
        // CREATE, NOTIFICATIONS, PROFILE -- and each is handed to this same method. So the create
        // button's view is the same `View` in the same register as the other two tabs, and the
        // tab keeps its entry in the bar's list, so navigation by identity still resolves.
        //
        // `.registers 8` with five declared parameters puts `this` in `v2`, the descriptor in
        // `v3` and the tab `View` in `v6`, leaving `v0` and `v1` free. Insertion is at index 4,
        // immediately after `move-result-object` puts the view in `v6` and before anything
        // overwrites it. The preference read takes a Context and the key and nothing else: a
        // default-value argument would need a third register, and this toggle is opt-in, so the
        // shared default lives in the extension.
        BottomNavTabAdderFingerprint.method.addInstructionsWithLabels(
            4,
            """
            iget-object v0, v3, $DESCRIPTOR_TYPE->a:$TAB_ENUM
            sget-object v1, $TAB_ENUM->$CREATE_TAB:$TAB_ENUM
            if-ne v0, v1, :morphe_end_hide_create_nav
            invoke-virtual {v6}, Landroid/view/View;->getContext()Landroid/content/Context;
            move-result-object v0
            const-string v1, "$SETTINGS_KEY"
            invoke-static {v0, v1}, $EXTENSION_CLASS->isEnabled(Landroid/content/Context;Ljava/lang/String;)Z
            move-result v0
            if-eqz v0, :morphe_end_hide_create_nav
            const/16 v1, $GONE
            invoke-virtual {v6, v1}, Landroid/view/View;->setVisibility(I)V
            :morphe_end_hide_create_nav
            nop
            """.trimIndent()
        )
    }
}