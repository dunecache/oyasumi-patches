package app.adm.patches.media

import app.adm.patches.shared.Constants.COMPATIBILITY_ADM
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

private const val EXTENSION_CLASS = "Lapp/adm/extension/media/MediaGrabber;"

private fun invokeStaticRange(
    startRegister: Int,
    name: String,
    parameters: List<String>,
    returnType: String
) = BuilderInstruction3rc(
    Opcode.INVOKE_STATIC_RANGE,
    startRegister,
    2,
    ImmutableMethodReference(EXTENSION_CLASS, name, parameters, returnType)
)

@Suppress("unused")
val mediaGrabberPatch = bytecodePatch(
    name = "Media grabber (direct video and subtitles)",
    description = "Capture direct video and subtitle URLs in ADM's browser and offer downloads.",
    default = false
) {
    compatibleWith(COMPATIBILITY_ADM)

    extendWith("extensions/adm-media.mpe")

    execute {
        WebPageStartedFingerprint.method.addInstruction(
            0,
            invokeStaticRange(
                4,
                "onPageStarted",
                listOf("Landroid/webkit/WebView;", "Ljava/lang/String;"),
                "V"
            )
        )
        WebRequestFingerprint.method.addInstruction(
            0,
            invokeStaticRange(
                4,
                "onRequest",
                listOf("Landroid/webkit/WebView;", "Ljava/lang/String;"),
                "V"
            )
        )
        WebCreateOptionsMenuFingerprint.let { fingerprint ->
            listOf(411, 293, 140, 60, 19).forEach { index ->
                fingerprint.method.addInstruction(
                    index,
                    invokeStaticRange(
                        16,
                        "onCreateOptionsMenu",
                        listOf("Ljava/lang/Object;", "Landroid/view/Menu;"),
                        "V"
                    )
                )
            }
        }
        WebOptionsItemSelectedFingerprint.method.let { method ->
            method.addInstruction(
                0,
                invokeStaticRange(
                    16,
                    "onOptionsItemSelected",
                    listOf("Ljava/lang/Object;", "Landroid/view/MenuItem;"),
                    "Z"
                )
            )
            val continueLabel = method.implementation!!.newLabelForIndex(1)
            method.addInstruction(1, BuilderInstruction11x(Opcode.MOVE_RESULT, 0))
            method.addInstruction(2, BuilderInstruction21t(Opcode.IF_EQZ, 0, continueLabel))
            method.addInstruction(3, BuilderInstruction11n(Opcode.CONST_4, 0, 1))
            method.addInstruction(4, BuilderInstruction11x(Opcode.RETURN, 0))
        }
    }
}
