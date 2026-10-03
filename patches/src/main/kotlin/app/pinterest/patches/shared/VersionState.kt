package app.pinterest.patches.shared

import app.morphe.patcher.patch.bytecodePatch
import java.util.Locale
import kotlin.properties.Delegates

/**
 * The version of the APK currently being patched, recorded once before any other Pinterest
 * patch runs.
 *
 * Every other patch declares `dependsOn(versionCheckPatch)`, so by the time its own `execute`
 * block runs these values are already set. That ordering is the whole point of the patch: it
 * exists so a patch can branch on the app version without reading the APK again, and so that
 * version-dependent fingerprints live in one place instead of being re-derived per patch.
 *
 * Three fingerprints in this bundle are known to be version sensitive and currently have no way
 * to notice that they have stopped matching. The advertising identifier patch identifies its
 * target only by return type and parameter list, which a future Pinterest release could satisfy
 * with different code. The AppsFlyer patch hangs entirely off one obfuscated class name that the
 * SDK renames on every update. The Google Engage worker has no usable anchor at all yet.
 */
internal var patchedVersionName: String by Delegates.notNull()

/** True when the APK being patched is at least `version`, compared component by component. */
internal fun isAtLeast(version: String): Boolean =
    compareVersions(patchedVersionName, version) >= 0

/**
 * Compares two dotted version strings numerically, one component at a time.
 *
 * The official bundle does this with Kotlin's string comparison, `versionName >= version`, which
 * is wrong as soon as a component reaches two digits. Pinterest is already past that: `14.38.0`
 * sorts *below* `14.9.0` as text, because `'3'` is less than `'9'`. Any predicate written against
 * `14.9.0` or `14.10.0` would then be permanently false, and a predicate written against `14.38.0`
 * would be true on `14.100.0` for the wrong reason.
 *
 * A missing component counts as zero, so `14.38` equals `14.38.0`. A non-numeric component is
 * read as zero rather than throwing, so an unexpected version string degrades to a comparison
 * against the numeric prefix instead of failing the whole patch run.
 */
internal fun compareVersions(left: String, right: String): Int {
    val a = left.split('.').map { it.toIntOrNull() ?: 0 }
    val b = right.split('.').map { it.toIntOrNull() ?: 0 }

    for (i in 0 until maxOf(a.size, b.size)) {
        val diff = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
        if (diff != 0) return diff
    }
    return 0
}

/**
 * Internal, so it never appears in the patch list: it changes no code and offers nothing to
 * toggle. `PatchLoader` only loads patches that carry a name, which is what keeps this out of the
 * Manager's list while still letting other patches depend on it.
 */
internal val versionCheckPatch = bytecodePatch {
    execute {
        patchedVersionName = packageMetadata.versionName.lowercase(Locale.ROOT)

        // Guards the assumption every predicate rests on. If Pinterest ever ships a version
        // string with no readable component, the failure is loud here instead of silently making
        // every `isAtLeast` answer wrong.
        require(patchedVersionName.isNotBlank()) {
            "The APK declares an unreadable version name: \"${packageMetadata.versionName}\""
        }
    }
}

/** True when the APK being patched is `14.38.0` or newer. */
internal val is_14_38_0_or_greater: Boolean
    get() = isAtLeast("14.38.0")
