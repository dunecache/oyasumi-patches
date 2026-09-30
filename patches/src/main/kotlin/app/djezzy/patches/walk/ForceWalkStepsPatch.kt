package app.djezzy.patches.walk

import app.djezzy.patches.shared.Constants.COMPATIBILITY_DJEZZY
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
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

@Suppress("unused")
val forceWalkStepsPatch = bytecodePatch(
    name = "Force Walk & Win steps to 10000",
    description = "Report 10,000 steps to Djezzy's Walk & Win campaign, both on every " +
        "step-counter event and once when the step stream is first subscribed.",
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
            val register = conversion.getInstruction<OneRegisterInstruction>().getRegisterA()

            // `v$register` and not `$register`: smali's grammar takes register names, and
            // the rendered text only matches the compiled output when the `v` is present.
            fingerprint.method.replaceInstruction(
                conversion.index,
                "const/16 v$register, $FORCED_STEPS"
            )
        }

        // `Sensor.TYPE_STEP_COUNTER` only reports when a step is actually detected, so a
        // user who is standing still produces no events at all and the Dart stream would
        // never carry a number. `onListen` is where the plugin registers its listener, and
        // it is the point where the channel's sink is still live, so the same constant is
        // pushed here once, as soon as Dart subscribes.
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
        PedometerStreamHostFingerprint.let { fingerprint ->
            val listenerStore = fingerprint.instructionMatches[0]

            fingerprint.method.addInstructions(
                listenerStore.index + 1,
                "const-string v0, \"walk: pushing \"\n" +
                    "const/16 v1, $FORCED_STEPS\n" +
                    "invoke-static {v1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;\n" +
                    "move-result-object v1\n" +
                    // `concat` is a virtual call, so it takes its receiver as the first
                    // register; the two `valueOf` calls above are static and take one
                    // register for the argument and nothing else.
                    "invoke-virtual {v0, v1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;\n" +
                    "move-result-object v0\n" +
                    "const-string v1, \"$LOG_TAG\"\n" +
                    "invoke-static {v1, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n" +
                    // The forced value goes to the sink; the log line above is only a trace.
                    "const/16 v0, $FORCED_STEPS\n" +
                    "invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;\n" +
                    "move-result-object v0\n" +
                    "invoke-interface {v4, v0}, Lio/flutter/plugin/common/EventChannel$EventSink;->success(Ljava/lang/Object;)V"
            )
        }
    }
}
