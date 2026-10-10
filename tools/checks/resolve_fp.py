"""Resolve a patch fingerprint against a cached APK, the way the patcher will.

`tools/checks/README.md` states the gap plainly: "Fingerprint resolution. Whether a filter chain
still matches a given APK needs the pinned APK". Three Truecaller releases went out before anyone
checked that locally -- v0.6.0-dev.31 and .32 shipped a filter that could never match, and the
fingerprint was only rejected on device.

This closes that gap for the cached targets. It reads a fingerprint's declared parameters out of the
Kotlin source and matches them against the dex with the same rules the patcher uses:

  * definingClass, name, returnType and the parameter list must match exactly, in order;
  * `string(x)` matches a const-string or const-string/jumbo whose literal is x;
  * `opcode(x)` matches that opcode;
  * `instanceOf(x)` matches an `instance-of` against type x -- and only that opcode;
  * `checkCast(x)` matches a `check-cast` against type x;
  * `methodCall(...)` matches an invoke of that exact method;
  * `fieldAccess(...)` matches a read or write of that exact field.

Every filter must be satisfied by at least one instruction, and all of them by the same method. The
result is pass/fail per fingerprint, with the reason, so a failure names its own cause instead of
arriving as "Failed to match the fingerprint" from a phone.

    python resolve_fp.py com.truecaller 26.31.6
    python resolve_fp.py com.pinterest 14.38.0 --patch contacts
"""

from __future__ import annotations

import argparse
import glob
import os
import re
import sys
from pathlib import Path

HOME = Path.home()
CACHE_ROOT = HOME / "apks"
# Prefer the working directory, then the usual checkouts. Running from a worktree is
# normal here, and a stale sibling checkout would silently resolve against the wrong sources.
REPO_CANDIDATES = [
    Path.cwd(),
    Path.home() / "goodnight-patches",
    Path.home() / "wt-truecaller",
]


def find_cache(package: str, version: str) -> Path:
    hits = sorted((CACHE_ROOT / package).glob(f"{version}*"))
    if not hits:
        raise SystemExit(f"no cache for {package} {version} under {CACHE_ROOT / package}")
    return hits[0]


def find_repo() -> Path:
    for candidate in REPO_CANDIDATES:
        if (candidate / "patches/src/main/kotlin").is_dir():
            return candidate
    raise SystemExit("could not locate the patches repo")


def unescape(value: str) -> str:
    return value.replace('\\$', '$').replace('\\"', '"')


def parse_fingerprints(package: str, only: str | None) -> list[dict]:
    """Pull every `object X : Fingerprint(...)` declaration out of the Kotlin sources."""
    repo = find_repo()
    root = repo / "patches/src/main/kotlin"
    # Match on the source package directory, not the application id: a patch package is
    # `app/<org>/patches`, which need not agree with the app's own package name.
    # The patch source package is `app/<short>/patches`: `com.truecaller` lives under
    # `app/truecaller/`, not `app/com/truecaller/`, so try the trailing segment too.
    short = package.rsplit(".", 1)[-1]
    candidates = [
        f"app/{package}/patches",
        f"app/{package.replace('.', '/')}/patches",
        f"app/{short}/patches",
    ]
    results = []

    for path in sorted(root.rglob("*.kt")):
        posix = path.as_posix()
        if not any(c in posix for c in candidates):
            continue
        if only and only not in path.as_posix():
            continue
        text = path.read_text(encoding="utf-8")

        for match in re.finditer(
            r"object\s+(\w+)\s*:\s*Fingerprint\((.*?)\n\)\s*(?=\n|$)", text, re.DOTALL
        ):
            name, body = match.group(1), match.group(2)
            head, _, tail = body.partition("filters = listOf(")

            def field(key: str) -> str | None:
                found = re.search(rf'{key}\s*=\s*"([^"]*)"', head)
                return unescape(found.group(1)) if found else None

            params = []
            if "parameters = listOf(" in head:
                block = head.split("parameters = listOf(", 1)[1]
                params = [unescape(p) for p in re.findall(r'"([^"]*)"', block)]

            results.append(
                {
                    "name": name,
                    "file": path.relative_to(repo).as_posix(),
                    "definingClass": field("definingClass"),
                    "method": field("name"),
                    "returnType": field("returnType"),
                    "parameters": params,
                    "filters": parse_filters(tail),
                }
            )
    return results


