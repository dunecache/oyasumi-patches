package app.adm.patches.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

object AppBrainBannerFingerprint : Fingerprint(
    definingClass = "Lcom/dv/get/f3;",
    name = "c",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("main-toolend"),
        string("18b2becc3142993292bf348e92467eded74e23229100a646")
    )
)

object AppodealInitFingerprint : Fingerprint(
    definingClass = "Lcom/dv/get/f3;",
    name = "i",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
    filters = listOf(
        string("18b2becc3142993292bf348e92467eded74e23229100a646")
    )
)

object BannerDisplayFingerprint : Fingerprint(
    definingClass = "Lcom/dv/get/f3;",
    name = "h",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("main-toolend")
    )
)

object InterstitialDisplayFingerprint : Fingerprint(
    definingClass = "Lcom/dv/get/f3;",
    name = "j",
    returnType = "V",
    parameters = listOf("Lcom/dv/get/all/MyActivity;"),
    filters = listOf(
        string("AppoInterShow")
    )
)
