package app.adm.patches.media

import app.adm.patches.shared.Constants.COMPATIBILITY_ADM
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val EXTENSION_CLASS = "Lapp/adm/extension/media/MediaGrabber;"

@Suppress("unused")
val mediaGrabberPatch = bytecodePatch(
    name = "Media grabber (direct video and subtitles)",
    description = "Capture direct video and subtitle URLs in ADM's browser and offer downloads.",
    default = false
) {
    compatibleWith(COMPATIBILITY_ADM)

    extendWith("extensions/adm-media.mpe")

    execute {
        WebPageStartedFingerprint.method.addInstructions(
            0,
            "invoke-static {p1, p2}, $EXTENSION_CLASS;->onPageStarted(Landroid/webkit/WebView;Ljava/lang/String;)V"
        )
        WebRequestFingerprint.method.addInstructions(
            0,
            "invoke-static {p1, p2}, $EXTENSION_CLASS;->onRequest(Landroid/webkit/WebView;Ljava/lang/String;)V"
        )
        WebCreateOptionsMenuFingerprint.let { fingerprint ->
            listOf(411, 293, 140, 60, 19).forEach { index ->
                fingerprint.method.addInstructions(
                    index,
                    "invoke-static {p0, p1}, $EXTENSION_CLASS;->onCreateOptionsMenu(Ljava/lang/Object;Landroid/view/Menu;)V"
                )
            }
        }
        WebOptionsItemSelectedFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0, p1}, $EXTENSION_CLASS;->onOptionsItemSelected(Ljava/lang/Object;Landroid/view/MenuItem;)Z
                move-result v0
                if-eqz v0, :morphe_media_continue
                const/4 v0, 0x1
                return v0
                :morphe_media_continue
            """.trimIndent()
        )
    }
}
