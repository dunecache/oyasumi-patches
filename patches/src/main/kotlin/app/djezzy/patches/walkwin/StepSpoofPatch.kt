package app.djezzy.patches.walkwin

import app.goodnight.patches.shared.Constants.COMPATIBILITY_DJEZZY
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Fixed step count reported to walk-and-win on every sensor event instead of
// the real TYPE_STEP_COUNTER value. The first reading after tapping Start
// Walk lands as ~10000 (minus any stored baseline), then holds flat because
// the Dart side diffs readings (walk_and_win_last_pedometer_value).
private const val SPOOFED_STEP_COUNT = 10000

@Suppress("unused")
val stepSpoofPatch = bytecodePatch(
    name = "Walk-and-win step spoof",
    description = "Report a fixed step count (10000) to walk-and-win instead " +
        "of the real sensor value. Rewards are issued server-side and may " +
        "still require genuine activity.",
    default = true
) {
    compatibleWith(COMPATIBILITY_DJEZZY)

    execute {
        StepCountFingerprint.let {
            // float-to-int converts event.values[0]; overwrite the result
            // with the fixed count before Integer.valueOf boxes it for the
            // EventSink. Register is read from the match, not hardcoded.
            val conversion = it.instructionMatches[1]
            val register = conversion.getInstruction<OneRegisterInstruction>().registerA
            it.method.addInstructions(
                conversion.index + 1,
                "const v$register, $SPOOFED_STEP_COUNT"
            )
        }
    }
}
