package app.pinterest.patches.sharesheet

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess

/**
 * The single app-wide gate for Pinterest's own share sheet.
 *
 * `getShowInSharesheet()` returns `this != CONTROL`, so the answer is decided by which
 * constant of a two-value enum the caller is holding, and only that. The variant is
 * assigned by `Lhn1/b;.a(Lfq0/v0;, Lfq0/w0;)Lhn1/a;`, which walks an
 * `Lfq0/a0;.h(String, String, Lfq0/w0;)` experiment list — `sg_android_sharesheet_holdout`
 * among them — and returns `FRONT` if any entry matches, `CONTROL` otherwise. So `CONTROL`
 * is not a failure state; it is the variant upstream ships for the holdout group.
 *
 * ## Why this is the right seam
 *
 * Four call sites, and all four ask the same question and all four do less when the answer
 * is no:
 *
 * | Caller | What it is |
 * | --- | --- |
 * | `Lfn1/f;.G3(ArrayList)V` | the pin overflow row builder (see the download patch) |
 * | `Lfn1/f;.N3(SharesheetModalAppListView)V` | the share-sheet refresh |
 * | `Lr11/a;.a(Lr11/m;)Ljava/util/List;` | builds the social-app list inside the sheet |
 * | `Lnj1/t0;` | a share-sheet helper |
 *
 * Forcing the method to answer no therefore suppresses one thing — the custom sheet — and
 * not four unrelated features. Nothing else in the app branches on it.
 *
 * ## Why the method name, not the class, is the anchor
 *
 * `Lhn1/a;` is obfuscated and will not survive an update. `getShowInSharesheet` did survive
 * it, which is the informative part: R8 does not keep a method name by accident, so this one
 * is either referenced across an obfuscation boundary or explicitly kept, and either way it
 * is the stable handle. The defining class is therefore left unset on the fingerprint, and
 * the two enum-constant reads are carried as filters instead.
 */
object InAppShareSheetGateFingerprint : Fingerprint(
    name = "getShowInSharesheet",
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            definingClass = "Lhn1/a;",
            name = "CONTROL",
            type = "Lhn1/a;"
        )
    )
)