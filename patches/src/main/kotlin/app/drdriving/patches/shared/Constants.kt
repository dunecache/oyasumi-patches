package app.drdriving.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    /**
     * The reference is `base.apk` lifted out of the VInstall APKV bundle
     * `com.ansangha.drdriving_1.73.apkv`, so the target is declared as `APK`: the file the
     * fingerprints were read out of is a self-contained APK. `ApkFileType.APK` is
     * non-required, so the APKV bundle the user supplies is also accepted.
     *
     * SHA-256 of the analysed `base.apk`:
     * `76d35d2af8ca77460c71da9025385ce1ffbf14636804c8659966c55d8799efd6`
     *
     * SHA-256 of the APKV: `c2d920df51859fec04042b572216f931bb98342088180b92e075f4d46018c1cf`
     *
     * Only this one version was extracted and checked. Do not add another target without
     * re-deriving every fingerprint against that version's own dex.
     */
    val COMPATIBILITY_DRDRIVING = Compatibility(
        name = "Dr. Driving",
        packageName = "com.ansangha.drdriving",
        apkFileType = ApkFileType.APK,
        targets = listOf(
            AppTarget(version = "1.73", versionCode = 73)
        )
    )
}