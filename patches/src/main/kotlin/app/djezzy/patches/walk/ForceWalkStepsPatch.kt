package app.djezzy.patches.walk

import app.djezzy.patches.shared.Constants.COMPATIBILITY_DJEZZY
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/** 10000 decimal, as a signed `const/16` literal. `0x7fff` is 32767, so this fits. */
private const val FORCED_STEPS = "0x2710"

/**
 * Both sites log under this tag, so a single `adb logcat -s djezzy-waw` shows what the
 * patch did. The logging is left in deliberately: without it a device run cannot tell
 * "the patch applied and the Dart layer clamped the number" apart from "the patch applied
 * and the number is wrong", and the Dart delta in this app is not readable from the
 * binary.
 */
private const val LOG_TAG = "djezzy-waw"

/**
 * The name `Li5/c;->l` carries for the `Sensor.TYPE_STEP_COUNTER` channel. The plugin
 * chooses it in its constructor, branching on whether the sensor type is 19, so this is
 * the app's own word for the channel and not something invented here.
 */
private const val STEP_COUNT_CHANNEL = "StepCount"

/**
 * The pref the running total lives under. Read straight out of `libapp.so` as a plain
 * string, next to `walk_and_win_last_pedometer_value`.
 */
private const val CURRENT_STEPS_PREF = "walk_and_win_current_steps"

