package app.pinterest.patches.settings

import app.morphe.patcher.patch.resourcePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch

/**
 * The `morphe://` URI the injected settings row opens. Clicking the row makes Pinterest fire
 * `Uri.parse(uri)` into a generic `ACTION_VIEW` intent, which resolves to the intent-filter
 * declared below and opens the settings activity. No Pinterest code needs to know the activity
 * exists; the URL is the whole integration.
 */
internal const val MORPHE_SETTINGS_URI = "morphe://settings"

private const val SETTINGS_ACTIVITY = "app.oyasumi.extension.MorpheSettingsActivity"

/**
 * A framework theme, deliberately not Pinterest's.
 *
 * Without this the activity inherits `<application>`'s `Theme.Pinterest.NoActionbar`, which
 * overrides framework widget styles (`android:buttonStyle` and friends) with references to
 * design-token attributes that only exist inside Pinterest's own theme overlays. Those overlays
 * are applied by Pinterest's activity base class, which this activity never goes through, so the
 * first framework widget construction fails while resolving the attribute and the settings
 * screen crashes on open. Since the settings UI uses no Pinterest resources at all, the correct
 * fix is to not inherit the app theme in the first place.
 */
private const val SETTINGS_THEME = "@android:style/Theme.Material.NoActionBar"

@Suppress("unused")
val morpheSettingsManifestPatch = resourcePatch(
    name = "Morphe settings screen (manifest)",
    description = "Register the Morphe settings activity in the manifest, with an intent-filter " +
        "for the morphe:// scheme.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    execute {
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.getElementsByTagName("application").item(0)

            val activity = manifest.createElement("activity").apply {
                setAttribute("android:name", SETTINGS_ACTIVITY)
                setAttribute("android:exported", "true")
                setAttribute("android:label", "Morphe")
                setAttribute("android:theme", SETTINGS_THEME)
            }

            val intentFilter = manifest.createElement("intent-filter")

            val action = manifest.createElement("action")
            action.setAttribute("android:name", "android.intent.action.VIEW")
            intentFilter.appendChild(action)

            val categoryDefault = manifest.createElement("category")
            categoryDefault.setAttribute("android:name", "android.intent.category.DEFAULT")
            intentFilter.appendChild(categoryDefault)

            val categoryBrowsable = manifest.createElement("category")
            categoryBrowsable.setAttribute("android:name", "android.intent.category.BROWSABLE")
            intentFilter.appendChild(categoryBrowsable)

            val data = manifest.createElement("data")
            data.setAttribute("android:scheme", "morphe")
            intentFilter.appendChild(data)

            activity.appendChild(intentFilter)
            application.appendChild(activity)
        }
    }
}
