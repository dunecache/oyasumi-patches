package app.djezzy.patches.walkwin

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

// Step-value listener of the obfuscated pedometer plugin (Li5/b in 3.0.9;
// R8 renames the class, so neither class nor field names are anchored).
// onSensorChanged is a framework override name: exactly two such methods
// exist in all dex (Li5/b pedometer + Lf7/b sensors_plus), and only this one
// converts the float sensor value to int and forwards it to a Flutter
// EventSink (the other emits a double[] with a timestamp). See
// reference/NOTES.md for the smali and the uniqueness counts.
object StepCountFingerprint : Fingerprint(
    name = "onSensorChanged",
    returnType = "V",
    parameters = listOf("Landroid/hardware/SensorEvent;"),
    filters = listOf(
        fieldAccess(smali = "Landroid/hardware/SensorEvent;->values:[F"),
        opcode(Opcode.FLOAT_TO_INT),
        methodCall(
            definingClass = "Ljava/lang/Integer;",
            name = "valueOf",
        ),
        methodCall(
            definingClass = "Lio/flutter/plugin/common/EventChannel\$EventSink;",
            name = "success",
        ),
    )
)
