#!/usr/bin/env python3
"""Fail-closed checks that the shipped product surface stays an information tool.

Covers Issue #52, guarding the `MVP_REPOSITIONING_2026_08_18` /
`LLM_INFORM_WITH_MANDATORY_NOTICE` invariants (`AGENTS.md`, `docs/product/mvp-redesign.md`):
the app renders no safety verdicts, so neither the shared UI nor the store listing may say one.

(a) Every `Text(...)` call under the shared UI package must source its string from
    `ProductCopy` (or a variable derived from user/domain data). A hardcoded string literal
    passed straight to `Text(` bypasses the single source of user-facing copy. A small,
    default-deny allowlist keyed by the literal's exact text covers the few legitimate
    exceptions that are not localized sentence copy (a brand wordmark, live-data readouts).

(b) `docs/product/store-listing.md` must carry none of the verdict vocabulary
    `shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/ui/UserFacingLanguageTest.kt` already
    bans from the shared UI. The banned set is mirrored here so the two surfaces cannot drift
    apart silently. A line-content allowlist covers the listing's own documentation of words it
    must never claim (quoting a banned word to disclaim it is not making the claim).

This validator is deliberately a second, independent implementation of the vocabulary ban the
Kotlin test enforces on the shared UI; a single implementation checking itself proves only that
it is self-consistent.
"""

from __future__ import annotations

import re
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UI_DIR = "shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/ui"
STORE_LISTING = "docs/product/store-listing.md"


