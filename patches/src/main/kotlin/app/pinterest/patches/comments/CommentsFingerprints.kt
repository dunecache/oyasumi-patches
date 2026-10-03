package app.pinterest.patches.comments

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.Opcode

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
 * The stable anchors are the resource ids. R8 renames the generated `R` *class* but leaves the
 * field names, so the ids below are readable while the class holding them (`Lvf0/c;`) is not, and
 * the defining class of the field reads is deliberately left unset for that reason.
 *
 *   - `action_module_comment_icon` is the comments button. Note the spelling: singular
 *     "comment", no trailing `s`, and no `_sab`. The plural `action_module_comments_icon` that
 *     `UnifiedPinActionBarView` reads is a different id in a different layout, which is what made
 *     the earlier fingerprints look plausible.
 *   - `action_buttons_center` is the row the buttons live in. Keeping it in the filters pins the
 *     method to the action bar rather than any other view in the module.
 *
 * Field `l : GestaltIconButton` receives the resolved comments button and is the handle the patch
 * acts on.
 *
 * Note that `createView` also reads `action_module_reaction_count`, `promote_button`, `menu_react`,
 * `menu_send` and `overflow_button`, so the module covers more than the comments button. Those are
 * left alone.
 */
object CommentsButtonFingerprint : Fingerprint(
    definingClass = "Lcom/pinterest/activity/pin/view/modules/LegacyPromotedCloseupActionButtonModule;",
    name = "createView",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(name = "action_module_comment_icon", opcode = Opcode.SGET),
        fieldAccess(name = "action_buttons_center", opcode = Opcode.SGET)
    )
)