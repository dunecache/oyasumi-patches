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
            "sget-object v0, Lcom/spotify/ads/esperanto/proto/GetAdsResponse;->DEFAULT_INSTANCE:Lcom/spotify/ads/esperanto/proto/GetAdsResponse;\nreturn-object v0"
        )
    }
}