package app.spotify.patches.shuffle

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess

/**
 * `Lp/ez4;->r()Z` backs `smart_shuffle_as_default_for_liked_songs`. Its body
 * delegates to a parent via `h()` and otherwise reads the boolean field
 * `Lp/ez4;->q:Z`, verified in the pinned 9.1.90.2270 DEX. The field access is
 * the filter anchor; `filters` must never be an empty list because the patcher
 * treats a non-null empty list as no match.
 */
object SmartShuffleLikedSongsDefaultFingerprint : Fingerprint(
    definingClass = "Lp/ez4;",
    name = "r",
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(
            definingClass = "Lp/ez4;",
            name = "q",
            type = "Z"
        )
    )
)

/**
 * `Lp/ez4;->s()Z` backs `smart_shuffle_as_default_for_playlist`. Same shape as
 * `r()`; it reads the boolean field `Lp/ez4;->r:Z` instead, verified in the
 * pinned 9.1.90.2270 DEX.
 */
object SmartShufflePlaylistDefaultFingerprint : Fingerprint(
    definingClass = "Lp/ez4;",
    name = "s",
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(
            definingClass = "Lp/ez4;",
            name = "r",
            type = "Z"
        )
    )
)
