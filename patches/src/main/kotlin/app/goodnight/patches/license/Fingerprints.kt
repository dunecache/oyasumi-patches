package app.goodnight.patches.license

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

// PairIP tamper check, called from the app entry Application.attachBaseContext.
// Throws SignatureTamperedException when the APK signature differs from the
// Play release (always the case for a patched APK). Anchored on the two
// string constants of verifyIntegrity (see reference/NOTES.md).
object SignatureCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/SignatureCheck;",
    name = "verifyIntegrity",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        string("SHA-256"),
        string("Apk signature is invalid."),
    )
)

// PairIP Play-license check, called from Application.attachBaseContext and
// LicenseContentProvider.onCreate. Fails (paywall activity -> Play Store)
// when the installer is not com.android.vending. Anchored on the two log
// strings of checkLicense (see reference/NOTES.md).
object LicenseCheckFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "checkLicense",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        string("Cannot check license with null context."),
        string("Skipping license check in isolated process."),
    )
)

// PairIP VM startup, invoked from the ComponentFactory <clinit> hooks
// planted in classes2.dex (Landroid/app/AppComponentFactory and
// androidx/core/app/CoreComponentFactory), i.e. before Application starts.
// It runs the encrypted VM program (assets \x00IAP blobs) in the native
// libpairipcore, which enforces signature/installer checks outside dex
// reach. Skipping it disables the native verdict entirely — at the risk
// that the VM also runs required init (device test decides).
object StartupLauncherFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/StartupLauncher;",
    name = "launch",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/pairip/VMRunner;",
            name = "invoke",
        ),
    )
)
// LicenseActivity.showPaywallAndCloseApp: fires the Play Store paywall
// PendingIntent ("paywallintent" extra) then closes the app. Anchored on its
// two unique strings (see reference/NOTES.md). No-op'ing it (plus
// showErrorDialog below) makes the redirect screen inert no matter what
// opens it — dex code or the native PairIP core via JNI.
object ShowPaywallFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "showPaywallAndCloseApp",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("paywallintent"),
        string("Paywall intent is not provided."),
    )
)

// LicenseActivity.showErrorDialog: shows the "Something went wrong" dialog
// whose only button closes the app. Body has no string constants of its own
// (message lives in lambda$showErrorDialog$0), so anchored on its
// runOnUiThread call — referenced as LicenseActivity->runOnUiThread in the
// 1.345.0 dex, not Activity->runOnUiThread. Uniqueness comes from class +
// name + empty params.
object ShowErrorDialogFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "showErrorDialog",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
            name = "runOnUiThread",
        ),
    )
)
