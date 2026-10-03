#!/usr/bin/env python3
"""Parse every smali block that a patch injects, using the patcher's own smali library.

Kotlin compiling is not evidence that injected smali is valid. `addInstructions` hands the
text to an ANTLR grammar at patch time, on the device, and a malformed block only surfaces
as a PatchException after the whole bundle has been assembled. That is how a `->member Type`
field reference, which is exactly what baksmali prints and what every disassembly listing
shows, reached a released build and threw `missing COLON` in the field.

So this walks the patch sources, pulls out each triple-quoted smali block passed to
`addInstructions` or `addInstructionsWithLabels`, substitutes the file's `private const val`
string constants, wraps the result in a stub method with the register count and free
registers the real target method has, and hands it to `com.android.tools.smali`. The same
`SmaliTestUtils.compileSmali` entry point the patcher uses.

The register layout is the part worth keeping honest: a block that parses can still be wrong
because it clobbers a live register. Each stub below declares the same `.registers` count and
parameter count as the method the block is actually injected into, so the assembler rejects
writes to a parameter register and out-of-range registers.

Requires the smali library jars. Point SMALI_CP at them, or leave it unset to auto-discover:

    export SMALI_CP=/path/to/jars/*

Usage: check_inline_smali.py [--verbose]
"""

from __future__ import annotations

import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PATCHES = ROOT / "patches/src/main/kotlin/app/pinterest/patches"

# How to wrap each block so the assembler sees the real target method's register layout.
# Keyed by the Kotlin file that contains the block.
#
#   registers:  total .registers for the method
#   ins:        declared .registers minus parameters; the stub must have exactly this many
#               leading NOPs so that `v<ins>` is the first free register
#   comment:    which method is being patched, and what is live where
LAYOUTS = {
    "navigation/HideSearchNavButtonPatch.kt": {
        "signature": "(Lae0/o; I Lf82/l; Lf82/j; Lf82/n;)V",
        "registers": 8,
        "ins": 2,
        "live": "v2=this v3=descriptor v4=int v5,l v6=tab View v7=listener; v0,v1 free",
        "method": "FloatingBottomNavBar.Q1",
    },
    "navigation/HideNotificationsNavButtonPatch.kt": {
        "signature": "(Lae0/o; I Lf82/l; Lf82/j; Lf82/n;)V",
        "registers": 8,
        "ins": 2,
        "live": "v2=this v3=descriptor v4=int v5,l v6=tab View v7=listener; v0,v1 free",
        "method": "FloatingBottomNavBar.Q1",
    },
    "comments/HideCommentsPatch.kt": {
        "signature": "(Landroid/content/Context;Landroid/util/AttributeSet;I)V",
        "registers": 9,
        "ins": 5,
        # v5=this v6=Context(reused as the wrapper by ins 25) v7=attrs v8=int; v0..v4 free.
        "live": "v5=this v6=comments wrapper v7=attrs v8=int; v0..v4 free",
        "method": "UnifiedPinActionBarView.<init>",
    },
}

# private const val NAME = "value"  /  = 0x8  (unquoted numeric constants too)
CONST_RE = re.compile(r'private const val (\w+) = "([^"]*)"')
HEX_CONST_RE = re.compile(r"private const val (\w+) = (0x[0-9a-fA-F]+)")
BLOCK_RE = re.compile(
    r"addInstructionsWithLabels\(\s*\n?\s*[^,]+,\s*\n?\s*\"\"\"(.*?)\"\"\"",
    re.DOTALL,
)
PLAIN_BLOCK_RE = re.compile(
    r"addInstructions\(\s*\n?\s*[^,]+,\s*\n?\s*(?:\"\"\"|\")",
)


def find_classpath() -> str | None:
    env = os.environ.get("SMALI_CP")
    if env:
        return env
    candidates = [
        Path.home() / ".gradle/caches/smali",
        Path("/data/data/com.termux/files/usr/tmp/opencode/jars"),
        Path.home() / "apks/_tools/smali",
    ]
    for directory in candidates:
        if (directory / "smali.jar").exists():
            return str(directory / "*")
    return None


def collect_constants(text: str) -> dict[str, str]:
    values = {name: value for name, value in CONST_RE.findall(text)}
    values.update({name: value for name, value in HEX_CONST_RE.findall(text)})
    return values


def wrap(body: str, layout: dict) -> str:
    """Wrap an instruction block in a method with the target's real register layout."""
    filler = "\n".join(["    nop"] * layout["ins"])
    return (
        ".class Ltest/Inline;\n"
        ".super Ljava/lang/Object;\n"
        "\n"
        f".method public stub{layout['signature']}\n"
        f"    .registers {layout['registers']}\n"
        f"{filler}\n"
        f"{body}\n"
        "    return-void\n"
        ".end method\n"
    )


def main() -> int:
    verbose = "--verbose" in sys.argv
    classpath = find_classpath()
    if not classpath is None and not Path(
        classpath.replace("*", "smali.jar")
    ).exists():
        classpath = None
    if classpath is None:
        print("check_inline_smali: SKIPPED - smali library jars not found.")
        print("  Set SMALI_CP to a directory of jars, or install the smali library.")
        print("  Without this check, injected smali is only validated at patch time on device.")
        return 0

    probe = ROOT / "tools/checks/InlineSmaliProbe.java"
    if not probe.exists():
        print(f"check_inline_smali: FAIL - missing probe {probe}")
        return 1

    with tempfile.TemporaryDirectory() as tmp:
        probe_class = Path(tmp) / "probe"
        probe_class.mkdir()
        compiled = subprocess.run(
            ["javac", "-cp", classpath, "-d", str(probe_class), str(probe)],
            capture_output=True,
            text=True,
        )
        if compiled.returncode != 0:
            print("check_inline_smali: FAIL - could not build the probe")
            print(compiled.stderr.strip()[:2000])
            return 1

        files = 0
        blocks = 0
        failures = 0

        for relative, layout in sorted(LAYOUTS.items()):
            source = PATCHES / relative
            if not source.exists():
                print(f"  MISSING  {relative}")
                failures += 1
                continue
            files += 1
            text = source.read_text(encoding="utf-8")
            constants = collect_constants(text)

            for raw in BLOCK_RE.findall(text):
                blocks += 1
                body = raw.strip("\n")
                for name, value in constants.items():
                    body = body.replace(f"${name}", value)
                leftover = re.findall(r"\$[A-Za-z_]\w*", body)
                stub = wrap("\n".join("    " + l for l in body.splitlines()), layout)

                smali_file = Path(tmp) / "Inline.smali"
                smali_file.write_text(stub, encoding="utf-8")

                result = subprocess.run(
                    ["java", "-cp", f"{classpath}:{probe_class}", "InlineSmaliProbe"],
                    input=stub,
                    capture_output=True,
                    text=True,
                )
                output = (result.stdout + result.stderr).strip()
                ok = "PARSE OK" in output

                if leftover and ok:
                    print(f"  WARN     {relative}: unsubstituted ${{{leftover[0]}}} in block")
                if not ok:
                    failures += 1
                    print(f"  FAIL     {relative} -> {layout['method']}")
                    for line in output.splitlines():
                        print(f"             {line}")
                elif verbose:
                    print(f"  ok       {relative} -> {layout['method']}")
                    print(f"             {layout['live']}")

        print(f"checked {files} file(s), {blocks} block(s): {failures} failure(s)")
        return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())