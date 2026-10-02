package app.pinterest.patches.ads

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

private const val EXTENSION_CLASS = "Lapp/oyasumi/extension/NeutralizeAdvertisingIdPatch;"

private const val INFO_TYPE = "Lcom/google/android/gms/ads/identifier/AdvertisingIdClient\$Info;"

@Suppress("unused")
val neutralizeAdvertisingIdPatch = bytecodePatch(
    name = "Neutralize advertising ID",
    description = "Return a random advertising ID instead of the real one, so the app has " +
        "no advertising identifier to hand to Pinterest or to any bundled tracker.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    extendWith("extensions/extension.mpe")

    execute {
        // Only the fetch is redirected. The `move-result-object` that follows, the store into
        // the cache field, the branch to the cached return, and the exception handler all stay
        // exactly as the app emitted them, so the synthetic value is cached on the first call
        // and the eight readers of that cache keep seeing a stable value for the session.
        //
        // The replacement is a single `invoke-static` over a single register, which is the same
        // shape and the same width as the `invoke-static` it replaces. That matters because the
        // method branches: a wider replacement would shift every branch target in it. The
        // register handed over is the one the app already loaded the `Context` into for the
        // original call, and the return type is unchanged, so the `move-result-object` and the
        // `iput-object` that read it are unaffected.
        AdvertisingIdInfoFingerprint.let { fingerprint ->
            val fetch = fingerprint.instructionMatches[0]

            // The register is read from the matched invoke rather than hardcoded. A 35c
            // invoke takes its arguments in the C slots and has no receiver, so this is
            // `getRegisterC(0)` and not `getRegisterA`, which would be the opcode nibble.
            // Reading it means the patch still works if Pinterest reorders the locals.
            val contextRegister = fetch.getInstruction<Instruction35c>().getRegisterC(0)

            fingerprint.method.replaceInstruction(
                fetch.index,
                "invoke-static {v$contextRegister}, " +
                    "$EXTENSION_CLASS->getInfo(Landroid/content/Context;)$INFO_TYPE"
            )
        }
    }
}
