package app.truecaller.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    /**
     * The reference is `base.apk` lifted out of the APKMirror APKM bundle
     * `com.truecaller_26.31.6-2631006_2arch_7dpi_1feat_…apkmirror.com.apkm`, so the target is
     * declared as `APK`: the file the fingerprints were read out of is a self-contained APK
     * carrying `classes.dex` through `classes9.dex`, `resources.arsc` and the whole of `res/`.
     *
     * The bundle's ABI and density splits hold no DEX and were not analysed. One split does carry
     * DEX — `split_insights_category_model.apk` — and it was deliberately left out of this
     * declaration, because it is an optional dynamically-loaded feature and no fingerprint here
     * depends on it. A bundle patched without that feature is still matched correctly.
     *
     * SHA-256 of the analysed `base.apk`:
     * `f6454d90b56469a00ac7b2608f43ab2410d7d89c94df05bd55c599283583a89d`
     *
     * Only this one version was extracted and checked. Do not add another target without
     * re-deriving every fingerprint against that version's own dex.
     */
    val COMPATIBILITY_TRUECALLER = Compatibility(
        name = "Truecaller",
        packageName = "com.truecaller",
        apkFileType = ApkFileType.APK,
        targets = listOf(
            AppTarget(version = "26.31.6", versionCode = 2631006)
        )
    )
}