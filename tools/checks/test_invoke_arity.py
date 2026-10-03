"""
Cases for `check_invoke_arity`, the check that exists because five releases in a row
shipped smali that assembled cleanly and failed the verifier on a device.

Every case is either a defect the check must catch or a correct instruction it must stay
quiet on, so the check cannot be "fixed" into silence by loosening it. Run it directly:

    python3 tools/checks/test_invoke_arity.py
"""

from __future__ import annotations

import importlib.util
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("patch_smali_checks", HERE / "patch_smali_checks.py")
checks = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checks)


def source(instruction: str) -> str:
    """A patch body shaped the way real ones are: the smali is a "..." + "..." chain."""
    return (
        'val p = bytecodePatch(n = "x") {\n'
        "    execute {\n"
        "        addInstructions(0,\n"
        '            "const/4 v0, 0\\n" +\n'
        f'            "{instruction}"\n'
        "        )\n"
        "    }\n"
        "}\n"
    )


def flagged(instruction: str) -> bool:
    path = Path(tempfile.mkstemp(suffix=".kt")[1])
    try:
        path.write_text(source(instruction))
        return bool(checks.check_invoke_arity(path))
    finally:
        path.unlink(missing_ok=True)


def dollar_flagged(src: str) -> bool:
    path = Path(tempfile.mkstemp(suffix=".kt")[1])
    try:
        path.write_text(src)
        return bool(checks.check_dollar_in_strings(path))
    finally:
        path.unlink(missing_ok=True)


#: Each row is (what it is, the smali, whether the check must flag it).
CASES: list[tuple[str, str, bool]] = [
    # Defects the check exists to catch. The first is the shipped v0.3.4 defect.
    ("v0.3.4 defect: virtual, receiver only",
     "invoke-virtual {v5}, Landroid/view/View;->setVisibility(I)V", True),
    ("interface, one register short",
     "invoke-interface {v0}, Lx/Y;->z(Ljava/lang/Object;)V", True),
    ("static given a phantom receiver",
     "invoke-static {v0, v1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;", True),
    ("two same-type arguments, three registers",
     "invoke-static {v0, v1, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I", True),
    ("long given a single register",
     "invoke-static {v0}, Lx/Y;->z(J)V", True),
    ("long given a single register, virtual",
     "invoke-virtual {v0}, Lx/Y;->z(J)V", True),
    # Correct instructions the check must not touch.
    ("virtual: receiver plus one argument",
     "invoke-virtual {v5, v0}, Landroid/view/View;->setVisibility(I)V", False),
    ("static: arguments only, no receiver",
     "invoke-static {v1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;", False),
    ("static: two same-type arguments",
     "invoke-static {v1, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I", False),
    ("static: a long occupies two registers",
     "invoke-static {v0, v1}, Lx/Y;->z(J)V", False),
    ("static: long plus int occupies three",
     "invoke-static {v0, v1, v2}, Lx/Y;->z(JI)V", False),
    ("virtual: receiver plus a long occupies three",
     "invoke-virtual {v0, v1, v2}, Lx/Y;->z(J)V", False),
    ("virtual: String plus Object[]",
     "invoke-virtual {v0, v1, v2}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", False),
]


#: `check_dollar_in_strings` cases, in the same two-column shape. A `$name` that the file
#: does not declare is a compile error; a deliberate template is not.
DOLLAR_CASES: list[tuple[str, str, bool]] = [
    ("nested type descriptor, unescaped",
     'val p = bytecodePatch(n = "x") { execute { addInstructions(0,\n'
     '  "const/4 v0, 0\\n" +\n'
     '  "invoke-interface {v0, v1}, Lio/flutter/plugin/common/EventChannel$EventSink;'
     '->success(Ljava/lang/Object;)V") } }', True),
    ("nested type descriptor in a fingerprint argument",
     'val f = Fingerprint(definingClass = "Lx/Y$Inner;")', True),
    ("nested type descriptor, escaped",
     'val p = bytecodePatch(n = "x") { execute { addInstructions(0,\n'
     '  "const/4 v0, 0\\n" +\n'
     '  "invoke-interface {v0, v1}, Lio/flutter/plugin/common/EventChannel\\$EventSink;'
     '->success(Ljava/lang/Object;)V") } }', False),
    ("deliberate const template",
     'private const val TAG = "djezzy"\n'
     'val p = bytecodePatch(n = "x") { execute { addInstructions(0, '
     '"const-string v0, \\"$TAG\\"\\n" + "return-void") } }', False),
    ("deliberate local template",
     'val p = bytecodePatch(n = "x") { execute { val reg = "v0"\n'
     '  addInstructions(0, "const/4 $reg, 0\\n" + "return-void") } }', False),
    ("bare dollar, not a template",
     'val p = bytecodePatch(n = "x") { execute { addInstructions(0, '
     '"const-string v0, \\"100$\\"") } }', False),
]



