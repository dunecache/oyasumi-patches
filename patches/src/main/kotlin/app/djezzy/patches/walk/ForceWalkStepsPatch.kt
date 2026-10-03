package app.djezzy.patches.walk

import app.djezzy.patches.shared.Constants.COMPATIBILITY_DJEZZY
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

/** 10000 decimal, as a signed `const/16` literal. `0x7fff` is 32767, so this fits. */
private const val FORCED_STEPS = "0x2710"

/**
 * The pref the running total lives under. Read straight out of `libapp.so` as a plain
 * string, beside `walk_and_win_last_pedometer_value`.
 */
private const val CURRENT_STEPS_PREF = "walk_and_win_current_steps"

/**
 * Both hooks log under this tag, so one `adb logcat -s djezzy-waw` shows which backend the
 * app actually uses:
 *
 *     prefs legacy map forced       the legacy hook is live
 *     prefs async getInt <key>      the async hook is live, and names every key read
 *
 * The logging is unconditional rather than gated on the key matching, because a gated log
 * produces an empty logcat in two completely different failure modes and that is exactly
 * what made the previous attempt undebuggable.
 */
private const val LOG_TAG = "djezzy-waw"

@Suppress("unused")
val forceWalkStepsPatch = bytecodePatch(
    name = "Force Walk & Win steps to 10000",
    description = "Make Djezzy's Walk & Win counter read 10,000 with no walk at all, by " +
        "forcing the stored step total itself rather than the pedometer event stream. The " +
        "card's figures are lifetime accumulators read back out of storage, so an event " +
        "delivered before a walk is never counted.",
    default = true
) {
    compatibleWith(COMPATIBILITY_DJEZZY)

    execute {
        // This used to also force the pedometer plugin's step stream. That was removed: it
        // never moved the counter, and gating it on the step-count channel introduced a
        // branch whose two paths rejoined with `v0` holding a `String` on one and an
        // `Integer` on the other. The verifier rejected `Li5/c;` outright --
        //
        //   VerifyError: Verifier rejected class i5.c: i5.c.onListen failed to verify:
        //   [0x2C] register v0 has type Conflict but expected Reference: i5.b
        //
        // -- which took down plugin registration and left a white screen. Forcing storage
        // is what the requirement needs, so the stream hooks are simply gone rather than
        // repaired.
        //
        // A fingerprint miss throws out of `execute`, so each hook is best-effort: an
        // optional refinement must not be able to stop the app from being patched at all.

        // Legacy backend. `.registers 8` with `ins 3` puts the parameters in `v5`-`v7` and
        // leaves `v0`-`v4` as locals, all dead by the return. `v1` is the map: the
        // `new-instance` the builder allocates is the receiver of the loop's own `put`, and
        // the same register is what the method returns. `v2` and `v3` are free for scratch.
        //
        // The insert is a plain tail insert immediately before the `return-object`, so it
        // runs on the way out of the method no matter which loop exit was taken. The method
        // has no branch of ours, so nothing needs a label and no register types meet.
        runCatching {
            LegacyPreferenceMapFingerprint.let { fingerprint ->
                val mapReturn = fingerprint.instructionMatches[3]

                fingerprint.method.addInstructions(
                    mapReturn.index,
                    "const-string v2, \"$LOG_TAG\"\n" +
                        "const-string v3, \"prefs legacy map forced\"\n" +
                        "invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n" +
                        "const-string v2, \"$CURRENT_STEPS_PREF\"\n" +
                        "const/16 v3, $FORCED_STEPS\n" +
                        "invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;\n" +
                        "move-result-object v3\n" +
                        // `invoke-interface` rather than `invoke-virtual`, so the register is
                        // accepted on its declared `Map` type instead of on whichever
                        // concrete map class the builder instantiated.
                        "invoke-interface {v1, v2, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
                )
            }
        }.onFailure {
            println("$LOG_TAG: legacy preference hook skipped, ${it.message}")
        }

        // Async backend. `.registers 5` with `ins 3` puts the parameters in `v2`-`v4` in
        // declaration order: `v2` is `this`, `v3` is the key and `v4` is the options
        // object. `v0` and `v1` are locals and both are free.
        //
        // The trace line is built before the key comparison so that it prints for every
        // call. If it never appears, the hook did not apply; if it appears without ever
        // naming this pref, the app does not read that key through this backend.
        //
        // The branch here is safe where the one in `onListen` was not: the taken path ends
        // in `return-object`, so the fall-through path never reaches the label and there is
        // no join at which register types would have to agree.
        runCatching {
            AsyncIntPreferenceFingerprint.let { fingerprint ->
                fingerprint.method.addInstructions(
                    0,
                    "const-string v0, \"$LOG_TAG\"\n" +
                        "const-string v1, \"prefs async getInt \"\n" +
                        "invoke-virtual {v1, v3}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;\n" +
                        "move-result-object v1\n" +
                        "invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n" +
                        "const-string v0, \"$CURRENT_STEPS_PREF\"\n" +
                        // `equals` has our own constant as the receiver, so a null key
                        // cannot throw before the branch is reached.
                        "invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z\n" +
                        "move-result v0\n" +
                        "if-eqz v0, :djezzy_waw_prefs_passthrough\n" +
                        // A wide literal: the Pigeon API boxes into `Long`, so 10000 is a
                        // `long` here and the constant occupies `v0` and `v1` together.
                        "const-wide/16 v0, $FORCED_STEPS\n" +
                        "invoke-static {v0, v1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;\n" +
                        "move-result-object v0\n" +
                        "return-object v0\n" +
                        ":djezzy_waw_prefs_passthrough"
                )
            }
        }.onFailure {
            println("$LOG_TAG: async preference hook skipped, ${it.message}")
        }
    }
}
