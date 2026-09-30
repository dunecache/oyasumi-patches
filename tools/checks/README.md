# Static checks

Both scripts run without a device, an APK, or the Morphe plugin.

```sh
python3 tools/checks/patch_smali_checks.py            # the CI check
python3 tools/checks/replay_history_check.py v0.3.4   # replay a released tag
```

## Why this exists

Five releases in a row shipped a defect that compiling cannot catch, because each one is
correct Kotlin that produces wrong smali. Every one of them was a value typed by hand and
never executed before a bundle was published.

| release | defect | caught by |
| --- | --- | --- |
| v0.2.1 – v0.3.3 | `replaceInstructions` deleted the instructions after the target | `check_replace_instructions` |
| v0.3.3 – v0.3.4 | inserted `invoke` named 1 register for a 2-register call | `check_invoke_arity` |

Both are silent at build time. smali assembles a `35c` register list of any length, the
Gradle build succeeds, and the patcher only rejects the class when the verifier runs it at
load time — so the failure surfaces on a device as a `VerifyError`, or in the worst case
as a patch that does not apply at all.

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
register holding a reference where an integer is required. That is what the three original
`VerifyError`s turned on, and only a real verifier catches it.

The honest summary: these two checks cover the defects that were mechanically checkable.
The rest still needs a device.
