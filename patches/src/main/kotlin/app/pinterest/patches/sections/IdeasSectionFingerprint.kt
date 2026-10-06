package app.pinterest.patches.sections

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.instanceOf
import app.morphe.patcher.methodCall

/**
 * The presenter that binds the "Ideas you might love" topic section, `Lhr1/f;.e`.
 *
 * `Lhr1/f` is one class serving four different closeup sections, told apart by an `int`
 * discriminator read from field `a` and passed to the constructor as its third argument. The
 * method body opens with
 *
 * ```
 * iget        p0, p0, Lhr1/f;->a:I
 * packed-switch p0, :pswitch_data_1f8
 * ```
 *
 * and `.packed-switch 0x0` covers exactly keys 0, 1 and 2. **A `packed-switch` has no default
 * arm, so a discriminator of 3 falls straight through to the instruction after it** — which is
 * the only path that reaches the topic code this patch cares about. All four construction sites
 * were enumerated: `pr/i9` builds discriminators `0` (`field f`), `1` (`field g`) and `3`
 * (`field Q`, at `pr/i9.smali:1365`), and `di1/t` builds `2` (`di1/t.smali:283`). Only `3`
 * reaches the fall-through.
 *
 * That fall-through arm is the one that:
 * - unwraps the model, accepting either `z5` directly or `ti` and reading its field `s`;
 * - resolves the already-created view with `Le53/a;->e(View)` and requires it to be a
 *   `Lwx0/e` binder;
 * - walks `z5.y`, keeping `q7` items whose `m()` is non-blank, via `Lkotlin/text/StringsKt;->K`;
 * - delegates each kept item to `Lwx0/e;->r3(q7, int)`.
 *
 * `Lwx0/e.r3` is what builds the cells: it `new`s `xx0/o` or `xx0/q` and `addView`s them. Both
 * descend from `xx0/c`, whose `D(String, Z)` sets the cell's content description from
 * `content_description_bubble_cell` — the string `Topic: %1$s`. That is the source of the
 * `content-desc="Topic: …"` in the device dump, and it is the link that ties this class to the
 * section on screen.
 *
 * The filters below are **not** load-bearing on 14.38.0. The class declares four methods and
 * exactly one is named `e`, so class + signature alone already resolves uniquely; verified by
 * `.scratch/resolve_ideas_fp2.py`, which drops each filter in turn and still matches every time.
 * They are kept as insurance against a release that adds a second `e` overload, which would
 * otherwise resolve to the wrong method silently. The names they pin (`q7`, `wx0/e.r3`) are
 * obfuscated and therefore the ones a future release is most likely to rename — with the filters,
 * a rename fails loudly at patch time instead.
 *
 * Resolved against the pinned APK's dex: exactly one survivor, `classes6.dex`,
 * `registers=11`, 264 instructions. See `.scratch/resolve_ideas_fp.py`.
 */
object IdeasSectionFingerprint : Fingerprint(
    definingClass = "Lhr1/f;",
    name = "e",
    returnType = "V",
    parameters = listOf("Liu1/l;", "Ljava/lang/Object;", "I"),
    filters = listOf(
        instanceOf("Lcom/pinterest/api/model/z5;"),
        instanceOf("Lcom/pinterest/api/model/q7;"),
        methodCall(
            definingClass = "Lkotlin/text/StringsKt;",
            name = "K",
            parameters = listOf("Ljava/lang/CharSequence;"),
            returnType = "Z"
        ),
        methodCall(
            definingClass = "Lwx0/e;",
            name = "r3",
            parameters = listOf("Lcom/pinterest/api/model/q7;", "I"),
            returnType = "V"
        )
    )
)