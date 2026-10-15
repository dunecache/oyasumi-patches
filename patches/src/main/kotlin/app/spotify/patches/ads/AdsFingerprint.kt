package app.spotify.patches.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

/**
 * `Lp/x22;->a(Lcom/spotify/ads/esperanto/proto/GetAdsRequest; Lp/xel;)Ljava/lang/Object;` is
 * Spotify's coroutine wrapper for `spotify.ads.esperanto.proto.Ads.GetAds`. It calls the
 * server, then parses the response into `GetAdsResponse`. These anchors occur once in the
 * reference 9.1.90.2270 DEX.
 */
object GetAdsFingerprint : Fingerprint(
    definingClass = "Lp/x22;",
    name = "a",
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        "Lcom/spotify/ads/esperanto/proto/GetAdsRequest;",
        "Lp/xel;"
    ),
    filters = listOf(
        string("spotify.ads.esperanto.proto.Ads"),
        string("GetAds"),
        methodCall(
            definingClass = "Lcom/spotify/ads/esperanto/proto/GetAdsResponse;",
            name = "r",
            parameters = listOf("[B"),
            returnType = "Lcom/spotify/ads/esperanto/proto/GetAdsResponse;"
        )
    )
)