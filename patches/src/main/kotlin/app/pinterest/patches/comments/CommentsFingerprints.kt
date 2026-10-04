package app.pinterest.patches.comments

import app.morphe.patcher.Fingerprint

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
