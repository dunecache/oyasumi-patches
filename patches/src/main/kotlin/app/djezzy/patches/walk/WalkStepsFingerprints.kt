package app.djezzy.patches.walk

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

/**
 * `Li5/b;->onSensorChanged(Landroid/hardware/SensorEvent;)V` is the only place in the
 * whole app where a step number is produced, so it is the only place worth overriding.
 *
 * The name and the descriptor are Android's, not R8's: `onSensorChanged` is declared by
 * `Landroid/hardware/SensorEventListener;` and the parameter is the framework's
 * `SensorEvent`, so neither can be renamed or retyped by obfuscation. That is what makes
 * this fingerprint survive a rebuild even though the class name will not.
 *
 * The filters pin the payload conversion, not the class. `Lf7/b;` is the other
 * `SensorEventListener` in this DEX and belongs to `sensors_plus`; it copies the floats
 * into a `double[]` and calls `success([D)`, so it never boxes an `Integer` and never
 * reads `SensorEvent.values`. Requiring the `values` read, the `float-to-int`
 * conversion and the `Integer.valueOf` box together leaves exactly this one method.
 *
 * `opcode(FLOAT_TO_INT, location = MatchAfterWithin(2))` sits after the `values` read
 * because the value travels through a `const/4` index and an `aget` before it is
 * converted; those two are the instructions allowed to intervene.
 */
object StepCountSensorFingerprint : Fingerprint(
    definingClass = "Li5/b;",
    name = "onSensorChanged",
    returnType = "V",
    parameters = listOf("Landroid/hardware/SensorEvent;"),
    filters = listOf(
        fieldAccess(
            definingClass = "Landroid/hardware/SensorEvent;",
            name = "values",
            type = "[F",
            opcode = Opcode.IGET_OBJECT
        ),
        opcode(Opcode.FLOAT_TO_INT, location = InstructionLocation.MatchAfterWithin(2)),
        methodCall(
            definingClass = "Ljava/lang/Integer;",
            name = "valueOf",
            parameters = listOf("I"),
            returnType = "Ljava/lang/Integer;"
        ),
        methodCall(
            definingClass = "Lio/flutter/plugin/common/EventChannel\$EventSink;",
            name = "success",
            parameters = listOf("Ljava/lang/Object;"),
            returnType = "V"
        )
    )
)

/**
 * `Li5/c;->onListen(Ljava/lang/Object;Lio/flutter/plugin/common/EventChannel$EventSink;)V`
 * is the `EventChannel$StreamHandler` for the pedometer plugin's two channels. Registering
 * the listener is not enough on its own: `Sensor.TYPE_STEP_COUNTER` (type 19, the
 * `step_count` channel) only reports when a step is actually detected, so a user who is
 * standing still receives no event at all and the Dart stream never carries a number.
 * This method is where the initial value is pushed, so the stream has a value as soon as
 * Dart subscribes.
 *
 * The first filter is the store into the plugin's listener field. That instruction is the
 * insert point for a reason that is easy to get wrong: the `EventSink` arrives in a
 * parameter register, and three instructions after the store the method loads the
 * `SensorManager` into that same register. Anything inserted after that point would pass a
 * `SensorManager` to `EventSink.success` and the verifier would reject the class on load.
 * The store into the listener field is the last instruction before the sink register is
 * reused, so `instructionMatches[0].index + 1` is the one index in this method where the
 * sink is provably still live.
 *
 * The second filter is `registerListener`, which is what makes this the pedometer plugin's
 * stream host rather than any other `EventChannel`. The two channels are two instances of
 * the same class, so the same method is matched for either, which is intended.
 */
object PedometerStreamHostFingerprint : Fingerprint(
    definingClass = "Li5/c;",
    name = "onListen",
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;", "Lio/flutter/plugin/common/EventChannel\$EventSink;"),
    filters = listOf(
        fieldAccess(
            definingClass = "Li5/c;",
            name = "i",
            type = "Li5/b;",
            opcode = Opcode.IPUT_OBJECT
        ),
        methodCall(
            definingClass = "Landroid/hardware/SensorManager;",
            name = "registerListener",
            parameters = listOf(
                "Landroid/hardware/SensorEventListener;",
                "Landroid/hardware/Sensor;",
                "I"
            ),
            returnType = "Z"
        )
    )
)

/**
 * The counter is rendered from the persisted total, not from the event stream. Every number
 * on the Walk & Win card is a lifetime accumulator, and a device run showed all of them
 * frozen while the stream was demonstrably delivering values — so the stream only writes to
 * storage during an active session and the displayed total comes back out of it.
 *
 * That makes this the layer that actually has to change for the counter to read 10,000
 * without a walk. `shared_preferences_android` has two independent backends and which one
 * Dart calls is not visible from the AOT snapshot, so both are hooked, and both log
 * unconditionally. The previous attempt logged only on a key match, which made "the hook
 * never applied" and "the hook applied but the app never reads that key through it" look
 * identical from the outside.
 *
 * No Fingerprint-level `definingClass` on either: that reduces matching to a single
 * `classMap` lookup that returns null with no fallback, which costs the indexed candidate
 * search without adding anything the name, return type, parameter list and filters do not
 * already give.
 */
object LegacyPreferenceMapFingerprint : Fingerprint(
    name = "getAllPrefs",
    returnType = "Ljava/util/Map;",
    parameters = listOf("Ljava/lang/String;", "Ljava/util/Set;"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/content/SharedPreferences;",
            name = "getAll",
            parameters = emptyList(),
            returnType = "Ljava/util/Map;"
        ),
        methodCall(
            definingClass = "Ljava/lang/String;",
            name = "startsWith",
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Z"
        ),
        methodCall(
            definingClass = "Lio/flutter/plugins/sharedpreferences/LegacySharedPreferencesPlugin;",
            name = "transformPref",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/Object;"),
            returnType = "Ljava/lang/Object;"
        ),
        opcode(Opcode.RETURN_OBJECT)
    )
)

/**
 * The async half. This plugin version stores through DataStore, so its `getInt` never
 * touches `android.content.SharedPreferences.getInt` and needs its own hook.
 *
 * `getInt` is a suspend wrapper whose whole body is two null checks, a coroutine start and
 * a return, so the key is available in a register before anything is read and the boxed
 * `Long` can simply be returned.
 *
 * The `const-string` is Kotlin's parameter-name null-check message for the options
 * argument. It is unique to this two-parameter overload and R8 has no reason to change it,
 * because the compiler emits it from the declared parameter name.
 */
object AsyncIntPreferenceFingerprint : Fingerprint(
    name = "getInt",
    returnType = "Ljava/lang/Long;",
    parameters = listOf(
        "Ljava/lang/String;",
        "Lio/flutter/plugins/sharedpreferences/SharedPreferencesPigeonOptions;"
    ),
    filters = listOf(
        string("options"),
        opcode(Opcode.RETURN_OBJECT)
    )
)
