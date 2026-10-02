package app.pinterest.patches.trackers

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch

@Suppress("unused")
val disableAppsFlyerPatch = bytecodePatch(
    name = "Disable AppsFlyer tracking",
    description = "Neutralize the AppsFlyer attribution SDK, so no state is returned to the " +
        "external data tracker.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    execute {
        // Six methods, all on the single concrete implementation, and all replaced with a value
        // that is correct for their return type. Nothing is left to reach into the SDK's own
        // state, so the SDK cannot be left half-initialised and cannot fault a caller that
        // reads it afterwards.
        //
        // Skipping `init` alone would be enough to stop transmission and would also crash. The
        // core the SDK builds during initialisation is fetched through `AFAdRevenueData()` in
        // `getAppsFlyerUID`, at index 8, and dereferenced by an `invoke-interface` at index 10
        // with no null check in between. Pinterest calls that method itself, so with `init`
        // suppressed the read would throw.
        //
        // `getInstance` is deliberately untouched: it is the singleton accessor, it is chained
        // on by every call site, and AppsFlyer's own internals reach for it too. Suppressing the
        // members below it is enough, because nothing that could transmit or fault survives.
        //
        // `setAdditionalData`, `setSharingFilterForPartners` and `setConsentData` are also left
        // alone. They only mutate local configuration that the suppressed methods would have
        // read, so keeping them costs nothing and keeps the diff smaller.
        //
        // `stop` and `unregisterConversionListener` are left alone for the same reason: they
        // only ever tear the SDK down.
        // Four registers with all four declared as parameters, so `this` is `v0` and the
        // replacement returns it unchanged. `v0` rather than `p0` on purpose: `p0` is spelled
        // `p0` in smali and the renderer does accept it, but nothing in this repository's
        // checks exercises a parameter register, and a plain register is what the existing
        // patches use.
        AppsFlyerInitFingerprint.method.addInstructions(0, "return-object v0")

        AppsFlyerStartFingerprint.method.addInstructions(0, "return-void")

        AppsFlyerLogEventFingerprint.method.addInstructions(0, "return-void")

        AppsFlyerLogSessionFingerprint.method.addInstructions(0, "return-void")

        AppsFlyerUninstallTokenFingerprint.method.addInstructions(0, "return-void")

        // The only non-void member of the path, so it has to produce a value instead of an
        // early return. Null is what the method already returns on its own error branch, and the
        // app-owned reader of an identifier tolerates it: `Lur/j;->b()V` tests the identifier
        // for null before it uses it, then tests it again for null, then for zero length.
        // Five registers with two incoming, so `v0` through `v2` are locals and free here.
        AppsFlyerUidFingerprint.method.addInstructions(
            0,
            "const/4 v0, 0\n" +
                "return-object v0"
        )
    }
}
