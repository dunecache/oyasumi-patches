package app.pinterest.patches.sections

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

/**
 * The board-feed section registration DSL, `La0/f;.a0`.
 *
 * `La0/f` is the Compose scope the board page builds its sections in. Its single `a0` method
 * **appends one section** to the builder: it constructs an `Lax2/k2` descriptor, adds it to
 * field `b` (an `ArrayList`), adds an `Lax2/z` wrapper of field `c` (another `ArrayList`), then
 * inserts the section id into field `d`, a `LinkedHashSet` used for duplicate detection. It
 * returns `void`, reads no builder field other than those three, and contains no `iput` at all, so
 * the builder's state is only ever extended, never derived. That is what makes "return early"
 * equivalent to "never registered", rather than merely similar.
 *
 * The section id is the tenth declared parameter, `Ljava/lang/String;`, which the body loads into
 * `v9` and then passes to `LinkedHashSet.contains`/`add`. Every caller builds it from an enum's
 * `getId()`, so it is a readable English camelCase string, never an obfuscated name.
 *
 * This is a different seam from [IdeasSectionFingerprint] on purpose. That one hides a bound
 * `View` in a `Lc01/g` presenter; the board page is Compose and registers sections rather than
 * binding them, so there is no view to hide and no `View.getContext()` to read a toggle with.
 *
 * Resolved against the pinned APK: `La0/f` declares exactly one method named `a0`, so class,
 * name and the twelve-parameter descriptor already resolve uniquely. The `methodCall` filter is
 * kept as insurance and documents intent — it pins the `ArrayList.add` that makes this the
 * appending entry point, which is what a future overload would most likely be confused with.
 */
object MoreIdeasSectionFingerprint : Fingerprint(
    definingClass = "La0/f;",
    name = "a0",
    returnType = "V",
    parameters = listOf(
        "Lax2/o3;", "Lax2/m;", "Lax2/u1;", "Z", "Lax2/f;", "Lax2/l;",
        "Lax2/v1;", "Lax2/l;", "Ljava/lang/String;", "Lax2/v1;", "Lax2/n;", "I"
    ),
    filters = listOf(
        methodCall(
            definingClass = "Ljava/util/ArrayList;",
            name = "add",
            parameters = listOf("Ljava/lang/Object;"),
            returnType = "Z"
        )
    )
)