class ValidationError(RuntimeError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


# ---------------------------------------------------------------------------------------------
# (a) Text() literals that bypass ProductCopy
# ---------------------------------------------------------------------------------------------

# Matches `Text(` (optionally spanning lines, optionally with the named `text =` argument)
# followed directly by a string literal — the shape every hardcoded Compose copy string has in
# this codebase, whichever of the two call styles the file already uses.
TEXT_LITERAL = re.compile(r'\bText\(\s*(?:text\s*=\s*)?"((?:[^"\\]|\\.)*)"')

# Literal Text() arguments that legitimately bypass ProductCopy, keyed by their exact source
# text. Default-deny: nothing is exempt unless listed here, so a new hardcoded string always
# fails the scan until someone consciously adds and justifies an entry.
TEXT_LITERAL_ALLOWLIST: dict[str, str] = {
    "GYM COME TRUE": "brand wordmark, invariant across locales — not localized sentence copy",
    "$platformName · local-first": (
        "dynamic platform name plus a static suffix — not localized sentence copy"
    ),
    "${activation.muscle.uppercase()}  ${activation.intensity}/10": (
        "muscle-intensity readout composed from live data — not localized sentence copy"
    ),
}


def scan_text_literals(ui_dir: Path, root: Path) -> list[str]:
    violations: list[str] = []
    for kt_file in sorted(ui_dir.rglob("*.kt")):
        text = kt_file.read_text(encoding="utf-8")
        for match in TEXT_LITERAL.finditer(text):
            literal = match.group(1)
            if literal in TEXT_LITERAL_ALLOWLIST:
                continue
            line_no = text.count("\n", 0, match.start()) + 1
            try:
                rel = kt_file.relative_to(root)
            except ValueError:
                rel = kt_file
            violations.append(f'{rel}:{line_no}: Text("{literal}") bypasses ProductCopy')
    return violations


def validate_product_copy_boundary() -> None:
    ui_dir = ROOT / UI_DIR
    require(ui_dir.is_dir(), f"Missing UI directory: {UI_DIR}")
    violations = scan_text_literals(ui_dir, ROOT)
    require(
        not violations,
        "Hardcoded Text() literal(s) bypass ProductCopy:\n" + "\n".join(violations),
    )


# ---------------------------------------------------------------------------------------------
# (b) Banned verdict vocabulary in the store listing
# ---------------------------------------------------------------------------------------------

# Mirrors UserFacingLanguageTest.bannedLatinWords exactly.
BANNED_LATIN_WORDS: tuple[str, ...] = (
    "safe",
    "unsafe",
    "approved",
    "approval",
    "block",
    "blocked",
    "blocks",
    "cleared",
    "clearance",
    "verdict",
    "forbidden",
    "prohibited",
)

# Mirrors UserFacingLanguageTest.bannedHanTerms exactly.
BANNED_HAN_TERMS: tuple[str, ...] = ("安全", "核准", "批准", "禁止", "阻擋", "通過審核")

# Lines whose full stripped text is a documented, legitimate use of banned vocabulary — e.g.
# quoting the words the listing must never claim about a product/dose/combination, or (should it
# ever recur) a banned Han substring caught inside unrelated wording such as the medical-risk
# notice's "...醫療專業人員" context. Matched on exact line content so an unrelated future edit
# reusing these words elsewhere still fails closed instead of riding the same allowlist entry.
STORE_LISTING_LINE_ALLOWLIST: set[str] = {
    '- no "safe", "approved", or "cleared" claim about any product, dose, or combination;',
}


def verdict_vocabulary_in(text: str) -> list[str]:
    latin = [
        word
        for word in BANNED_LATIN_WORDS
        if re.search(r"\b" + word + r"\b", text, re.IGNORECASE)
    ]
    han = [term for term in BANNED_HAN_TERMS if term in text]
    return latin + han


def scan_store_listing(path: Path, root: Path) -> list[str]:
    violations: list[str] = []
    try:
        rel = path.relative_to(root)
    except ValueError:
        rel = path
    for line_no, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if line.strip() in STORE_LISTING_LINE_ALLOWLIST:
            continue
        hits = verdict_vocabulary_in(line)
        if hits:
            violations.append(f"{rel}:{line_no}: {hits} in {line.strip()!r}")
    return violations


def validate_store_listing_language() -> None:
    path = ROOT / STORE_LISTING
    require(path.is_file(), f"Missing store listing: {STORE_LISTING}")
    violations = scan_store_listing(path, ROOT)
    require(
        not violations,
        "Store listing carries banned verdict vocabulary:\n" + "\n".join(violations),
    )


# ---------------------------------------------------------------------------------------------
# --selftest: plant both defect classes in temp copies and assert the scan goes red
# ---------------------------------------------------------------------------------------------


def selftest() -> int:
    failures: list[str] = []

    # Defect class (a): a hardcoded Text() literal with no allowlist entry.
    with tempfile.TemporaryDirectory() as tmp:
        tmp_ui = Path(tmp) / "ui"
        tmp_ui.mkdir()
        (tmp_ui / "Planted.kt").write_text(
            "package dev.ed3c.gymcometrue.ui\n\n"
            "@Composable\n"
            "fun Planted() {\n"
            '    Text(\n        text = "這個組合對你是安全的",\n    )\n'
            "}\n",
            encoding="utf-8",
        )
        violations = scan_text_literals(tmp_ui, Path(tmp))
        if not violations:
            failures.append("(a) planted hardcoded Text() literal was not detected")

    # Defect class (b): a banned verdict word planted into a copy of the store listing.
    with tempfile.TemporaryDirectory() as tmp:
        planted = Path(tmp) / "store-listing.md"
        original = (ROOT / STORE_LISTING).read_text(encoding="utf-8")
        planted.write_text(original + "\nThis stack is safe for you.\n", encoding="utf-8")
        violations = scan_store_listing(planted, Path(tmp))
        if not any("safe" in v for v in violations):
            failures.append("(b) planted banned-verdict sentence was not detected")

    if failures:
        for failure in failures:
            print(f"SELFTEST FAIL {failure}", file=sys.stderr)
        return 1
    print("SELFTEST PASS: both planted defect classes were detected")
    return 0


def main() -> int:
    if "--selftest" in sys.argv[1:]:
        return selftest()

    checks = (
        validate_product_copy_boundary,
        validate_store_listing_language,
    )
    try:
        for check in checks:
            check()
            print(f"PASS {check.__name__}")
    except ValidationError as error:
        print(f"FAIL {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
