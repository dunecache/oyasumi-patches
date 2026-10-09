package app.pinterest.patches.sections

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch

/** `View.GONE`. Needs `const/16`: `const/4` has a signed 4-bit literal and cannot encode 8. */
private const val GONE = "0x8"

/** Reads this patch's toggle from the Morphe settings. */
private const val EXTENSION_CLASS = "Lapp/oyasumi/extension/MorpheSettingsActivity;"

private const val SETTINGS_KEY = "morphe_hide_ideas_section"

@Suppress("unused")
val hideIdeasSectionPatch = bytecodePatch(
    name = "Hide \"Ideas you might love\" section (DIAGNOSTIC BUILD)",
    description = "Hide the suggested-topics section shown under a pin.",
    default = false
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    extendWith("extensions/extension.mpe")

    execute {
        // Hides the section's own view, right after the discriminator switch in
        // `Lhr1/f;.e(Liu1/l;Ljava/lang/Object;I)V`.
        //
        // Index 2 is the instruction immediately after the `packed-switch`, which is the whole
        // point of this patch. `Lhr1/f` serves four closeup sections, told apart by field `a`, and
        // `.packed-switch 0x0` covers keys 0, 1 and 2 only. A `packed-switch` has no default arm,
        // so the fourth variant — discriminator `3`, the one `pr/i9` builds at `pr/i9.smali:1365`
        // — falls through to index 2 and nowhere else. Every sibling arm jumps to a `:pswitch_*`
        // label near the end of the method and therefore never reaches this insertion point. So
        // the patch does not have to identify the topic section by its own contents: the control
        // flow already selects it. The sibling arms are not collateral damage, they are skipped.
        //
        // The payload was read out of the dex to confirm this rather than trusted from the
        // disassembly: `ident=0x0100 size=3 first_key=0 targets=[454, 0, 413]`, so the covered
        // keys are exactly [0, 1, 2] and a discriminator of 3 has no arm.
        // See `.scratch/check_pswitch.py`.
        //
        // Setting `GONE` rather than returning early is deliberate. This method is the shared
        // `Lc01/g.e` bind entry point, called from `Lm72/d0;.n` with the view it is about to
        // populate, and `e` returns `void`, so there is nothing to signal "skip me" with. Hiding
        // the view lets the presenter still bind its items into a view that is not drawn, which
        // keeps the surrounding bind contract intact. It also survives recycling: `m72/d0.n` sets
        // only a content description on the way out and never calls `setVisibility`, and neither
        // does `xx0/j`, `qc1/b`, `qc1/c`, `wx0/e` or `hr1/f` itself, so nothing restores `VISIBLE`
        // after this runs.
        //
        // `p1` is the view parameter (`Liu1/l`) and is still live here: the fall-through arm has
        // not yet reached its own `instance-of`/`check-cast` pair. It is tested again rather than
        // assumed, because `Liu1/l` is an interface and the sibling arm that shares this register
        // explicitly nulls `p1` when the argument is not a `View`.
        //
        // `p1` is `v8`, which a `35c` invoke register list cannot name -- those are four bits per
        // register, so `v0`..`v15` only. That is why the block routes it through `v0`, and it is
        // why the `move-object/from16` on the next line is load-bearing rather than redundant: the
        // `instance-of` above leaves an **int** in `v0`, so a `check-cast` straight onto `v0`
        // rejects the class at load time,
        //
        //   [0x9] check-cast on non-reference in v0
        //
        // `check-cast` is here to convince the verifier, not the runtime -- `instance-of` has
        // already established the type -- so copying the reference in first is what makes the cast
        // legal. `move-object/from16` is a `22x` and can carry `p1` at any register; `move-object`
        // would be a `12x` and could not.
        //
        // Registers: `e` declares `.registers 11` with four parameters, so `this` is `v7`, the
        // view is `v8`, the model `v9`, the position `v10`, and `v0` through `v6` are free. This
        // block uses `v0` (the view), `v1` (the context, then the flag, then the visibility) and
        // `v2` (the settings key). All three are re-initialised by the surrounding code before it
        // reads them — `v0` by the `sget-object` of `Laj0/f.a`, `v1` and `v2` by the two
        // `const/4 0x0` that follow — so clobbering them here is safe.
        //
        // Field references use the `->member:Type` spelling, not the `->member Type` spelling
        // baksmali prints: the inline smali compiler is an ANTLR grammar that requires the colon.
        IdeasSectionFingerprint.method.addInstructionsWithLabels(
            2,
            """
            instance-of v0, p1, Landroid/view/View;
            if-eqz v0, :morphe_diag_notview
            move-object/from16 v0, p1
            check-cast v0, Landroid/view/View;
            invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;
            move-result-object v1
            const-string v2, "$SETTINGS_KEY"
            invoke-static {v1, v2}, $EXTENSION_CLASS->isEnabled(Landroid/content/Context;Ljava/lang/String;)Z
            move-result v1
            const-string v3, "MorpheIdeas"
            if-nez v1, :morphe_diag_on
            const-string v4, "toggle OFF"
            invoke-static {v3, v4}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
            goto :morphe_end_hide_ideas_section
            :morphe_diag_on
            const-string v4, "toggle ON"
            invoke-static {v3, v4}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
            const/16 v1, $GONE
            invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
            goto :morphe_end_hide_ideas_section
            :morphe_diag_notview
            const-string v3, "MorpheIdeas"
            const-string v4, "p1 NOT a View"
            invoke-static {v3, v4}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
            :morphe_end_hide_ideas_section
            nop
            """.trimIndent()
        )

        // Inserted at 0, i.e. before the `packed-switch`, so it fires on EVERY call whatever the
        // discriminator is. The block above only runs on the fall-through, and the first logcat
        // contained no `MorpheIdeas` line at all -- which cannot distinguish "this method is never
        // called" from "it is called but not with the discriminator that falls through". This one
        // log answers that: nothing means the wrong presenter, a number means the wrong arm.
        //
        // `p0` is `this` and is live at method entry, so field `a` is readable before the method's
        // own `iget p0, p0, Lhr1/f;->a:I`. A separate tag keeps the discriminator distinguishable
        // from the outcome lines.
        //
        // Inserted after the index-2 block on purpose: insertions are applied against the indices
        // as they were, so the higher one has to go first.
        IdeasSectionFingerprint.method.addInstructionsWithLabels(
            0,
            """
            iget v4, p0, Lhr1/f;->a:I
            invoke-static {v4}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;
            move-result-object v4
            const-string v3, "MorpheIdeasEntry"
            invoke-static {v3, v4}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
            """.trimIndent()
        )
    }
}