@Suppress("unused")
val forceWalkStepsPatch = bytecodePatch(
    name = "Force Walk & Win steps to 10000",
    description = "Report 10,000 steps to Djezzy's Walk & Win campaign, both on every " +
        "step-counter event and once when the step stream is first subscribed. Each push " +
        "is a zero followed by 10,000, because one value cannot both open the counter's " +
        "accumulation window and jump through it. Only the step-count channel is touched, " +
        "and the stored total itself is forced to read back as 10,000 so the counter needs " +
        "no walk at all.",
    default = true
) {
    compatibleWith(COMPATIBILITY_DJEZZY)

    execute {
        // The pedometer plugin's `onSensorChanged` is the only place in the app where a
        // step number is produced, so overriding the conversion there covers every value
        // the Dart layer can ever see from the sensor.
        //
        // The `float-to-int` is replaced rather than the `aget` above it, so the read of
        // `SensorEvent.values[0]` still happens and its result is simply discarded. That
        // keeps the instruction count identical, which matters because the boxing and the
        // `success` call below are left exactly as the plugin emitted them and neither
        // accepts anything but the boxed int.
        //
        // `const/16` is two code units where `float-to-int` was one, so the replacement is
        // wider than what it replaces. That is safe here for the same reason it is safe in
        // the ADM limits patch: nothing in this method branches, so there are no branch
        // targets to shift.
        StepCountSensorFingerprint.let { fingerprint ->
            val conversion = fingerprint.instructionMatches[1]
            val successCall = fingerprint.instructionMatches[3]
            val register = conversion.getInstruction<OneRegisterInstruction>().getRegisterA()
            val sink = successCall.getInstruction<FiveRegisterInstruction>().getRegisterC()

            // `v$register` and not `$register`: smali's grammar takes register names, and
            // the rendered text only matches the compiled output when the `v` is present.
            fingerprint.method.replaceInstruction(
                conversion.index,
                "const/16 v$register, $FORCED_STEPS"
            )

            // A real step is the first moment the app is definitely counting, so the pair
            // is repeated here and not only at subscribe. See the `onListen` note for why
            // the pair is needed at all; the short version is that the app very likely
            // discards everything the stream delivers until the walk is started, which
            // makes a subscribe-time push arrive far too early to ever be counted.
            //
            // `.registers 3` with `ins 2` puts `this` in `v1` and the event in `v2`, and the
            // sink lands in `v0` on the `iget-object` immediately above. Both `v0` and `v2`
            // are still live for the original `success` call, so `v1` — dead since that
            // `iget-object` read it — is the only register available to build the leading
            // zero without disturbing anything the plugin emitted.
            fingerprint.method.addInstructions(
                successCall.index,
                "const/4 v1, 0x0\n" +
                    "invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;\n" +
                    "move-result-object v1\n" +
                    "invoke-interface {v$sink, v1}, Lio/flutter/plugin/common/EventChannel\$EventSink;->success(Ljava/lang/Object;)V"
            )
        }

        // `Sensor.TYPE_STEP_COUNTER` only reports when a step is actually detected, so a
        // user who is standing still produces no events at all and the Dart stream would
        // never carry a number. `onListen` is where the plugin registers its listener, and
        // it is the point where the channel's sink is still live, so the value is pushed
        // here as soon as Dart subscribes.
        //
        // It is pushed as a *pair*, and the leading zero is the whole point. Two
        // accumulations fit the evidence, and the campaign response cannot tell them apart:
        //
        //   delta:    current += raw - last_raw
        //   baseline: current  = raw - sessionStartRaw
        //
        // v0.5.0 pushed a single `10000` here and the counter read 0 forever. Under the
        // baseline model that lone value *is* the baseline, so the total is
        // `10000 - 10000 = 0`, and every later event carries the same constant and so
        // contributes a delta of zero. The number can never leave 0. Under the delta model
        // the same constant pays out once and then contributes nothing. Both are the same
        // bug: one value cannot both open the window and jump through it.
        //
        // A leading `0` then settles it without any state of our own:
        //
        //   baseline          baseline := 0, so 10000 - 0 = 10000
        //   delta, fresh      the 0 is a no-op or a +0, then +10000
        //   delta, stale      the 0 rewinds last_raw from any earlier value, then +10000
        //
        // The zero is also harmless if the app discards non-positive readings outright: the
        // stored baseline is already 0 on a fresh install, so the second event still lands
        // on 10000.
        //
        // The insert index is the one that is easy to get wrong in this method. The
        // `EventSink` arrives in a parameter register, and the instruction after the
        // listener store loads the `SensorManager` into that same register. Inserting
        // anywhere later — including immediately before the closing `return-void`, which is
        // where a tail insert naturally goes — would hand a `SensorManager` to
        // `EventSink.success` and the verifier would reject the class when it loads.
        // Inserting directly after the store into the listener field is the last point
        // where the sink register is provably untouched.
        //
        // `v0` and `v1` are the two locals. `v0` holds the listener that was just stored
        // and is reloaded from the field further down; `v1` is not read again on this path.
        //
        // The push is gated on the channel actually being the step counter. The pedometer
        // plugin builds two instances of this one class, and its constructor is what
        // decides which:
        //
        //     if (sensorType == Sensor.TYPE_STEP_COUNTER /* 19 */) l = "StepCount"
        //     else                                            l = "StepDetection"
        //
        // Dart subscribes to *both*, which is why v0.5.3 logged two pushes for one modal.
        // `step_detection` carries a boolean — `PedestrianStatus` on the Dart side — so
        // pushing a boxed `Integer` down it is a type error the moment anything listens,
        // and a stream that errors mid-flight can take the shared subscriber with it.
        // Nothing in the app's own code needs the detection channel, so the gate keeps
        // those bytes off it entirely.
        PedometerStreamHostFingerprint.let { fingerprint ->
            val listenerStore = fingerprint.instructionMatches[0]

            fingerprint.method.addInstructions(
                listenerStore.index + 1,
                "iget-object v0, v2, Li5/c;->l:Ljava/lang/String;\n" +
                    "const-string v1, \"$STEP_COUNT_CHANNEL\"\n" +
                    // `equals` has our own constant as the receiver so that a null name
                    // cannot throw before the branch below is reached.
                    "invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z\n" +
                    "move-result v0\n" +
                    "if-eqz v0, :djezzy_waw_not_step_count\n" +
                    "const-string v0, \"walk: pushing 0 then \"\n" +
                    "const/16 v1, $FORCED_STEPS\n" +
                    "invoke-static {v1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;\n" +
                    "move-result-object v1\n" +
                    // `concat` is a virtual call, so it takes its receiver as the first
                    // register; the `valueOf` call above is static and takes one register
                    // for the argument and nothing else.
                    "invoke-virtual {v0, v1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;\n" +
                    "move-result-object v0\n" +
                    "const-string v1, \"$LOG_TAG\"\n" +
                    "invoke-static {v1, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n" +
                    // First event: the baseline. Nothing has been read from this channel
                    // yet, so zero is always a valid opening value for the app to store.
                    "const/4 v0, 0x0\n" +
                    "invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;\n" +
                    "move-result-object v0\n" +
                    "invoke-interface {v4, v0}, Lio/flutter/plugin/common/EventChannel\$EventSink;->success(Ljava/lang/Object;)V\n" +
                    // Second event: the value. Against the baseline just established this
                    // is the total itself; against a delta accumulator it is the whole jump.
                    "const/16 v0, $FORCED_STEPS\n" +
                    "invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;\n" +
                    "move-result-object v0\n" +
                    "invoke-interface {v4, v0}, Lio/flutter/plugin/common/EventChannel\$EventSink;->success(Ljava/lang/Object;)V\n" +
                    // The detection channel falls through to the plugin's own code
                    // untouched. The label sits after the block and before the original
                    // instructions, so both paths continue correctly.
                    ":djezzy_waw_not_step_count"
            )
        }

        // The stream cannot do this on its own, and the device run is what proved it.
        //
        // Everything on the Walk & Win card is a lifetime accumulator — steps, distance,
        // calories and the `63h 34m` reading all sat frozen while the stream was visibly
        // delivering values, and none of them moved across Start Walk. A stream event
        // therefore only reaches storage while a session is running, and the number the
        // card renders is read back out of storage. Delivering 10,000 at rest means
        // forcing the stored total, because no event delivered before Start Walk is ever
        // counted.
        //
        // Both backends are hooked because which one Dart calls is not knowable from here,
        // and both log *unconditionally*. The previous attempt logged only when the key
        // matched, which left "the hook never applied" indistinguishable from "the hook
        // applied and the app never reads that key through it" — two very different bugs
        // that looked identical in the one signal available.
        //
        // Neither is allowed to fail the patch. A fingerprint miss throws out of `execute`,
        // and an unhandled one takes the pedometer hooks above down with it, which is
        // exactly how an optional refinement once stopped the app from patching at all.

        // Legacy backend. `LegacySharedPreferencesPlugin` has no per-type getter at all —
        // its only reads are `getAllPrefs`, `getAll` and `getKeys`, and all of them funnel
        // through `getAllPrefs`, which hands Dart the whole map and lets Dart pick the key.
        // So the map is amended on the way out rather than a getter being hooked.
        //
        // `.registers 8` with `ins 3` puts the parameters in `v5`-`v7` and leaves
        // `v0`-`v4` as locals, all dead by the return. `v1` is the map: the `new-instance`
        // the builder allocates is the receiver of the loop's own `put`, and the same
        // register is what the method returns. `v2` and `v3` are free for scratch.
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
                        // `invoke-interface` rather than `invoke-virtual` so the register is
                        // accepted on its declared `Map` type rather than on whichever
                        // concrete map the builder instantiated.
                        "invoke-interface {v1, v2, v3}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
                )
            }
        }.onFailure {
            println("$LOG_TAG: legacy preference hook skipped, ${it.message}")
        }

        // Async backend. `.registers 5` with `ins 3` puts the parameters in `v2`-`v4` in
        // declaration order: `v2` is `this`, `v3` is the key and `v4` is the options
        // object. `v0` and `v1` are locals, and both are free.
        //
        // The trace line is built before the key comparison so that it prints for every
        // call. If it never appears, the hook did not apply; if it appears without ever
        // naming this pref, the app does not read that key through this backend.
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
                        // `equals` has our own constant as the receiver so a null key
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
