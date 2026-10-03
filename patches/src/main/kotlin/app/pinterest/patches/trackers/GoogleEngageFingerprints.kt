package app.pinterest.patches.trackers

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

/**
 * The receiver Pinterest registers for `com.google.android.engage.action.PUBLISH_RECOMMENDATION`
 * and for its own login and logout broadcasts.
 *
 * `onReceive` is 49 instructions and does exactly one thing: build a WorkManager one-time request
 * and enqueue it under the name `google_engage_one_time_publish_job`, with
 * `BackoffPolicy.EXPONENTIAL` and a 30 000 ms interval, for
 * `Lcom/pinterest/engage/GoogleEngageWorker;`. It reads neither of its two parameters. The
 * `getClass()` calls on the incoming registers at indices 0 and 1 are obfuscation filler and the
 * intent's action is never inspected, so every broadcast that reaches this receiver enqueues the
 * same publish job.
 *
 * Neither the class nor the method name is obfuscated here, and the job name occurs exactly once
 * in the whole APK, in this method and nowhere else. That literal is the anchor rather than the
 * WorkManager types around it, which are all obfuscated (`Lbd/d0;` for the request builder,
 * `Lbd/e;` for the request, `Lbd/e0;` for its constraints).
 */
object GoogleEngageReceiverFingerprint : Fingerprint(
    definingClass = "Lcom/pinterest/engage/GoogleEngageBroadcastReceiver;",
    name = "onReceive",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
    filters = listOf(
        string("google_engage_one_time_publish_job")
    )
)