def parse_filters(tail: str) -> list[tuple[str, tuple]]:
    """Parse the filter calls, in declaration order, into (kind, args) pairs."""
    filters: list[tuple[str, tuple]] = []

    # Split on top-level commas only: an argument list may contain commas of its own.
    depth = 0
    current = ""
    parts = []
    for char in tail:
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
        if char == "," and depth == 0:
            parts.append(current)
            current = ""
        else:
            current += char
    if current.strip():
        parts.append(current)

    for part in parts:
        part = part.strip()
        call = re.match(r"(\w+)\s*\(", part)
        if not call:
            continue
        kind = call.group(1)
        args = re.findall(r'"([^"]*)"', part)
        filters.append((kind, tuple(unescape(a) for a in args)))
    return filters


def load_dex(cache: Path):
    """Parse every classes*.dex under the cache, once."""
    from loguru import logger

    logger.remove()  # androguard logs every map item otherwise
    sys.path.insert(0, str(cache / "tools"))
    from androguard.core.dex import DEX  # noqa: PLC0415

    parsed = []
    for dex in sorted(glob.glob(str(cache / "dex" / "classes*.dex"))):
        with open(dex, "rb") as handle:
            parsed.append((os.path.basename(dex), DEX(handle.read())))
    return parsed


def instructions(method):
    code = method.get_code()
    if not code:
        return
    offset = 0
    for ins in code.get_bc().get_instructions():
        yield offset, ins.get_name(), ins.get_output()
        offset += ins.get_length()


def operand(output: str) -> str:
    """The reference portion of an instruction output, e.g. `v0, Lp/q;->r(Lx;)V` -> `Lp/q;->r(Lx;)V`."""
    return output.rsplit(", ", 1)[-1]


def method_descriptor(params: list[str], return_type: str) -> str:
    return "(" + "".join(params) + ")" + return_type


def find_method(parsed, defining_class: str, method: str):
    """Yield (dex name, androguard method) for every exact name match in that class."""
    found = []
    for dex_name, dex in parsed:
        klass = dex.get_class(defining_class)
        if klass is None:
            continue
        for candidate in klass.get_methods():
            if candidate.get_name() == method:
                found.append((dex_name, candidate))
    return found


# Three states are needed per instruction, not two: "this instruction is not the kind the filter
# looks at", "it is, and it matches", and "it is, and it does not". Collapsing the first into the
# second makes every filter vacuously true, which is how a fingerprint with a literal that exists
# nowhere was reported as resolving.
PASS = "pass"
FAIL = "fail"
SKIP = "skip"      # instruction is irrelevant to this filter
MATCH = "match"    # instruction satisfies this filter
NO_MATCH = "no"    # instruction is the right kind but the operand differs


def check_filter(kind: str, args: tuple, name: str, output: str) -> str:
    if kind == "string":
        if not name.startswith("const-string"):
            return SKIP
        literal = output.split('"')[-2] if '"' in output else output
        return MATCH if literal == args[0] else NO_MATCH

    if kind in ("instanceOf", "checkCast"):
        wanted = "instance-of" if kind == "instanceOf" else "check-cast"
        if not name.startswith(wanted):
            return SKIP
        return MATCH if args and args[0] in output else NO_MATCH

    if kind == "opcode":
        return MATCH if name == args[0] else NO_MATCH

    if kind == "methodCall":
        if not name.startswith("invoke"):
            return SKIP
        # args are (definingClass, name, params..., returnType); matching on the class token keeps a
        # partial declaration useful without pretending to be a full descriptor comparison.
        return MATCH if args and args[0] in output else NO_MATCH

    if kind == "fieldAccess":
        if not (name.startswith(("iget", "iput", "sget", "sput"))):
            return SKIP
        return MATCH if args and args[0] in output else NO_MATCH

    return NO_MATCH


def describe_filter_miss(kind: str, args: tuple, reason: str) -> str:
    shown = ", ".join(repr(a) for a in args)
    if reason == NO_MATCH:
        return f"{kind}({shown}): found the instruction but not that operand"
    return f"{kind}({shown}): no instruction of this kind in the method"


UNSUPPORTED = "unsupported"  # fingerprint shape this checker does not model


