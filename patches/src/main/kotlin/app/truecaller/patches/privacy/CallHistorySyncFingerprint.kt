package app.truecaller.patches.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

/**
 * `Lcom/truecaller/callhistory/CallHistoryFullSyncWorker;->doWork`, the entry point of the call-history
 * backend sync.
 *
 * The full sync chain, read out of the dex rather than assumed:
 *
 * ```text
 * TruecallerApp;.onCreate          registers the worker under the name
 *                                  "com.truecaller.callhistory.CallHistoryFullSyncWorker"
 * Lcs0/bar;.invokeSuspend         1128-instruction coroutine that also enqueues it, by Class
 * CallHistoryFullSyncWorker;.doWork
 *   -> state machine on field E (values 0..4), literal "hasMatchingEntries"
 *   -> Lcv0/e;.invokeSuspend      the 48-instruction leaf
 *        -> ns/l;.M(ContentResolver, k81/a;.w(),
 *                    "conversation_id = ? AND date >= ?",
 *                    "sequence_number DESC, date DESC, _id DESC",
 *                    "COUNT(*)")                                     <- reads the call log
 *        -> Lcv0/c0;.f(I J J)      "expCallLogSyncPartial", dv0/bar;.a(...) cursor,
 *                                  then ContentResolver.applyBatch("com.truecaller", ...)
 *                                  with a "call_log" argument              <- writes it out
 * ```
 *
 * **Why this is the target.** It is the only place in the app that both reads the system call log and
 * ships the result somewhere. Suppressing it stops the exfiltration at the top, which is a stronger
 * guarantee than emptying a query and hoping every consumer is covered. The app schedules the worker
 * from two places, so this is genuinely the main path and not one of several.
 *
 * **Why the fingerprint is stable.** The class name is readable and R8 has not renamed it, which
 * matters because the worker is constructed reflectively by class name and looked up in a
 * `WorkerFactory` (`Ltx/r;.a`, 759 instructions) — a rename would break scheduling regardless of any
 * patch. Beyond that, the two `string` filters are literals from the method's own body, and they are
 * the same kind of filter that has already been proven to resolve on this build: the contact-list
 * patch's two sibling fingerprints use nothing but parameter-name literals and both matched on
 * device. No type filter is used, because an array-typed one failed twice in v0.6.0-dev.31 and
 * v0.6.0-dev.32 and there is no third spelling worth guessing at.
 */
object CallHistorySyncWorkerFingerprint : Fingerprint(
    definingClass = "Lcom/truecaller/callhistory/CallHistoryFullSyncWorker;",
    name = "doWork",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Lzf3/bar;"),
    filters = listOf(
        string("hasMatchingEntries"),
        string("workerClass")
    )
)