# Static checks

Both scripts run without a device, an APK, or the Morphe plugin.

```sh
python3 tools/checks/patch_smali_checks.py            # the CI check
python3 tools/checks/replay_history_check.py v0.3.4   # replay a released tag
python3 tools/checks/test_invoke_arity.py             # the arity check's own cases
```

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

## What is not covered

**Fingerprint resolution.** Whether a filter chain still matches a given APK needs the
pinned APK, which is a user-supplied 80 MB file that CI cannot fetch. That gap is real:
v0.4.0 declared `returnType = "Ljava/util/Timer;"` for `Timer.schedule`, which returns
`void`, and the fingerprint silently matched nothing. Checking that needs the reference
DEX and is not implemented here.

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
