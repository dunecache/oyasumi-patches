package app.pinterest.patches.settings

import app.morphe.patcher.patch.resourcePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch

/**
 * The string resource reused as the "Morphe" row label.
 *
 * This is the external-link row's own label ("Teen safety resources"), renamed to "Morphe" rather
 * than adding a new string. Renaming is safer than adding: a new `<string>` means a new resource
 * ID that was not in the original ARSC, which can fail the repackaging step, while rewriting the
 * text of an existing entry cannot. The row class the entry patch instantiates is exactly the
 * class that displays this string, so the label and the row stay consistent by construction.
 *
 * The real "Teen safety resources" row is rare — it appears only for certain underage accounts —
 * so in practice this rename is invisible anywhere except on the injected Morphe row. On an
 * account where the real row does appear, it will also read "Morphe"; that is accepted rather
 * than working around, because the alternative (a new resource) risks the build.
 */
private const val REUSED_STRING = "settings_menu_teen_safety_resources"

private const val LABEL_TEXT = "Morphe"

/**
 * Locales to rename the label in, derived from the reference APK's own `resources.arsc`
 * (48 locales including the default). Directories absent from a given build are skipped — the
 * `document` call throws for a missing file and the catch continues — so keeping this list wide
 * is harmless. What matters is that every locale the APK actually ships gets the rename;
 * otherwise a non-English device keeps showing the original text and the Morphe row is
 * indistinguishable from a stock Pinterest row.
 */
private val LOCALISED_VALUES_DIRS = listOf(
    "values",
    "values-af", "values-ar", "values-bg", "values-bn", "values-cs", "values-da", "values-de",
    "values-el", "values-en-rAU", "values-en-rGB", "values-en-rIN", "values-es", "values-es-rES",
    "values-fi", "values-fil", "values-fr", "values-hi", "values-hr", "values-hu", "values-in",
    "values-it", "values-iw", "values-ja", "values-kk", "values-ko", "values-ky", "values-ms",
    "values-nb", "values-nl", "values-pl", "values-pt", "values-pt-rBR", "values-pt-rPT",
    "values-ro", "values-ru", "values-sk", "values-sv", "values-te", "values-th", "values-tl",
    "values-tr", "values-uk", "values-uk-rUA", "values-vi", "values-zh", "values-zh-rCN",
    "values-zh-rTW",
)

@Suppress("unused")
val morpheSettingsLabelPatch = resourcePatch(
    name = "Morphe settings screen (label)",
    description = "Rename the reused string resource to \"Morphe\" in every shipped language, " +
        "so the settings entry is identifiable.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    execute {
        var renamed = 0

        for (dir in LOCALISED_VALUES_DIRS) {
            val path = "res/" + dir + "/strings.xml"
            try {
                document(path).use { doc ->
                    val strings = doc.getElementsByTagName("string")
                    for (i in 0 until strings.length) {
                        val node = strings.item(i)
                        if (node.attributes?.getNamedItem("name")?.nodeValue == REUSED_STRING) {
                            node.textContent = LABEL_TEXT
                            renamed++
                            break
                        }
                    }
                }
            } catch (_: Exception) {
                // Directory absent from this build. Normal; the locale falls back to English.
            }
        }

        check(renamed > 0) {
            "String $REUSED_STRING found in no res/values*/strings.xml: the Morphe row would " +
                "have no recognizable label."
        }
    }
}
