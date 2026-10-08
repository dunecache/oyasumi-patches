package app.pinterest.patches.sections

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.pinterest.patches.shared.Constants.COMPATIBILITY_PINTEREST
import app.pinterest.patches.shared.versionCheckPatch

/**
 * The id of the "More ideas for this board" header section, from `rv0/k0`, `sv0/d2` and
 * `wv0/i0`. Unobfuscated camelCase, taken from `getId()`.
 */
private const val HEADER_SECTION_ID = "MoreIdeasHeader"

/** The id of the ideas list rendered under that header. */
private const val CONTENT_SECTION_ID = "MoreIdeas"

@Suppress("unused")
val hideMoreIdeasSectionPatch = bytecodePatch(
    name = "Hide \"More ideas for this board\" section",
    description = "Remove the \"More ideas for this board\" section from a board, header and " +
        "contents alike.",
    default = false
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    dependsOn(versionCheckPatch)

    execute {
        // Skips the registration outright rather than hiding the section's view, because there is
        // no view to hide: the board page is Compose, so `a0` appends a descriptor to the builder
        // and the section only exists because it is in the builder. Returning early from `a0` is
        // exactly "never registered".
        //
        // `a0` returns `void`, so there is no value to supply and no caller to satisfy. It only
        // appends — the whole method is two `ArrayList.add` calls and a `LinkedHashSet.add`,
        // with no `iput` and no read of the builder beyond those three collections — so skipping
        // it cannot leave the builder half-updated. `b0()`, which builds the final `Lax2/a0` from
        // the same three collections, is a pure read and does not require any particular section
        // to be present.
        //
        // The gate is on the section id rather than on the caller, so one hook covers all three
        // board variants: `com/pinterest/feature/board/redesign/tabbed/i2.smali` registers
        // `rv0/k0`/`wv0/i0` ids and `a2/c0.smali` registers `sv0/d2` ones.
        //
        // `MoreIdeas` is not a unique string across the APK — five enums declare a constant with
        // that id. It is unique *here*: of the seven enum constants whose id is exactly
        // `MoreIdeas` or `MoreIdeasHeader`, only `i2.smali` and `a2/c0.smali` also call `a0`.
        // The other two, `iv0/l` and `en1/d`, are consumed by unrelated code (an `ArrayList`
        // build in `rv0/x.smali:734`), so nothing else can reach this gate.
        //
        // Both ids must be listed. `MoreIdeasHeader` is the section whose title is
        // `board_more_ideas_section_header` ("More ideas for this board", resource id
        // `0x7f1402a9`), and `MoreIdeas` is the list registered immediately after it. Skipping
        // only the header would leave an unlabelled list, so the header alone is not enough.
        //
        // `p9` is null-tested rather than assumed: the id comes from `getId()` at every known
        // call site, but `String.equals` on a null receiver throws, and a null there must mean
        // "not a more-ideas section" rather than a crash.
        //
        // Registers: `.registers 25` with thirteen parameters, so `p0`..`p12` are `v12`..`v24`
        // and `v0`..`v11` are the only locals. The block uses `v0` and `v1`, inserted before the
        // method's own first instruction (`move/from16 v0, p12`), so neither is live yet.
        //
        // `p9` is `v21`, and it is **copied into `v0` rather than named in place**. That is not
        // style. A `35c` invoke register list is four bits per register, so every register in one
        // must be `v0`..`v15`; `invoke-virtual {p9, v0}` names `v21` and the assembler rejects it
        // with "Invalid register". `move-object/from16` is a `22x`, whose registers are eight
        // bits, so it can carry `p9` freely, and the compares then name only `v0`/`v1`. The
        // alternative spellings are no better: `move-object` is a `12x` and has the same four-bit
        // limit. `check_inline_smali.py` catches this and `:patches:compileKotlin` does not.
        //
        // This is gated by patch selection rather than by the Morphe settings toggle that
        // `HideIdeasSectionPatch` reads, because there is no `Context` here to read it with:
        // `a0` takes no one and `La0/f` stores no `Context` field. See NOTES.md.
        MoreIdeasSectionFingerprint.method.addInstructionsWithLabels(
            0,
            """
            if-eqz p9, :morphe_end_hide_more_ideas_section
            move-object/from16 v0, p9
            const-string v1, "$HEADER_SECTION_ID"
            invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v0
            if-nez v0, :morphe_skip_more_ideas_section
            const-string v1, "$CONTENT_SECTION_ID"
            invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v0
            if-eqz v0, :morphe_end_hide_more_ideas_section
            :morphe_skip_more_ideas_section
            return-void
            :morphe_end_hide_more_ideas_section
            nop
            """.trimIndent()
        )
    }
}