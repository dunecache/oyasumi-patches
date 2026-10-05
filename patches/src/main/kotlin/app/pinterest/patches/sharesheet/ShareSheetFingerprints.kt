package app.pinterest.patches.sharesheet

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

/** The sharesheet-variant accessor this patch used to override, and still filters on. */
internal const val SHARE_SHEET_ACCESSOR = "Lhn1/a;"

internal const val SHARE_SHEET_ACCESSOR_NAME = "getShowInSharesheet"

/**
 * The share-sheet refresh: `Lfn1/f;.N3(SharesheetModalAppListView)`.
 *
 * The parameter type is unobfuscated, which is the best anchor available here.
 */
object ShareSheetRefreshFingerprint : Fingerprint(
    definingClass = "Lfn1/f;",
    name = "N3",
    returnType = "V",
    parameters = listOf("Lcom/pinterest/feature/sharesheet/view/SharesheetModalAppListView;"),
    filters = listOf(
        methodCall(
            definingClass = SHARE_SHEET_ACCESSOR,
            name = SHARE_SHEET_ACCESSOR_NAME,
            parameters = listOf(),
            returnType = "Z"
        )
    )
)

/**
 * The social-app list inside the custom sheet: `Lr11/a;.a(Lr11/m;)Ljava/util/List;`.
 */
object ShareSheetAppListFingerprint : Fingerprint(
    definingClass = "Lr11/a;",
    name = "a",
    returnType = "Ljava/util/List;",
    parameters = listOf("Lr11/m;"),
    filters = listOf(
        methodCall(
            definingClass = SHARE_SHEET_ACCESSOR,
            name = SHARE_SHEET_ACCESSOR_NAME,
            parameters = listOf(),
            returnType = "Z"
        )
    )
)

/**
 * The share-navigation helper: `Lnj1/t0;.a(...)Lcom/pinterest/navigation/NavigationImpl;`.
 *
 * Nine declared parameters, two of them unobfuscated model types
 * (`MultiPinSharingPayload`, `GreetingIntentData`), so the descriptor is itself a strong filter.
 */
object ShareSheetNavigationFingerprint : Fingerprint(
    definingClass = "Lnj1/t0;",
    name = "a",
    returnType = "Lcom/pinterest/navigation/NavigationImpl;",
    parameters = listOf(
        "Lnj1/t0;",
        "Lmu1/s;",
        "I",
        "Lzo2/c;",
        "Lin1/x1;",
        "Lnj1/c;",
        "Landroid/net/Uri;",
        "Lcom/pinterest/sendshare/model/SendableObject;",
        "Z",
        "Lcom/pinterest/feature/sharesheet/model/MultiPinSharingPayload;",
        "Z",
        "Lcom/pinterest/feature/sharesheet/model/GreetingIntentData;",
        "I"
    ),
    filters = listOf(
        methodCall(
            definingClass = SHARE_SHEET_ACCESSOR,
            name = SHARE_SHEET_ACCESSOR_NAME,
            parameters = listOf(),
            returnType = "Z"
        )
    )
)