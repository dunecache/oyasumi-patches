package app.truecaller.patches.contacts

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

/**
 * The contact list is read through one interface, `Le81/g;`, whose single accessor is
 *
 * ```text
 * D(Lcom/truecaller/contacts_list/ContactsHolder$FavoritesFilter;
 *   Lcom/truecaller/contacts_list/ContactsHolder$PhonebookFilter;)Ljava/util/List;
 * ```
 *
 * Exactly two classes implement that interface, verified by enumerating every method in the APK
 * with that descriptor and every class listing `Le81/g;` among its interfaces:
 *
 * | class | access | what it reads |
 * | --- | --- | --- |
 * | `Le81/x;` | `public final` | field `G : La52/g0;`, or field `J : Ljava/util/ArrayList;` |
 * | `Lj71/d;` | `public final` | field `d : Leh3/c2;` → `j71/b.c : Ljava/util/List;` |
 *
 * Plus one direct reader of the underlying holder that skips the interface altogether,
 * `La52/g0;->u`. All three are needed; see `EmptyContactListPatch` for why.
 *
 * The interface itself, `Le81/g;->D`, is `public abstract` and has no body, so there is nothing to
 * patch there — which is exactly why patching a single implementation is not enough.
 *
 * **Why these fingerprints are stable.** Every class and method name involved is R8-minified and
 * would be the first thing a future release renames, so none of them is load-bearing on its own.
 * What pins each method is material that a rename does not touch: the two parameter types are
 * app-owned enum names under `com/truecaller/contacts_list/`, the return type is
 * `Ljava/util/List;`, and both bodies open with Kotlin's null-check intrinsics carrying the
 * parameter names `"favoritesFilter"` and `"phonebookFilter"` as literals. Those two literals are
 * asserted as `string` filters on all three fingerprints: they are emitted from the Kotlin
 * signature rather than written by hand, so they survive R8 renaming, and they are what makes the
 * filter set specific enough that a repurposed `D` in a still-named `e81/x` fails loudly at patch
 * time instead of matching silently.
 */

/** `Le81/x;->D`, the accessor backed by the partitioned `List[][]` in `La52/g0`. */
object ContactsHolderAccessorFingerprint : Fingerprint(
    definingClass = "Le81/x;",
    name = "D",
    returnType = "Ljava/util/List;",
    parameters = listOf(
        "Lcom/truecaller/contacts_list/ContactsHolder\$FavoritesFilter;",
        "Lcom/truecaller/contacts_list/ContactsHolder\$PhonebookFilter;"
    ),
    filters = listOf(
        string("favoritesFilter"),
        string("phonebookFilter")
    )
)

/** `Lj71/d;->D`, the accessor backed by the cached `List` in `Lj71/b.c`. */
object CachedContactsAccessorFingerprint : Fingerprint(
    definingClass = "Lj71/d;",
    name = "D",
    returnType = "Ljava/util/List;",
    parameters = listOf(
        "Lcom/truecaller/contacts_list/ContactsHolder\$FavoritesFilter;",
        "Lcom/truecaller/contacts_list/ContactsHolder\$PhonebookFilter;"
    ),
    filters = listOf(
        string("favoritesFilter"),
        string("phonebookFilter")
    )
)

/**
 * `La52/g0;->u`, the 2-D lookup into `La52/g0.b`.
 *
 * **No type filter, deliberately.** This fingerprint failed on device twice, once per attempt:
 * `instanceOf("[[Ljava/util/List;")` in v0.6.0-dev.31, then `checkCast` with the same operand in
 * v0.6.0-dev.32. The method really does contain `check-cast v0, [[Ljava/util/List;` -- verified
 * against the dex -- so the fault is in how the filter compares an array-typed operand, not in the
 * target. Both spellings are dropped rather than guessed at a third time.
 *
 * Nothing is lost by dropping it. Class plus name plus this descriptor already identifies the
 * method uniquely: across all 9 dex files only four methods carry the descriptor, and `La52/g0;->u`
 * is the only one named `u`. The two `string` filters are what make it fail loudly if the method is
 * ever repurposed, and they are proven -- `ContactsHolderAccessorFingerprint` and
 * `CachedContactsAccessorFingerprint`, which use nothing but these same two literals, both
 * resolved successfully against this build on device.
 */
object PartitionedContactsLookupFingerprint : Fingerprint(
    definingClass = "La52/g0;",
    name = "u",
    returnType = "Ljava/util/List;",
    parameters = listOf(
        "Lcom/truecaller/contacts_list/ContactsHolder\$FavoritesFilter;",
        "Lcom/truecaller/contacts_list/ContactsHolder\$PhonebookFilter;"
    ),
    filters = listOf(
        string("favoritesFilter"),
        string("phonebookFilter")
    )
)