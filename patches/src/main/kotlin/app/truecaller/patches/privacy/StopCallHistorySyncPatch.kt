package app.truecaller.patches.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.truecaller.patches.shared.Constants.COMPATIBILITY_TRUECALLER

@Suppress("unused")
val stopCallHistorySyncPatch = bytecodePatch(
    name = "Stop call history sync",
    description = "Stop uploading your call history to Truecaller's servers, and stop it reading " +
        "the system call log to do so.",
    default = true
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // The sync is stopped at the worker's entry rather than at the query or the upload, so
        // nothing downstream has to be covered: no read happens, so there is nothing to upload.
        //
        // `doWork` returns `Ljava/lang/Object;` and extends `CoroutineWorker`, so the body's own
        // success path is reached through `Lsw0/r;.l(ILjava/lang/Object;)` and never returns a
        // `ListenableWorker.Result` directly. Returning `Unit` is what that path resolves to for a
        // `CoroutineWorker`, which WorkManager reads as `Result.success()`. So the work is
        // acknowledged as done rather than failed or retried -- returning `failure()` here would make
        // WorkManager re-enqueue the worker forever.
        //
        // The payload is inlined rather than shared through a `private const val` because
        // `tools/checks/check_inline_smali.py` collects blocks by matching a literal triple-quoted
        // string at the `addInstructionsWithLabels` call site, and a bare identifier would leave this
        // block outside the only check that catches malformed smali without a device.
        //
        // Registers: `doWork` declares `.registers 31` with two parameters (`this` and `Lzf3/bar;`),
        // so `v0` through `v28` are locals and `v0` is free. The payload touches `v0` alone.
        //
        // Deliberately not done here, because each is a separate patch with its own trade-offs:
        // emptying the in-app call log list, the `registerContentObserver` calls that watch the
        // system call log for changes, and the call-recording transcription store.
        //
        // Field references use the `->member:Type` spelling, not the `->member Type` spelling
        // baksmali prints. The inline smali compiler is an ANTLR grammar that requires the colon,
        // and `check_inline_smali.py` rejects the other form with `missing COLON` — which is exactly
        // what it did on this block before the spelling was corrected.
        CallHistorySyncWorkerFingerprint.method.addInstructionsWithLabels(
            0,
            """
            sget-object v0, Lkotlin/Unit;->a:Lkotlin/Unit;
            return-object v0
            """.trimIndent()
        )
    }
}