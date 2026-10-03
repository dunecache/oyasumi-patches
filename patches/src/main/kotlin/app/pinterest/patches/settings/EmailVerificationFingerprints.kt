package app.pinterest.patches.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

/**
 * The gate for Pinterest's "confirm your email" prompt.
 *
 * In the reference this is `Lfq0/r0;->b()Z`, but both the class and the method name are
 * obfuscated, so neither is pinned. What identifies it is the contract: it is the one
 * parameterless method returning `boolean` whose body reads the
 * `android_settings_email_verification` experiment key.
 *
 * That literal occurs in exactly three code methods in the APK (plus one `<clinit>` that only
 * interns it). The other two return `Object`, so the return type alone separates them:
 *
 *   - `Lak1/i;->invoke()Object`, ins 2146
 *   - `Lqh1/h;->invoke()Object`, ins 272
 *   - `Lfq0/r0;->b()Z`, ins 14 — the target
 *
 * The `"enabled"` literal it compares the key against is added as a second filter. Both
 * literals sit in the target's body, so a future method would have to share the whole pair to
 * collide.
 *
 * Returning `false` is the hide path, verified at the call sites: `Lvj1/u0;.F1(Z)V` branches
 * past its email-verification UI block when this returns false and falls through to the normal
 * `super.F1`, and the same gate feeds `Lvj1/v;.onCreateView`, `Lvj1/v;.z9`, `Lvj1/u0;.dismiss`
 * and `Lak1/k;.z9`. One edit covers all five.
 */
object EmailVerificationGateFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        string("android_settings_email_verification"),
        string("enabled")
    )
)
