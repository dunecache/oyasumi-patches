package app.pinterest.patches.comments

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.Opcode

/**
 * Both constructors of the pin closeup action bar — the row of comment, save and share buttons
 * under a pin.
 *
 * `UnifiedPinActionBarView` is not obfuscated, and neither are the resource names it looks up:
 * R8 renames the generated `R` *class* but leaves the field names, so
 * `action_module_comments_wrapper` and `action_module_comments_icon` are stable anchors while the
 * R class holding them is not. The defining class of those field reads is deliberately left unset
 * in the filters for that reason.
 *
 * Each constructor is 84 instructions with nine registers, and does, in order:
 *
 *   23  sget              action_module_comments_wrapper
 *   24  findViewById
 *   28  iput-object       -> field `d : ViewGroup`      (the wrapper)
 *   29  sget              action_module_comments_icon
 *   34  iput-object       -> field `e : GestaltIcon`    (the icon)
 *
 * The wrapper is the module's container, so hiding it removes the button together with anything
 * else inside it. Note that the two field names are *not* unique on their own: each constructor
 * reads both. Uniqueness comes from the defining class, the names, and the parameter list.
 *
 * Both overloads have to be patched, and the reason is not obvious. They are **siblings, not a
 * delegating pair**: the three-parameter constructor does not call the two-parameter one, and
 * neither calls the other. Each independently runs the super constructor, builds every child view
 * and stores `d` and `e` itself. `LayoutInflater` inflates a custom view from XML through
 * `(Context, AttributeSet)`, so on a real pin that is the two-parameter constructor and the
 * three-parameter one is never reached.
 *
 * Patching only the three-parameter constructor applies cleanly, reports success, and does
 * nothing at runtime, because nothing ever calls it. That is exactly what happened in
 * `v0.6.0-dev.13`: the patch matched, the build succeeded, and the button stayed.
 *
 * The register layouts also differ between them, so the injected block is not shared verbatim.
 * The three-parameter constructor has four declared parameters, so `this` is `v5` and `v0` through
 * `v4` are free. The two-parameter constructor has three, and it passes six registers to its super
 * constructor with `invoke-direct/range`, so `this` is `v0` and `v1` through `v5` are free. In
 * both, the wrapper is in `v6` at the insertion point.
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

/** The overload `LayoutInflater` actually calls. See [CommentsModuleWrapperFingerprint]. */
object CommentsModuleWrapper2ArgFingerprint : Fingerprint(
    definingClass = "Lcom/pinterest/feature/pin/closeup/view/UnifiedPinActionBarView;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;"),
    filters = listOf(
        fieldAccess(name = "action_module_comments_wrapper", opcode = Opcode.SGET),
        fieldAccess(name = "action_module_comments_icon", opcode = Opcode.SGET)
    )
)