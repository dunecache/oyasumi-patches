package app.pinterest.patches.trackers

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch

@Suppress("unused")
val disableGoogleEngagePatch = bytecodePatch(
    name = "Disable Google Engage",
    description = "Stop Pinterest publishing user actions to Google, so nothing is sent to " +
        "Snooper, Analytics, Play or Ads.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    execute {
        // Suppressing the receiver is complete on its own, because the receiver is the only
        // thing that ever enqueues this job. `GoogleEngageWorker` is constructed in exactly two
        // places, and neither of them schedules it: `Lpr/n9;` is the WorkManager `WorkerFactory`
        // that WorkManager calls to rebuild the worker, and `Lpo0/f;` holds the injected
        // instance. The single `const-class` for `GoogleEngageWorker` in the APK is at index 23
        // of this method.
        //
        // `onReceive` returns `void`, so an early `return-void` needs no value register. The two
        // parameters are unused anyway: the intent's action is never read, so dropping the
        // broadcast has no side effect on the receiver's own state.
        GoogleEngageReceiverFingerprint.method.addInstructions(0, "return-void")
    }
}
