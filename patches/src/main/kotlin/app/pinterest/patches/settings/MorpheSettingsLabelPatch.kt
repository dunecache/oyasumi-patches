package app.pinterest.patches.settings

import app.morphe.patcher.patch.resourcePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch

/**
 * The string the "Morphe" row in Pinterest's settings will display.
 *
 * This is a resource patch rather than a code patch so that, when the settings entry is finally
 * added, the row can point at `R.string.morphe_settings_entry` like any other Pinterest row
 * points at its own string. The value is added to the decoded `res/values/strings.xml` through
 * the DOM helper, which is a documented patcher API.
 *
 * Two deliberate limitations are recorded here rather than hidden.
 *
 * First, only the default `res/values/strings.xml` is touched. The description this patch is
 * transcribed from promises the label "across all languages", which in the official bundle is
 * done by walking `res` for every `values-<locale>/strings.xml` and rewriting each. Enumerating the
 * decoded resource directory from inside a patch has no documented API — `get(String, Boolean)`
 * addresses one file by name, and `listApkEntries(String)` lists the *input* APK, not the decoded
 * working tree — so the multi-locale expansion is not written here. It needs either a confirmed
 * enumeration API or a hardcoded locale list derived from the reference APK's own `res` tree.
 *
 * Second, appending to the shared `strings.xml` is the minimal change but not the tidiest one. A
 * dedicated `res/values/morphe_settings.xml` would isolate the bundle's strings, but writing a
 * file that does not already exist in the reference has the same unconfirmed-API problem as the
 * locale walk, so the shared file is used instead.
 *
 * Resource compilation after the edit is unverified: the patcher rebuilds `resources.arsc` from
 * the edited tree, and this environment cannot run that build.
 */
private const val LABEL_NAME = "morphe_settings_entry"

private const val LABEL_TEXT = "Morphe"

@Suppress("unused")
val morpheSettingsLabelPatch = resourcePatch(
    name = "Morphe settings screen (label)",
    description = "Provide the localized string for the \"Morphe\" entry in Pinterest's settings.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    execute {
        document("res/values/strings.xml").use { manifest ->
            val element = manifest.createElement("string").apply {
                setAttribute("name", LABEL_NAME)
                textContent = LABEL_TEXT
            }
            manifest.documentElement.appendChild(element)
        }
    }
}