#: A `//` comment between a literal and the `+` continuing the chain must not stop the
#: fold. It did once: every literal after the comment was dropped, so a trailing smali
#: label -- the operand of a branch -- went unchecked while still being emitted. These rows
#: put a bad invoke *after* a comment, so a fold that stops early reports no problem.
COMMENT_CASES: list[tuple[str, str, bool]] = [
    (
        "bad invoke after an interposed comment is caught",
        "invoke-static {v0, v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;",
        True,
    ),
    (
        "correct invoke after an interposed comment is not flagged",
        "invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;",
        False,
    ),
]


def comment_flagged(instruction: str) -> bool:
    src = (
        'val p = bytecodePatch(n = "x") {\n'
        "    execute {\n"
        "        addInstructions(0,\n"
        '            "const/4 v0, 0\\n" +\n'
        "            // a comment the compiler ignores\n"
        f'            "{instruction}"\n'
        "        )\n"
        "    }\n"
        "}\n"
    )
    path = Path(tempfile.mkstemp(suffix=".kt")[1])
    try:
        path.write_text(src)
        return bool(checks.check_invoke_arity(path))
    except Exception:
        return False
    finally:
        path.unlink(missing_ok=True)



#: A conditional branch whose target label sits in the same fragment is a join, and Dalvik
#: requires both paths to agree on every register's type. These rows are the shape that
#: shipped and took the app down with a blank screen -- gating the pedometer push on the
#: channel name put a `String` in `v0` on the skip path and an `Integer` in `v0` on the push
#: path, and the verifier rejected the class at load:
#:
#:     VerifyError: Verifier rejected class i5.c: i5.c.onListen failed to verify:
#:     [0x2C] register v0 has type Conflict but expected Reference: i5.b
#:
#: The second row is the shape that is fine, and it is fine for a specific reason worth
#: keeping: the skipped block ends in `return-object`, so the fall-through path never
#: reaches the label and there is no join to reconcile.
BRANCH_CASES: list[tuple[str, str, bool]] = [
    (
        "merging branch is flagged",
        r'const-string v0, "StepCount"' + "\n"
        + r"invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z" + "\n"
        + "move-result v0\n"
        + "if-eqz v0, :skip\n"
        + "const/16 v0, 0x2710\n"
        + "move-result-object v0\n"
        + ":skip",
        True,
    ),
    (
        "merging branch is flagged past an interposed comment",
        r'const-string v0, "StepCount"' + "\n"
        + "move-result v0\n"
        + "if-eqz v0, :skip\n"
        + "const/16 v0, 0x1\n"
        + ":skip",
        True,
    ),
    (
        "skipped block ending in a return is not a join",
        r'const-string v0, "StepCount"' + "\n"
        + "move-result v0\n"
        + "if-eqz v0, :pass\n"
        + "const-wide/16 v0, 0x2710\n"
        + "return-object v0\n"
        + ":pass",
        False,
    ),
    (
        "skipped block touching only fresh registers is fine",
        "iget-object v0, v2, Lx;->l:Ljava/lang/String;\n"
        + "move-result v0\n"
        + "if-eqz v0, :skip\n"
        + "const/16 v4, 0x2710\n"
        + ":skip",
        False,
    ),
]


def join_flagged(smali: str, comment: bool = False) -> bool:
    """Write `smali` as the folded literal of an addInstructions call."""
    escaped = smali.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")
    gap = "            // a comment the compiler ignores\n" if comment else ""
    src = (
        'val p = bytecodePatch(n = "x") {\n'
        "    execute {\n"
        "        addInstructions(0,\n"
        '            "const/4 v0, 0\\n" +\n'
        + gap
        + f'            "{escaped}"\n'
        "        )\n"
        "    }\n"
        "}\n"
    )
    path = Path(tempfile.mkstemp(suffix=".kt")[1])
    try:
        path.write_text(src)
        return bool(checks.check_branch_joins(path))
    finally:
        path.unlink(missing_ok=True)


def main() -> int:
    failures = 0
    for label, instruction, expected in CASES:
        got = flagged(instruction)
        ok = got == expected
        failures += not ok
        print(f"  {'ok  ' if ok else 'BAD '} {label:46s} flagged={got} expected={expected}")
    total = len(CASES)
    for label, smali, expected in BRANCH_CASES:
        got = join_flagged(smali, comment="comment" in label)
        ok = got == expected
        failures += not ok
        total += 1
        print(f"  {'ok  ' if ok else 'BAD '} {label:46s} flagged={got} expected={expected}")
    for label, instruction, expected in COMMENT_CASES:
        got = comment_flagged(instruction)
        ok = got == expected
        failures += not ok
        total += 1
        print(f"  {'ok  ' if ok else 'BAD '} {label:46s} flagged={got} expected={expected}")
    for label, src, expected in DOLLAR_CASES:
        got = dollar_flagged(src)
        ok = got == expected
        failures += not ok
        total += 1
        print(f"  {'ok  ' if ok else 'BAD '} {label:46s} flagged={got} expected={expected}")
    print()
    print(f"{total - failures}/{total} cases pass" if not failures
          else f"{failures} case(s) wrong")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
