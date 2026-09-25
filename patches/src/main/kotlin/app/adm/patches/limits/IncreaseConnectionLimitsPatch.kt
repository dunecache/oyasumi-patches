package app.adm.patches.limits

import app.adm.patches.shared.Constants.COMPATIBILITY_ADM
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val increaseConnectionLimitsPatch = bytecodePatch(
    name = "Increase connection limits",
    description = "Raise the download slider ceiling to 64 and set torrent defaults to 500 global and 100 per torrent.",
    default = true
) {
    compatibleWith(COMPATIBILITY_ADM)

    execute {
        DownloadSliderMaxFingerprint.method.replaceInstruction(6, "const/16 v1, 64")
        TorrentConnectionDefaultsFingerprint.method.replaceInstruction(1222, "const-string v8, \"500\"")
        TorrentConnectionDefaultsFingerprint.method.replaceInstruction(1227, "const-string v8, \"100\"")
    }
}
