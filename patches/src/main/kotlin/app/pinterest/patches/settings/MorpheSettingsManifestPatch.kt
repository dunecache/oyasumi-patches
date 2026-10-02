package app.pinterest.patches.settings

import app.morphe.patcher.patch.resourcePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch

/**
 * Declares the Morphe settings activity in the APK manifest.
 *
 * The activity itself does not exist yet. It will be supplied as a precompiled extension that is
 * merged into the app by the settings entry patch, and this declaration is what makes that
 * activity launchable. Writing the declaration first is deliberate: the manifest edit can be
 * validated against the decoded manifest on its own, while the activity's own code is a separate
 * piece of work that needs the same untestable extension tooling as the rest of `extensions/`.
 *
 * The component name pinned here is the one the future extension must use. `app.oyasumi.extension`
 * is the namespace the `extensions/extension` module already declares, and
 * `MorpheSettingsActivity` is the class the settings entry will start, so the declaration and the
 * eventual class agree on exactly this string.
 *
 * No theme is declared, so the activity inherits the application theme (`@7F150341` in the
 * reference). Naming a theme here would mean guessing at a style resource that may not exist in
 * this app, while the inherited theme is guaranteed to compile.
 *
 * Two things are unverified. The `document("AndroidManifest.xml")` form addresses the decoded
 * manifest through the same DOM helper the patcher documents for `res/values/strings.xml`, but no
 * example of it being used on the manifest was found in the official bundle, so the path is an
 * inference from the documented API rather than a confirmed pattern. And whether the rebuilt
 * manifest still satisfies the platform's parser after the edit cannot be checked here, because the
 * patcher build does not run in this environment.
 */
private const val SETTINGS_ACTIVITY = "app.oyasumi.extension.MorpheSettingsActivity"

@Suppress("unused")
val morpheSettingsManifestPatch = resourcePatch(
    name = "Morphe settings screen (manifest)",
    description = "Register the Morphe settings activity in the manifest, so the settings " +
        "screen is reachable on any supported version.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    execute {
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.getElementsByTagName("application").item(0)

            val activity = manifest.createElement("activity").apply {
                setAttribute("android:name", SETTINGS_ACTIVITY)
                setAttribute("android:exported", "false")
            }
            application.appendChild(activity)
        }
    }
}
