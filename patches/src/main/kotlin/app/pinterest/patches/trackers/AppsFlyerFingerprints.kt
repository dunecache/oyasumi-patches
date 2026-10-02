package app.pinterest.patches.trackers

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * `com.appsflyer.AppsFlyerLib` is the SDK's public API and is not obfuscated, but every one of
 * its methods is abstract there. In the reference the only concrete implementation is
 * `Lcom/appsflyer/internal/AFa1tSDK;`, and it is the only class in the APK that extends
 * `AppsFlyerLib`. That implementation class name *is* obfuscated, so these fingerprints pin it.
 * That is the one fragile element in this patch, and it is why the bundle wants a runtime
 * version check: when the SDK is updated the class is renamed and has to be re-derived.
 *
 * What keeps the rest of each fingerprint honest is the signature. Every target here is
 * `public final` in the reference, is declared with an exact return type and parameter list, and
 * the parameter lists are specific enough to separate the overloads that share a name. The
 * overloads that are deliberately left alone are listed in `DisableAppsFlyerPatch`.
 */
private const val IMPL = "Lcom/appsflyer/internal/AFa1tSDK;"

private val implOnly = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL)

/**
 * `init` is the SDK's gate. It is ten instructions long and does nothing but marshal its three
 * arguments into an `Object[]` and hand them to a private static, so nothing is lost by
 * returning `this` before the first of them.
 */
object AppsFlyerInitFingerprint : Fingerprint(
    definingClass = IMPL,
    name = "init",
    returnType = "Lcom/appsflyer/AppsFlyerLib;",
    parameters = listOf(
        "Ljava/lang/String;",
        "Lcom/appsflyer/AppsFlyerConversionListener;",
        "Landroid/content/Context;"
    ),
    accessFlags = implOnly
)

/**
 * The one-argument `start`, which is the overload Pinterest calls. The two- and three-argument
 * overloads share the name and are not matched.
 */
object AppsFlyerStartFingerprint : Fingerprint(
    definingClass = IMPL,
    name = "start",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    accessFlags = implOnly
)

/**
 * The three-argument `logEvent`. The four-argument overload, which takes an
 * `AppsFlyerRequestListener`, is not matched.
 */
object AppsFlyerLogEventFingerprint : Fingerprint(
    definingClass = IMPL,
    name = "logEvent",
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Ljava/lang/String;",
        "Ljava/util/Map;"
    ),
    accessFlags = implOnly
)

/**
 * The only member of the transmit path that returns a value rather than `void`, so it is the one
 * that needs a null returned instead of an early `return`.
 */
object AppsFlyerUidFingerprint : Fingerprint(
    definingClass = IMPL,
    name = "getAppsFlyerUID",
    returnType = "Ljava/lang/String;",
    parameters = listOf("Landroid/content/Context;"),
    accessFlags = implOnly
)

object AppsFlyerLogSessionFingerprint : Fingerprint(
    definingClass = IMPL,
    name = "setCustomerIdAndLogSession",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Landroid/content/Context;"),
    accessFlags = implOnly
)

object AppsFlyerUninstallTokenFingerprint : Fingerprint(
    definingClass = IMPL,
    name = "updateServerUninstallToken",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
    accessFlags = implOnly
)
