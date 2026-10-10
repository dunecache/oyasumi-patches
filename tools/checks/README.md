# Static checks

Both scripts run without a device, an APK, or the Morphe plugin.

```sh
python3 tools/checks/patch_smali_checks.py            # the CI check
python3 tools/checks/check_inline_smali.py            # injected smali, via the real compiler
python3 tools/checks/resolve_fp.py com.truecaller 26.31.6   # do the fingerprints match?
python3 tools/checks/replay_history_check.py v0.3.4   # replay a released tag
python3 tools/checks/test_invoke_arity.py             # the arity check's own cases
```

`resolve_fp.py` is the one that closes the fingerprint gap described below. It needs the pinned APK
in the `~/apks` cache and `androguard`, so it does not run in CI; run it locally before tagging a
patch for a new version.

`test_invoke_arity.py` holds the cases `check_invoke_arity` and `check_dollar_in_strings`
must and must not flag. They exist because a check that is wrong in the strict direction
is not a safety net. While writing the Djezzy patch, `check_invoke_arity` reported two
correct `invoke-static` calls as errors (it was counting a receiver that `invoke-static`
does not have), reported a two-argument `Landroid/util/Log;->i(String, String)` call as
one argument (it split the descriptor on whitespace), and had the wide-register arithmetic
backwards (AOSP's verifier counts a `long` as *two* registers, so `z(J)V` needs two, not
one). Obeying it would have added a stray register to a static call and broken the build.
`check_dollar_in_strings` came from the same patch: `Lio/flutter/plugin/common/EventChannel$EventSink;`
is a nested type, and the `$EventSink` was read as a Kotlin template over a name that
does not exist, which fails `:patches:compileKotlin` before any smali is assembled.

## Why this exists

Five releases in a row shipped a defect that compiling cannot catch, because each one is
correct Kotlin that produces wrong smali. Every one of them was a value typed by hand and
never executed before a bundle was published.

| release | defect | caught by |
| --- | --- | --- |
| v0.2.1 – v0.3.3 | `replaceInstructions` deleted the instructions after the target | `check_replace_instructions` |
| v0.3.3 – v0.3.4 | inserted `invoke` named 1 register for a 2-register call | `check_invoke_arity` |
| v0.4.2 | `methodCall` import removed while still in use | `check_imports` |
| v0.5.0 (unreleased) | nested type `$EventSink` read as a Kotlin template | `check_dollar_in_strings` |
| v0.6.0 | a boolean result reused as the next invoke's receiver | `check_invoke_receiver_type` |

Both are silent at build time. smali assembles a `35c` register list of any length, the
Gradle build succeeds, and the patcher only rejects the class when the verifier runs it at
load time — so the failure surfaces on a device as a `VerifyError`, or in the worst case
as a patch that does not apply at all.

The last row is the sharpest example of the gap. Every injected block in this repo is a
triple-quoted literal, and `string_concat_in` — the extractor the other smali checks are built
on — cannot read one, so `check_branch_joins` had been reviewing patches' `name`/`description`
concatenations and no smali at all. `injected_blocks` fixes the extraction, and only
`check_invoke_receiver_type` uses it. Pointing `check_branch_joins` at triple-quoted blocks too
was tried and reverted: it produced 25 findings on patches that ship and work, because its
premise — that two paths giving one register different types at a join is fatal — is stricter
than ART turns out to be. A conflict is apparently only fatal when the register is read before
being reassigned.

`replay_history_check.py` replays those tags to demonstrate the checks fire on the real
defects and stay quiet on the fixes:

```
$ python3 tools/checks/replay_history_check.py v0.3.4 v0.4.1
v0.3.4  chore: Release v0.3.4 [skip ci]
   FAIL DisableHomeScreenAdsPatch.kt:3545: `invoke-virtual {v$receiver}, ...` names
         1 register(s) but ...->setVisibility declares 1 argument(s) and needs 2
   -> 1 problem(s)
v0.4.1  chore: Release v0.4.1 [skip ci]
   -> 0 problem(s)
```

## Fingerprint resolution — `resolve_fp.py`

Whether a filter chain still matches a given APK is now checked, by `resolve_fp.py`, against
the `~/apks` cache. This gap was closed after it cost two Truecaller releases:

| release | defect | caught by |
| --- | --- | --- |
| v0.6.0-dev.31 | `instanceOf` filter on a method that contains `check-cast` and no `instance-of` | `resolve_fp.py`, once written |
| v0.6.0-dev.32 | `checkCast` with the same operand — still no match | device, again |

Both were rejected only on device, as `PatchException: Failed to match the fingerprint`, and both
compiled cleanly and passed `patch_smali_checks.py`. The second release was shipped on the strength
of a `javap` reading that was correct about the opcode and wrong about the operand comparison.

What it checks, per fingerprint: the class, name, return type and parameter list must match exactly
and in order; then each declared filter must be satisfied by at least one instruction in that
method. `string`, `opcode`, `instanceOf`, `checkCast`, `methodCall` and `fieldAccess` are understood.
More than one method sharing a descriptor is reported rather than silently taking the first.

**A note on writing such a checker.** Its first version reported every fingerprint as resolving,
including a deliberately planted one whose literal existed nowhere, because `check_filter` collapsed
"this instruction is not the kind the filter inspects" into "the filter is satisfied". Three states
are needed, not two. The regression cases are in the script's own use: re-adding `instanceOf` to
`PartitionedContactsLookupFingerprint` makes it fail with *"no instruction of this kind in the
method"*, which is the dev.31 defect reproduced exactly.

It reports a **likely** outcome, not a guarantee: it is a reimplementation of the patcher's matching
rules in Python, so treat a PASS as strong evidence and a FAIL as a stop-and-investigate.

**Verifier-visible register typing.** A patch can be arity-correct and still leave a
register holding a reference where an integer is required. `check_invoke_receiver_type` covers
the straight-line case — a primitive used as an invoke's receiver — which is unconditional and so
provable without a verifier. It stops at a label on purpose, because after a merge the register's
type depends on which path arrived. What is still uncovered is a register left holding a
reference where an integer is required at a *use*, with no straight-line path between the write
and the read. That is what the three original `VerifyError`s turned on, and only a real verifier
catches it.

The honest summary: these two checks cover the defects that were mechanically checkable.
The rest still needs a device.
