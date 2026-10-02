package app.pinterest.patches.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

/**
 * The app-owned wrapper around Google's advertising identifier.
 *
 * In the reference this is `Lvi2/b;->b(Landroid/content/Context;)Lcom/google/android/gms/ads/identifier/AdvertisingIdClient$Info;`,
 * but the class and the method name are obfuscated and will change between releases, so neither
 * is pinned here. What identifies the method instead is the shape of its contract: it is the one
 * method in the whole APK that returns an `AdvertisingIdClient$Info` from a single `Context`
 * argument, and the one that calls the identifier's own `getAdvertisingIdInfo` to do it.
 *
 * Four methods in the reference return `AdvertisingIdClient$Info`:
 *
 *   - `AdvertisingIdClient->d()`, which returns the platform implementation's result and takes
 *     no arguments.
 *   - `AdvertisingIdClient->getAdvertisingIdInfo(Context)`, which is the GMS static itself. It
 *     declares the return type but never calls itself, so the method-call filter excludes it.
 *   - `Lvi2/b;->a()`, which takes no arguments and only returns the cached field.
 *   - `Lvi2/b;->b(Context)`, which is the target.
 *
 * Pinning the return type, the parameter list and the `getAdvertisingIdInfo` call therefore
 * selects exactly one method. The remaining calls to `getAdvertisingIdInfo` in the APK belong to
 * third-party code and are all excluded by their own signatures: `com.appsflyer.internal.AFe1eSDK`
 * returns `boolean` from `(Context, AFe1rSDK)`, `ads_mobile_sdk.ez.w(...)` returns `Object`, and
 * the two `uk/` measurement methods return `boolean` and `Pair`. Those paths are not reached by
 * this patch and are covered by the AppsFlyer and third-party tracker patches instead.
 *
 * The method caches its result into a field before returning, and the no-argument getter has
 * eight callers that read only that cache. Patching the fetch rather than returning early is what
 * keeps those callers working: a return-early replacement would leave the cache permanently null
 * and every one of the eight callers would relaunch the asynchronous fetch on each call.
 */
object AdvertisingIdInfoFingerprint : Fingerprint(
    returnType = "Lcom/google/android/gms/ads/identifier/AdvertisingIdClient\$Info;",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/google/android/gms/ads/identifier/AdvertisingIdClient;",
            name = "getAdvertisingIdInfo",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Lcom/google/android/gms/ads/identifier/AdvertisingIdClient\$Info;"
        )
    )
)
