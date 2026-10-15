package app.spotify.patches.shuffle

import app.spotify.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val disableSmartShuffleDefaultPatch = bytecodePatch(
    name = "Disable Smart Shuffle default",
    description = "Make Smart Shuffle not the default for playlists and liked songs, so playback follows the normal queue order.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        SmartShuffleLikedSongsDefaultFingerprint.method.addInstructions(
            0,
            "const/4 v0, 0\nreturn v0"
        )
        SmartShufflePlaylistDefaultFingerprint.method.addInstructions(
            0,
            "const/4 v0, 0\nreturn v0"
        )
    }
}