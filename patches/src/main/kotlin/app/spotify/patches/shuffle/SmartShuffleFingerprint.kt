package app.spotify.patches.shuffle

import app.morphe.patcher.Fingerprint

object SmartShuffleLikedSongsDefaultFingerprint : Fingerprint(
    definingClass = "Lp/ez4;",
    name = "r",
    returnType = "Z",
    parameters = emptyList(),
    filters = emptyList()
)

object SmartShufflePlaylistDefaultFingerprint : Fingerprint(
    definingClass = "Lp/ez4;",
    name = "s",
    returnType = "Z",
    parameters = emptyList(),
    filters = emptyList()
)