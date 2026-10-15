package app.spotify.patches.ads

import app.spotify.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val disableSpotifyAdsPatch = bytecodePatch(
    name = "Disable ads",
    description = "Return Spotify's default empty ad response from GetAds, so no server-provided audio ad is used.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        GetAdsFingerprint.method.addInstructions(
            0,
            "const/4 v0, 0\nnew-array v0, v0, [B\ninvoke-static {v0}, Lcom/spotify/ads/esperanto/proto/GetAdsResponse;->r([B)Lcom/spotify/ads/esperanto/proto/GetAdsResponse;\nmove-result-object v0\nreturn-object v0"
        )
    }
}