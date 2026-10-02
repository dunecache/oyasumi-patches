package app.pinterest.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    /**
     * The reference is the APKMirror standalone all-ABI build, a regular APK rather than a
     * split bundle, so the target is declared as `APK`. It still carries native libraries for
     * all four ABIs, so one entry covers the whole archive.
     */
    val COMPATIBILITY_PINTEREST = Compatibility(
        name = "Pinterest",
        packageName = "com.pinterest",
        apkFileType = ApkFileType.APK,
        targets = listOf(
            AppTarget(version = "14.38.0", versionCode = 14388010)
        )
    )
}
