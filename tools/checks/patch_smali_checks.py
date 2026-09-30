"""Static checks for the patch sources, runnable without a device or an APK.

Two classes of defect have shipped five broken releases in a row, and neither is caught
by compiling, because both are correct Kotlin producing wrong smali:

1. **Invoke arity.** An inserted 35c invoke's register list must name the receiver plus
   every declared argument. Passing only the receiver assembles cleanly, compiles, and
   is rejected by the verifier at class-load time. `check_invoke_arity` derives the
   required register count from the target method's own descriptor.

2. **Helper call shape.** `replaceInstructions` removes as many instructions as the
   replacement list is long, so a hand-written `nop` block that is meant to erase one
   invoke silently deletes the instructions after it. `check_replace_instructions` flags
   every call whose replacement is longer than what the comment claims to replace.

Fingerprint resolution needs a pinned APK and is handled by `verify_fingerprints.py`.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

#: a descriptor like (Ljava/lang/String; I J)V, split into parameter types
DESC = re.compile(r"^\((.*)\)(.*)$")


def parse_descriptor(desc: str) -> tuple[list[str], str]:
    """Split a method descriptor into its parameter types and return type."""
    m = DESC.match(desc)
    if not m:
        raise ValueError(f"not a method descriptor: {desc!r}")
    params = m.group(1)
    out: list[str] = []
    i = 0
    while i < len(params):
        start = i
        while params[i] == "[":
            i += 1
        if params[i] == "L":
            i = params.index(";", i) + 1
        else:
            i += 1
        out.append(params[start:i])
    return out, m.group(2)


def is_reference(descriptor: str) -> bool:
    """True when a type is an object or array, i.e. occupies a register as a reference."""
    return descriptor.startswith("L") or descriptor.startswith("[")


def smali_registers(text: str) -> list[str]:
    """Registers named in a smali 35c/3rc register list, e.g. `{v5, v0}`."""
    m = re.search(r"\{([^}]*)\}", text)
    if not m:
        return []
    return [r.strip() for r in m.group(1).split(",") if r.strip()]


#: `invoke-virtual {v5, v0}, Landroid/view/View;->setVisibility(I)V`
INVOKE = re.compile(
    r"invoke-\S+\s*\{([^}]*)\}\s*,\s*"
    r"(L[^;]+;)->([^\s(]+)\(([^)]*)\)([^\s]*)"
)

#: a bare `invoke-kind v0, v0, L...;->m()V` with no register list
INVOKE_NO_LIST = re.compile(
    r"invoke-\S+\s+((?:v\d+\s*,\s*)*v\d+)\s*,\s*"
    r"(L[^;]+;)->([^\s(]+)\(([^)]*)\)([^\s]*)"
)


def string_concat_in(src: str) -> list[tuple[int, str]]:
    """Reassemble Kotlin string concatenations into the smali text they produce.

    A patch body builds its smali as `"const/16 v0, 0x8\n" + "invoke-virtual {v5}, ..."`,
    so the smali only exists after the concatenation is folded. Literal interpolation of
    a `const` is preserved verbatim, which is enough for arity because only the register
    list matters.
    """
    out: list[tuple[int, str]] = []
    for m in re.finditer(r'"((?:[^"\\]|\\.)*)"', src):
        pass
    # fold adjacent "..." + "..." chains
    chain = re.compile(r'"((?:[^"\\]|\\.)*)"(?:\s*\+\s*\\?\s*\n?\s*"((?:[^"\\]|\\.)*)")*')
    for m in re.finditer(r'"((?:[^"\\\n]|\\.)*)"\s*(?:\+\s*\n?\s*"((?:[^"\\\n]|\\.)*)")+', src):
        parts = re.findall(r'"((?:[^"\\\n]|\\.)*)"', m.group(0))
        if len(parts) > 1:
            out.append((m.start(), "".join(p.encode().decode("unicode_escape")
                                           for p in parts)))
    return out


def check_invoke_arity(path: Path, constants: dict[str, str] | None = None) -> list[str]:
    """Every inserted invoke must name the receiver plus each declared argument.

    Register names in the source are interpolated (`{v$receiver, $VISIBILITY_REGISTER}`),
    so the real count has to come from the *shape* of the list rather than from the
    literal text: a `$NAME` or `v$NAME` entry is one register whatever it expands to.
    That is enough for arity, which is a count and not a value.
    """
    problems: list[str] = []
    src = path.read_text(encoding="utf-8")
    for offset, smali in string_concat_in(src):
        # the folded text is one logical smali program; join before splitting so an
        # invoke whose reference sits on the next source line is still one instruction
        for line in smali.splitlines():
            line = line.strip()
            if not line.startswith("invoke-"):
                continue
            m = INVOKE.search(line) or INVOKE_NO_LIST.search(line)
            if not m:
                continue
            named = smali_registers(line)
            if not named:
                # bare form: count the comma separated registers before the reference
                named = [r.strip() for r in m.group(1).split(",") if r.strip()]
            params_text = m.group(4)
            params = [p for p in re.split(r"\s+", params_text.strip()) if p]
            wide = sum(1 for p in params if p in ("J", "D"))
            # a long or double occupies a register pair, so it is named once
            required = 1 + len(params) - wide
            if len(named) != required:
                problems.append(
                    f"{path.name}:{offset}: `{line[:64]}` names {len(named)} "
                    f"register(s) but {m.group(2)}->{m.group(3)} declares "
                    f"{len(params)} argument(s) and needs {required} "
                    f"(receiver + arguments)")
    return problems


def check_replace_instructions(path: Path) -> list[str]:
    """`replaceInstructions` deletes as many instructions as the list is long.

    A single 35c invoke is three code units, and padding it back out to three `nop`s
    keeps the method size identical -- which is exactly why the mistake is invisible.
    Width preservation is necessary but not sufficient: the helper also removes the two
    instructions that follow. Each of those shipped a launch crash.
    """
    problems: list[str] = []
    src = path.read_text(encoding="utf-8")
    for m in re.finditer(r"replaceInstructions\(", src):
        # take the balanced argument list so nested parens do not truncate the match
        i = m.end() - 1
        depth = 0
        for j in range(i, len(src)):
            if src[j] == "(":
                depth += 1
            elif src[j] == ")":
                depth -= 1
                if depth == 0:
                    break
        else:
            continue
        arg = src[i:j + 1]
        line = src[: m.start()].count("\n") + 1
        # the replacement is one or more string literals holding newline-separated smali,
        # so count instructions in the folded text rather than occurrences of a quote
        folded = "".join(re.findall(r'"((?:[^"\\\n]|\\.)*)"', arg))
        nops = len([w for w in re.split(r"\\n|\s+", folded) if w == "nop"])
        if nops > 1:
            problems.append(
                f"{path.name}:{line}: replaceInstructions with a {nops}-instruction "
                f"replacement also removes the {nops - 1} instruction(s) after the "
                f"target. To erase one invoke while keeping the width, use "
                f"removeInstruction(index) followed by addInstructions(index, nops).")
    return problems


def main() -> int:
    root = Path(__file__).resolve().parents[2] / "patches/src/main/kotlin"
    if not root.is_dir():
        print(f"no patch sources at {root}")
        return 0
    problems: list[str] = []
    for path in sorted(root.rglob("*.kt")):
        problems += check_invoke_arity(path)
        problems += check_replace_instructions(path)
    for p in problems:
        print("  FAIL", p)
    print(f"checked {len(list(root.rglob('*.kt')))} file(s): "
          f"{len(problems)} problem(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
