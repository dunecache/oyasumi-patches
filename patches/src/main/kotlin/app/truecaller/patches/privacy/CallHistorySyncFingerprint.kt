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
 *
 * **Filter order is load-bearing.** Morphe does not evaluate filters independently: it walks the
 * instruction list once with a forward-only cursor, so filter k > 0 can only match at an index
 * after filter k-1's match, and a miss under the default `MatchAfterAnywhere` location fails the
 * whole fingerprint with no retry (read out of `morphe-patcher-1.13.0.jar`,
 * `Fingerprint.matchOrNull$matchFilters`). In `doWork` the literals appear as
 * `"workerClass"` at instruction 3 and `"hasMatchingEntries"` at 23, so they must be declared in
 * that order. Declared reversed, the fingerprint failed on device in the 2026-10-10 build;
 * see `reference/NOTES.md` under "Device failure 2026-10-10". The three contact-list fingerprints
 * matched on device because their two literals happen to be declared in instruction order already.
 *
 * `tools/checks/resolve_fp.py` treats filters as independent and cannot see this defect; it also
 * mis-parses a comment placed between `parameters` and `filters` inside the constructor call, which
 * is why this note lives here and not next to the list.
 */
object CallHistorySyncWorkerFingerprint : Fingerprint(
    definingClass = "Lcom/truecaller/callhistory/CallHistoryFullSyncWorker;",
    name = "doWork",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Lzf3/bar;"),
    filters = listOf(
        string("workerClass"),
        string("hasMatchingEntries")
    )
)