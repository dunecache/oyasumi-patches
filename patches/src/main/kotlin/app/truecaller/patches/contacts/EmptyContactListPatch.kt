package app.truecaller.patches.contacts

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.truecaller.patches.shared.Constants.COMPATIBILITY_TRUECALLER

@Suppress("unused")
val emptyContactListPatch = bytecodePatch(
    name = "Return an empty contact list",
    description = "Make the app see an empty contact list, so nothing is shown from your address " +
        "book and nothing can be matched or uploaded from it.",
    default = true
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        // Three methods, all returning `Ljava/util/List;`, all patched the same way. One method
        // would have been a silent no-op for two of the three read paths, which is why this is
        // three and not one.
        //
        // 1. `Le81/x;->D` and 2. `Lj71/d;->D` are the only two implementations of the `Le81/g;`
        //    accessor. The interface method itself is abstract, so it has no body to patch, and
        //    patching either implementation alone leaves the other live. `Le81/x.D` in particular
        //    has **zero direct callers** — it is only ever reached through `invoke-interface` on
        //    `Le81/g;` — so grepping for its name suggests it is unused when it is the main path.
        // 3. `La52/g0;->u` is the lookup into the partitioned `List[][]`. Two app classes call it
        //    directly, bypassing the interface entirely:
        //    `com/truecaller/account/domain/auth/i;.invokeSuspend` and `dj2/o;.invokeSuspend`.
        //    The first sits in the account/auth domain, which is where an onboarding phonebook
        //    upload would live, so leaving it alone would defeat the point of the patch.
        //
        // Patching all three is exhaustive for the read paths found: a scan of all 9 dex files and
        // all 86,463 classes found no other route to the list, and the interface has exactly these
        // two implementors.
        //
        // `La52/g0;->K`, the matching writer, is deliberately left alone. It only populates the
        // holder, and nothing reads the result any more once the three readers above return empty,
        // so suppressing it would add risk without changing behaviour.
        //
        // An empty list is returned rather than `null` because every original path returns a real
        // `List` and callers are not obliged to null-check. Empty keeps the type contract intact,
        // so a caller that iterates or calls `size()` behaves normally instead of faulting.
        //
        // `Collections.emptyList()` erases to `()Ljava/util/List;`, so `move-result-object` and
        // `return-object` agree with the declared return type and the verifier sees a reference in
        // `v0` throughout.
        //
        // The payload is inlined rather than shared through a `private const val` because
        // `tools/checks/check_inline_smali.py` collects blocks by matching a literal triple-quoted
        // string at the `addInstructionsWithLabels` call site, and a bare identifier would leave
        // these three blocks outside the one check that can catch malformed smali without a device.
        //
        // `v0` rather than `p0` on purpose, matching the rest of this repository: the renderer
        // accepts a parameter register but nothing here exercises one.

        // `Le81/x;->D`. `.registers 4` with three parameters, so `v0` is the only local and `this`
        // plus both filters occupy `v1`-`v3`.
        ContactsHolderAccessorFingerprint.method.addInstructionsWithLabels(
            0,
            """
            invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
            move-result-object v0
            return-object v0
            """.trimIndent()
        )

        // `Lj71/d;->D`. `.registers 4` with three parameters, same map as above.
        CachedContactsAccessorFingerprint.method.addInstructionsWithLabels(
            0,
            """
            invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
            move-result-object v0
            return-object v0
            """.trimIndent()
        )

        // `La52/g0;->u`. `.registers 7` with three parameters; the dump puts `this` in `v4` and the
        // two filters in `v5`/`v6`, so `v0`-`v3` are locals and `v0` is free.
        PartitionedContactsLookupFingerprint.method.addInstructionsWithLabels(
            0,
            """
            invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
            move-result-object v0
            return-object v0
            """.trimIndent()
        )
    }
}