package app.pinterest.patches.comments

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.Opcode

/**
 * The constructor of the pin closeup action bar — the row of comment, save and share buttons
 * under a pin.
 *
 * `UnifiedPinActionBarView` is not obfuscated, and neither are the resource names it looks up:
 * R8 renames the generated `R` *class* but leaves the field names, so
 * `action_module_comments_wrapper` and `action_module_comments_icon` are stable anchors while the
 * R class holding them is not. The defining class of those field reads is deliberately left unset
 * in the filters for that reason.
 *
 * In the reference the constructor is 84 instructions with nine registers and four declared
 * parameters, and does, in order:
 *
 *   23  sget              action_module_comments_wrapper
 *   24  findViewById
 *   28  iput-object       -> field `d : ViewGroup`      (the wrapper)
 *   29  sget              action_module_comments_icon
 *   34  iput-object       -> field `e : GestaltIcon`    (the icon)
 *
 * The wrapper is the module's container, so hiding it removes the button together with anything
 * else inside it. Note that the two field names are *not* unique on their own: the two-argument
 * constructor is a near-clone of this one and reads both too. Uniqueness comes from the defining
 * class, the name and the three-parameter signature; the field reads are there to pin which
 * constructor and which module, not to identify it on their own.
 */
object CommentsModuleWrapperFingerprint : Fingerprint(
    definingClass = "Lcom/pinterest/feature/pin/closeup/view/UnifiedPinActionBarView;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;", "I"),
    filters = listOf(
        fieldAccess(name = "action_module_comments_wrapper", opcode = Opcode.SGET),
        fieldAccess(name = "action_module_comments_icon", opcode = Opcode.SGET)
    )
)