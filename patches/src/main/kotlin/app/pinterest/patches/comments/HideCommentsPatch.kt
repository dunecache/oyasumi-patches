package app.pinterest.patches.comments

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.InlineSmaliCompiler
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

/** `View.GONE`. Needs `const/16`: `const/4` has a signed 4-bit literal and cannot encode 8. */
private const val GONE = "0x8"

/** Reads this patch's toggle from the Morphe settings. */
private const val EXTENSION_CLASS = "Lapp/oyasumi/extension/MorpheSettingsActivity;"

private const val SETTINGS_KEY = "morphe_hide_comments"

/** The module holding the comments button, whose field `l` is the button itself. */
private const val MODULE_CLASS = "Lcom/pinterest/activity/pin/view/modules/LegacyPromotedCloseupActionButtonModule;"

/**
 * Field `l` is the comments button. Note `iconbutton`, not `iconcomponent`; the latter is the
 * other action bar's type and is the near-miss that misidentified the class in the first place.
 * No trailing semicolon, because the descriptor constant already ends in one and a second would
 * be a doubled `;`, which the inline parser rejects.
 */
private const val BUTTON_TYPE = "Lcom/pinterest/gestalt/iconbutton/GestaltIconButton"

/** The unified action bar comments cell: a plain `LinearLayout` holding icon plus count. */
private const val UAB_CLASS = "Lsa1/i;"

/** `UAB_CLASS`'s direct superclass, which is where `setVisibility` has to be re-dispatched to. */
private const val UAB_SUPER_CLASS = "Lqc1/b;"

private const val SET_VISIBILITY = "setVisibility"

/**
 * Registers in the injected [SET_VISIBILITY]: `v0` the `Context`, `v1` the key, and the two
 * parameters the parser places at the top, `p0` = `v2` (`this`) and `p1` = `v3` (the visibility
 * being set). Four is therefore the minimum: two scratch registers plus the two parameters.
 */
private const val SET_VISIBILITY_REGISTERS = 4

/**
 * The override, which is what actually keeps the cell hidden.
 *
 * `UAB_CLASS` is `VISIBLE` by default and nothing in its own constructor sets it otherwise, so the
 * constructor block below is what hides it initially. It is not enough on its own, and the reason
 * is a single instruction in the host's presenter, `Ltt/b2;.i(Lyt/b;)V` (`classes4.dex`,
 * ins 255-266, verified in `~/apks/dex_files/pinterest_v14.38.0`):
 *
 * ```
 * 255  iget-object       v1, v2, Lbb1/u0;->g Lsa1/i;   // the comments cell
 * 259  if-nez            v20, +009h
 * 260  const/16          v4, 8                            // logged out -> GONE
 * 261  invoke-virtual    v1, v4, View;->setVisibility(I)V
 * 263  const/4           v11, 0                            // logged in  -> VISIBLE
 * 264  invoke-virtual    v1, v11, View;->setVisibility(I)V
 * ```
 *
 * That runs once the comment count has been bound, long after `<init>` returned, which is why
 * hiding in the constructor left the cell on screen. Rewriting the argument inside the cell's own
 * `setVisibility` covers ins 261 and 264 and every other caller at once, which matters because the
 * app has 2,305 `View.setVisibility` call sites and this one is reached through a Dagger-generated
 * presenter whose name changes every release. The override cannot hide a view nobody calls
 * `setVisibility` on, though, which is why the constructor block stays.
 *
 * `invoke-super`, never `invoke-virtual`: the override is the target of the very call it
 * intercepts, so a virtual dispatch here would recurse until the stack blew.
 *
 * `UAB_SUPER_CLASS` is `abstract` and declares no `setVisibility` of its own, so this resolves
 * through it to `View.setVisibility`, the same target the compiler would have emitted.
 */
private val SET_VISIBILITY_BODY = """
    invoke-virtual {p0}, Landroid/view/View;->getContext()Landroid/content/Context;
    move-result-object v0
    const-string v1, "$SETTINGS_KEY"
    invoke-static {v0, v1}, $EXTENSION_CLASS->isEnabled(Landroid/content/Context;Ljava/lang/String;)Z
    move-result v0
    if-eqz v0, :morphe_hide_comments_passthrough
    const/16 p1, $GONE
    :morphe_hide_comments_passthrough
    invoke-super {p0, p1}, $UAB_SUPER_CLASS->setVisibility(I)V
    return-void
""".trimIndent()

