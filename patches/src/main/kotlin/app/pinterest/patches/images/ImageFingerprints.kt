package app.pinterest.patches.images

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess

/** The resolved-rendition holder: four fields, one per size, plus an EMPTY singleton. */
internal const val RENDITIONS_CLASS = "Lvu2/d1;"

/** One rendition: a url and three ints. */
internal const val RENDITION_CLASS = "Lvu2/e1;"

/**
 * The single place the app chooses which rendition of a pin image to use.
 *
 * Pinterest's API does not seal the rendition: the pin's `images` field is a map keyed by size,
 * and `Lau2/w;.d(Ljava/util/Map;)Lvu2/d1;` reads four of those keys and packs them into four
 * fields. The mapping is not a guess — it is the argument order of the constructor call at the
 * end of that method:
 *
 * ```smali
 * # keys read, in source order:  736x -> v2,  345x -> v3,  236x -> v4,  originals -> v1
 * new-instance p0, Lvu2/d1;
 * invoke-direct {p0, v2, v3, v4, v1}, Lvu2/d1;-><init>(Lvu2/e1;Lvu2/e1;Lvu2/e1;Lvu2/e1;)V
 * ```
 *
 * which given the constructor's `iput` order (`a`, `b`, `c`, `d`) means:
 *
 * | Field | Size |
 * | --- | --- |
 * | `a` | `736x` |
 * | `b` | `345x` |
 * | `c` | `236x` |
 * | `d` | `originals` |
 *
 * `b()` is the consumer, and its stock preference order is `a`, `d`, `b`, `c` — 736x first,
 * originals only as the fallback. That ordering is the whole patch surface: see
 * `ForceOriginalImageFingerprint`'s user in the patch file.
 *
 * ## Why this method and not the map reader
 *
 * `Lau2/w;.d(...)` carries the only unobfuscated anchors in this whole area — the literals
 * `"736x"`, `"345x"`, `"236x"` and `"originals"` — so it is the more robust place to *find* this
 * code again, and the notes say so. But rewriting it means re-deriving four `ImageDomain`
 * lookups, four width/height extractions and the null handling for each. Reordering two field
 * reads here is two instructions. The robust anchor is documented; the edit is taken where it is
 * smallest.
 *
 * ## Known fragility, stated plainly
 *
 * Both the class and the method name are obfuscated, so this is the same fragility class as the
 * AppsFlyer patch and not the Google Engage pattern. What limits the damage is that `b()` is the
 * *only* reader of field `a` anywhere in the APK — the sole other hit is the constructor's own
 * `iput` — so if the name moves, the fingerprint fails to resolve rather than resolving onto an
 * unrelated method. Both filters are field reads on the defining class, which no unrelated
 * four-field value holder will satisfy in this order.
 */
object ForceOriginalImageFingerprint : Fingerprint(
    definingClass = RENDITIONS_CLASS,
    name = "b",
    returnType = RENDITION_CLASS,
    parameters = listOf(),
    filters = listOf(
        // The field the patch promotes: originals.
        fieldAccess(
            definingClass = RENDITIONS_CLASS,
            name = "d",
            type = RENDITION_CLASS
        ),
        // The last field in the chain, 236x, which is what makes this the four-deep chooser and
        // not some other accessor over the same class.
        fieldAccess(
            definingClass = RENDITIONS_CLASS,
            name = "c",
            type = RENDITION_CLASS
        )
    )
)