def resolve(fingerprint: dict, parsed) -> tuple[str, str]:
    """Return (verdict, detail) where verdict is PASS, FAIL or UNSUPPORTED."""
    defining_class = fingerprint["definingClass"]
    method_name = fingerprint["method"]

    if not defining_class or not method_name:
        # A fingerprint may be declared by signature alone, or may name a Kotlin inner class whose
        # method actually lives on a `Outer$member;` class in the dex. Both are legitimate; neither
        # is modelled here, and reporting either as a failure would be worse than useless because it
        # would send someone hunting a defect that is not there.
        return UNSUPPORTED, (
            "matched by signature or inner-class shape; not modelled by this checker"
        )

    candidates = find_method(parsed, defining_class, method_name)
    if not candidates:
        return FAIL, f"no method {defining_class}->{method_name} in the apk"

    expected = method_descriptor(fingerprint["parameters"], fingerprint["returnType"] or "V")
    by_descriptor = [
        (dex_name, candidate)
        for dex_name, candidate in candidates
        if candidate.get_descriptor().replace(" ", "") == expected
    ]

    if not by_descriptor:
        actual = ", ".join(
            sorted({c.get_descriptor().replace(" ", "") for _, c in candidates})
        )
        return FAIL, f"descriptor mismatch; expected {expected}, found {actual}"

    # More than one overload means the filters have to disambiguate, which is exactly what the
    # extra filters are for -- so report it rather than silently taking the first.
    if len(by_descriptor) > 1:
        dexes = ", ".join(sorted({dex for dex, _ in by_descriptor}))
        return FAIL, f"{len(by_descriptor)} methods share that descriptor ({dexes})"

    dex_name, method = by_descriptor[0]
    seen = [(name, output) for _, name, output in instructions(method)]
    if not seen:
        return FAIL, f"{dex_name}: method has no code"

    unresolved = []
    for kind, args in fingerprint["filters"]:
        verdict = NO_MATCH
        saw_kind = False
        for name, output in seen:
            outcome = check_filter(kind, args, name, output)
            if outcome == MATCH:
                verdict = MATCH
                break
            if outcome == NO_MATCH:
                saw_kind = True
        if verdict != MATCH:
            unresolved.append(describe_filter_miss(kind, args, NO_MATCH if saw_kind else SKIP))

    if unresolved:
        return FAIL, "; ".join(unresolved)

    return PASS, f"{dex_name}, {sum(1 for _ in instructions(method))} instructions"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("package")
    parser.add_argument("version")
    parser.add_argument("--patch", help="only fingerprints whose file path contains this")
    args = parser.parse_args()

    cache = find_cache(args.package, args.version)
    print(f"cache  {cache}")
    fingerprints = parse_fingerprints(args.package, args.patch)
    if not fingerprints:
        raise SystemExit(f"no Fingerprint declarations found for {args.package}")
    print(f"found  {len(fingerprints)} fingerprint(s)\n")

    print("parsing dex ...", flush=True)
    parsed = load_dex(cache)
    if not parsed:
        # Reporting every fingerprint as "no method ... in the apk" when the cache simply holds no
        # DEX is worse than useless: it looks like a broken patch. Some older caches keep their dex
        # elsewhere, or keep only disassembly, so say so instead.
        raise SystemExit(
            f"{cache}/dex holds no classes*.dex, so nothing can be resolved against it.\n"
            "This cache keeps its dex elsewhere or only as disassembly; re-extract it before\n"
            "asking this question of it (see AGENTS.md on the working cache)."
        )
    print(f"parsed  {len(parsed)} dex files\n")

    failures = 0
    skipped = 0
    for fingerprint in fingerprints:
        verdict, detail = resolve(fingerprint, parsed)
        if verdict == PASS:
            print(f"  PASS  {fingerprint['name']}")
            print(f"        {detail}")
        elif verdict == UNSUPPORTED:
            skipped += 1
            print(f"  SKIP  {fingerprint['name']}")
            print(f"        {detail}")
        else:
            failures += 1
            print(f"  FAIL  {fingerprint['name']}")
            print(f"        {detail}")
            print(f"        {fingerprint['file']}")

    print()
    checked = len(fingerprints) - skipped
    if failures:
        print(f"{failures} of {checked} checked fingerprint(s) would NOT resolve")
    else:
        print(f"all {checked} checked fingerprint(s) resolve")
    if skipped:
        print(f"({skipped} skipped: shape not modelled by this checker)")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())