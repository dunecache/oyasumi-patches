package app.pinterest.patches.comments

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall

/**
 * The comments button on a pin's closeup action bar.
 *
 * The target is `LegacyPromotedCloseupActionButtonModule`, **not** `UnifiedPinActionBarView`.
 * Two earlier attempts patched the latter and shipped in `v0.6.0-dev.13`, `.14` and `.15`. All
 * three applied cleanly, matched their fingerprints, and did nothing, because a pin's closeup
 * screen inflates the legacy "promoted" action bar instead. The giveaway was a uiautomator dump
 * of a patched pin: its ids end in `_sab` (`action_module_react_icon_sab`,
 * `action_module_share_icon_sab`) and it reports `action_bar_root` as a plain `LinearLayout`,
 * where `UnifiedPinActionBarView` is a custom ViewGroup and would appear under its own class name.
 *
 * `createView` is the right method rather than a constructor, because this module is assembled by
 * the pin closeup view framework and its ids are resolved in one place here.
 *
 * No instruction filters: this class declares exactly one `createView()V` (verified in
 * `classes4.dex`), so defining class plus signature already resolves to one method. The
 * `action_module_comment_icon` / `action_buttons_center` `SGET`s at ins 102 / 58 are documented
 * here only as the patch's anchor context — the patch inserts after the `iput-object` to field
 * `l` at ins 106 — because adding them as `fieldAccess` filters failed to match on device
 * against the same bytes (`v0.6.0-dev.16` and `.17`), while every other Pinterest fingerprint
 * in this bundle resolved.
 *
 * Field `l : GestaltIconButton` receives the resolved comments button and is the handle the patch
 * acts on. Note the spelling of the id: singular "comment", no trailing `s`, no `_sab`. The
 * plural `action_module_comments_icon` that `UnifiedPinActionBarView` reads is a different id
 * in a different layout, which is what made the earlier fingerprints look plausible.
 */
object CommentsButtonFingerprint : Fingerprint(
    definingClass = "Lcom/pinterest/activity/pin/view/modules/LegacyPromotedCloseupActionButtonModule;",
    name = "createView",
    returnType = "V",
    parameters = listOf()
)

/**
 * The comments button actually on screen on an organic pin closeup.
 *
 * `Lsa1/i` **is** the `action_module_comments_icon` view: its `<init>` calls
 * `setId(action_module_comments_icon)` on `this` (ins 34-35, plural id, no `_sab`),
 * then builds the observed children — a `GestaltIcon` and a `GestaltText` count —
 * and `addView`s both. The uiautomator dump reports exactly that: a `LinearLayout`
 * with that id holding an `ImageView` and a `TextView '84'`. Its host is `Lbb1/u0`,
 * which `new`s it into field `g` and `addView`s it between the react and share cells.
 *
 * This is kept alongside [CommentsButtonFingerprint] (the promoted-closeup variant),
 * not instead of it, until device testing shows which closeups use which bar.
 *
 * The class declares exactly one method, `<init>(Context)V` (verified in
 * `classes*.dex` via the prebuilt index), so defining class plus signature already
 * resolves uniquely. The filters below are the stable, unobfuscated anchors that
 * keep it so: the `Lsf0/b` R field, `View.setId`, and `ViewGroup.addView`.
 */
object UabCommentsButtonFingerprint : Fingerprint(
    definingClass = "Lsa1/i;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        fieldAccess(
            definingClass = "Lsf0/b;",
            name = "action_module_comments_icon",
            type = "I"
        ),
        methodCall(
            definingClass = "Landroid/view/View;",
            name = "setId",
            parameters = listOf("I"),
            returnType = "V"
        ),
        methodCall(
            definingClass = "Landroid/view/ViewGroup;",
            name = "addView",
            parameters = listOf("Landroid/view/View;"),
            returnType = "V"
        )
    )
)