@Suppress("unused")
val hideCommentsPatch = bytecodePatch(
    name = "Hide comments",
    description = "Hide the comments button on a pin, so comments cannot be opened from the pin.",
    default = false
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    extendWith("extensions/extension.mpe")

    execute {
        // Hides field `l`, the comments button, in
        // `LegacyPromotedCloseupActionButtonModule.createView`.
        //
        // Three earlier attempts patched `UnifiedPinActionBarView` instead and shipped in
        // v0.6.0-dev.13 through .15. Each applied cleanly and changed nothing, because a pin's
        // closeup screen uses the legacy "promoted" action bar. See the fingerprint for how the
        // uiautomator dump identified the right class.
        //
        // The button is read off `this` rather than reused from a register. `createView` has six
        // registers and one declared parameter, so `this` is `v5` and only `v0` through `v4` are
        // free. `v1` is the register the surrounding code uses for every `findViewById` result; the
        // block below runs after the last read of `v1`, and re-initialises `v1` itself.
        //
        // `const/16`, not `const/4`: `const/4` encodes a signed nibble, so `const/4 v1, 0x8`
        // assembles without complaint but decodes as `-8`, which stores `0xFFF8` in the visibility
        // bits. The button would then be neither VISIBLE, INVISIBLE nor GONE: not drawn, but still
        // holding its layout slot, so the user sees a blank gap instead of a removed button.
        //
        // The insertion index is the crux, and picking it wrong is what made the first three
        // attempts no-ops. Field `l` is written by this `iput-object`:
        //
        //   102  sget           v1, action_module_comment_icon
        //   103  findViewById
        //   104  move-result-object v1
        //   105  check-cast     v1, GestaltIconButton
        //   106  iput-object    v1 -> l          <- insert immediately after this
        //   107  invoke-virtual v5, ->l()V
        //
        // Anchoring on either `fieldAccess` match is wrong. `action_buttons_center` is resolved at
        // ins 58, long *before* `l` exists, and `action_module_comment_icon` at ins 102 is four
        // instructions before the store. Reading `l` at either point yields null, and a null guard
        // would turn that into a silent no-op rather than a crash. So the anchor is found by
        // scanning for the store itself, which is the only position where `l` is guaranteed live.
        val instructions = CommentsButtonFingerprint.method.implementation!!.instructions
        val storeIndex = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.IPUT_OBJECT &&
                (instruction as? ReferenceInstruction)?.reference?.let {
                    it is FieldReference && it.name == "l" && it.definingClass == MODULE_CLASS
                } == true
        }
        check(storeIndex != -1) {
            "Comments button field 'l' store not found in createView; the anchor this patch " +
                "depends on is gone, so inserting earlier would read a null field."
        }

        CommentsButtonFingerprint.method.addInstructionsWithLabels(
            storeIndex + 1,
            """
            iget-object v0, v5, $MODULE_CLASS->l:$BUTTON_TYPE;
            invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;
            move-result-object v1
            const-string v2, "$SETTINGS_KEY"
            invoke-static {v1, v2}, $EXTENSION_CLASS->isEnabled(Landroid/content/Context;Ljava/lang/String;)Z
            move-result v1
            if-eqz v1, :morphe_end_hide_comments
            const/16 v1, $GONE
            invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
            :morphe_end_hide_comments
            nop
            """.trimIndent()
        )

        // Hides `this` at the end of `Lsa1/i.<init>` — the unified action bar
        // comments cell actually on screen (plural `action_module_comments_icon`,
        // plain `LinearLayout` with icon + count), hosted by `Lbb1/u0` field `g`.
        // Kept alongside the legacy `createView` block above, which covers the
        // promoted-closeup variant, until device testing shows which bar each
        // pin type uses.
        //
        // Unlike the legacy module, this view never does `findViewById`: it sets
        // its own id (`setId` at ins 35), so there is no lookup to hook after.
        // The single `return-void` (ins 108, no try blocks) is the only point
        // where the fully built view is guaranteed live, hence the anchor.
        //
        // Registers: `<init>` has 15 registers and `(Context)` params, so `this`
        // is `v13` and the `Context` param is `v14`. At the return, `v1` holds
        // dead constant 22 and `v2` dead padding, so both are safe scratch;
        // `v13` is read but never clobbered. `const/16` for the same signed-
        // nibble reason as above.
        val uabInstructions = UabCommentsButtonFingerprint.method.implementation!!.instructions
        val uabReturns = uabInstructions.indices.filter { index ->
            uabInstructions[index].opcode == Opcode.RETURN_VOID
        }
        check(uabReturns.size == 1) {
            "UAB comments button <init> has ${uabReturns.size} return-voids, expected exactly 1; " +
                "the end-of-constructor anchor needs re-analysis before patching."
        }

        // Its own `setVisibility` call below now goes through the override added further down, so
        // the toggle gets read twice when the patch is on. That is harmless: the override rewrites
        // the argument to the same `GONE`, and `isEnabled` is an in-memory `SharedPreferences`
        // read. Left alone rather than switched to `invoke-super`, which would trade a redundant
        // read for a second super-chain resolution in the constructor.
        UabCommentsButtonFingerprint.method.addInstructionsWithLabels(
            uabReturns.single(),
                """
                invoke-virtual {v13}, Landroid/view/View;->getContext()Landroid/content/Context;
                move-result-object v1
                const-string v2, "$SETTINGS_KEY"
                invoke-static {v1, v2}, $EXTENSION_CLASS->isEnabled(Landroid/content/Context;Ljava/lang/String;)Z
                move-result v1
                if-eqz v1, :morphe_end_hide_uab_comments
                const/16 v1, $GONE
                invoke-virtual {v13, v1}, Landroid/view/View;->setVisibility(I)V
                :morphe_end_hide_uab_comments
                nop
                """.trimIndent()
            )

        // Adds the override that the constructor block above provably cannot replace: the parent
        // presenter calls `setVisibility(VISIBLE)` once it has bound the comment count, which
        // restores the cell after `<init>` has returned. Verified against the class in
        // `classes6.dex` — `Lsa1/i` is `public final`, so nothing can shadow the override, and
        // every call site in the app reaches it by virtual dispatch.
        val uabClass = mutableClassDefBy(UAB_CLASS)
        check(uabClass.methods.none { it.name == SET_VISIBILITY && it.parameterTypes == listOf("I") }) {
            "$UAB_CLASS already declares $SET_VISIBILITY(I)V, so this patch would add a duplicate " +
                "method definition and the dex writer would reject the class. Re-check whether " +
                "the upstream class grew the override itself."
        }

        // Built through `InlineSmaliCompiler` rather than `addInstructionsWithLabels` because that
        // one only inserts into a method that already exists. It wraps the text in a throwaway
        // `.method public dummyMethod(I)V` with these register counts, which is what lets `p0` and
        // `p1` resolve the way they do here.
        val implementation = MutableMethodImplementation(SET_VISIBILITY_REGISTERS)
        InlineSmaliCompiler.compile(
            SET_VISIBILITY_BODY,
            "I",
            SET_VISIBILITY_REGISTERS,
            false
        ).forEach(implementation::addInstruction)

        // Morphe's `MutableClass` hands out a `LinkedHashSet`, so this is the only way to add a
        // method to a class. It has to be Morphe's own `MutableMethod`, though: `getDirectMethods`
        // casts every element of that set to `MutableMethod` while dexlib2 pools the class, so
        // adding a plain `ImmutableMethod` here fails with a `ClassCastException` at write time.
        uabClass.methods.add(
            MutableMethod(
                ImmutableMethod(
                    UAB_CLASS,
                    SET_VISIBILITY,
                    listOf(ImmutableMethodParameter("I", null, null)),
                    "V",
                    AccessFlags.PUBLIC.value,
                    emptySet(),
                    emptySet(),
                    implementation
                )
            )
        )
    }
}
