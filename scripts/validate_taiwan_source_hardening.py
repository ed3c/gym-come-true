#!/usr/bin/env python3
"""Runtime and contract checks for Phase 8B source-capture hardening."""

from __future__ import annotations

import hashlib
import json
import os
import re
import stat
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import NoReturn

ROOT = Path(__file__).resolve().parents[1]
CAPTURE = ROOT / "scripts/capture_taiwan_source.py"
MAIN_KOTLIN = (
    ROOT
    / "shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/domain/TaiwanSourceLifecycle.kt"
)
TEST_KOTLIN = (
    ROOT
    / "shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/domain/TaiwanSourceLifecycleTest.kt"
)
HARDENING_TEST_KOTLIN = (
    ROOT
    / "shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/domain/TaiwanSourceLifecycleHardeningTest.kt"
)
DOC = ROOT / "docs/taiwan-source-capture-hardening.md"
SHA256 = re.compile(r"^[0-9a-f]{64}$")


class ValidationError(RuntimeError):
    """Raised when a hardening invariant is absent or executable behavior regresses."""


def fail(message: str) -> NoReturn:
    raise ValidationError(message)


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def run_capture(arguments: list[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, str(CAPTURE), *arguments],
        cwd=ROOT,
        text=True,
        capture_output=True,
        check=False,
    )


def base_arguments(
    source: Path,
    archive: Path,
    manifest: Path,
) -> list[str]:
    return [
        "--source-id",
        "synthetic-source-hardening",
        "--snapshot-id",
        "synthetic-snapshot-hardening",
        "--input",
        str(source),
        "--artifact-kind",
        "TEXT",
        "--canonical-url",
        "repo://synthetic/taiwan-source-hardening",
        "--captured-at",
        "2026-08-15",
        "--source-modified-at",
        "2026-08-15",
        "--media-type",
        "text/plain; charset=utf-8",
        "--license-id",
        "REPOSITORY_SYNTHETIC",
        "--attribution-text",
        "Repository-authored synthetic runtime fixture",
        "--note",
        "Runtime-only hardening fixture; not law, regulator guidance, or product data.",
        "--archive-root",
        str(archive),
        "--archive-uri-prefix",
        "evidence://test/taiwan-source-snapshots",
        "--manifest-out",
        str(manifest),
        "--synthetic",
    ]


def load_json(path: Path) -> dict[str, object]:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (FileNotFoundError, json.JSONDecodeError) as error:
        raise ValidationError(f"Cannot load JSON receipt {path}: {error}") from error
    require(isinstance(payload, dict), f"JSON receipt root must be an object: {path}")
    return payload


def validate_capture_round_trip() -> None:
    with tempfile.TemporaryDirectory(prefix="tw-source-hardening-") as temporary:
        root = Path(temporary)
        source = root / "source.txt"
        archive = root / "archive"
        manifest = root / "receipt.json"
        content = b"repository-authored source lifecycle hardening fixture\n"
        source.write_bytes(content)
        arguments = base_arguments(source, archive, manifest)

        first = run_capture(arguments)
        require(first.returncode == 0, f"First capture failed: {first.stderr or first.stdout}")
        summary = json.loads(first.stdout)
        receipt = load_json(manifest)
        expected_hash = hashlib.sha256(content).hexdigest()

        require(summary.get("sha256") == expected_hash, "Capture summary SHA-256 changed")
        require(receipt.get("sha256") == expected_hash, "Receipt SHA-256 does not bind source bytes")
        require(receipt.get("byteLength") == len(content), "Receipt byte length does not bind source bytes")
        require(receipt.get("state") == "HASH_VERIFIED", "Capture must stop at HASH_VERIFIED")
        require(receipt.get("productionUse") == "DENY", "Capture must never self-promote production use")
        require(receipt.get("legalReviewRef") is None, "Capture must not fabricate legal review")
        require(receipt.get("modelGenerated") is False, "Capture receipt must reject model authority")
        require(expected_hash in str(receipt.get("archiveUri")), "Archive URI is not content-addressed")

        archived = Path(str(summary.get("localArchive")))
        require(archived.is_file(), "Content-addressed local archive was not created")
        require(archived.read_bytes() == content, "Archived bytes changed after capture")
        mode = stat.S_IMODE(archived.stat().st_mode)
        require(mode & (stat.S_IWUSR | stat.S_IWGRP | stat.S_IWOTH) == 0, "Archived bytes remain writable")
        manifest_before = manifest.read_bytes()

        second = run_capture(arguments)
        require(second.returncode != 0, "Capture silently replaced an existing evidence receipt")
        require("manifest already exists" in second.stderr, "Overwrite refusal was not explicit")
        require(manifest.read_bytes() == manifest_before, "Rejected capture changed the existing receipt")

        replaced = run_capture([*arguments, "--replace-manifest"])
        require(replaced.returncode == 0, f"Explicit manifest replacement failed: {replaced.stderr}")
        require(load_json(manifest).get("sha256") == expected_hash, "Explicit replacement changed source identity")


def validate_capture_input_guards() -> None:
    with tempfile.TemporaryDirectory(prefix="tw-source-guards-") as temporary:
        root = Path(temporary)
        source = root / "source.txt"
        source.write_text("guard fixture\n", encoding="utf-8")

        synthetic = run_capture(base_arguments(source, root / "archive-a", root / "manifest-a.json"))
        require(synthetic.returncode == 0, "Synthetic repo:// fixture unexpectedly failed")

        live_arguments = base_arguments(source, root / "archive-b", root / "manifest-b.json")
        synthetic_index = live_arguments.index("--synthetic")
        del live_arguments[synthetic_index]
        canonical_index = live_arguments.index("--canonical-url") + 1
        live_arguments[canonical_index] = "http://insecure.example/source"
        live = run_capture(live_arguments)
        require(live.returncode != 0, "Non-synthetic HTTP source was admitted")
        require("must be HTTPS" in live.stderr, "HTTPS rejection reason is missing")

        too_large = run_capture(
            [
                *base_arguments(source, root / "archive-c", root / "manifest-c.json"),
                "--max-bytes",
                "1",
            ]
        )
        require(too_large.returncode != 0, "Maximum source size was not enforced")
        require("exceeds maximum" in too_large.stderr, "Maximum-size rejection reason is missing")

        empty = root / "empty.txt"
        empty.write_bytes(b"")
        empty_result = run_capture(base_arguments(empty, root / "archive-d", root / "manifest-d.json"))
        require(empty_result.returncode != 0, "Empty source artifact was admitted")

        if hasattr(os, "symlink"):
            link = root / "source-link.txt"
            link.symlink_to(source)
            link_result = run_capture(base_arguments(link, root / "archive-e", root / "manifest-e.json"))
            require(link_result.returncode != 0, "Symlink source was admitted")
            require("symlink" in link_result.stderr.lower(), "Symlink rejection reason is missing")

            real_archive = root / "real-archive"
            real_archive.mkdir()
            archive_link = root / "archive-link"
            archive_link.symlink_to(real_archive, target_is_directory=True)
            archive_result = run_capture(base_arguments(source, archive_link, root / "manifest-f.json"))
            require(archive_result.returncode != 0, "Symlink archive root was admitted")
            require("archive root cannot be a symlink" in archive_result.stderr, "Archive-root rejection reason is missing")


def validate_capture_source_contract() -> None:
    text = CAPTURE.read_text(encoding="utf-8")
    for token in (
        "O_NOFOLLOW",
        "DEFAULT_MAX_BYTES",
        "READ_ONLY_MODE",
        "archived bytes do not match the capture receipt",
        "refuse silent evidence-receipt replacement",
        "--replace-manifest",
        '"productionUse": "DENY"',
        '"legalReviewRef": None',
        '"modelGenerated": False',
    ):
        require(token in text, f"Missing capture hardening invariant: {token}")
    require("urllib" not in text and "requests" not in text, "Capture tool must not fetch network resources")


def validate_kotlin_hardening_contract() -> None:
    main_text = MAIN_KOTLIN.read_text(encoding="utf-8")
    existing_test = TEST_KOTLIN.read_text(encoding="utf-8")
    hardening_test = HARDENING_TEST_KOTLIN.read_text(encoding="utf-8")

    for token in (
        "Mapping sourceId does not match the referenced snapshot.",
        "Input manifest cannot self-declare production admission.",
        "Lifecycle event sequences must be unique.",
        "Lifecycle event sequences must be contiguous from 1.",
        "occurs after the as-of date.",
        "Rollback target must equal rollbackToVersion.",
        "productionAdmitted = production &&",
    ):
        require(token in main_text, f"Missing deterministic lifecycle invariant: {token}")

    total_tests = existing_test.count("@Test") + hardening_test.count("@Test")
    require(total_tests >= 19, f"Expected at least 19 lifecycle tests, found {total_tests}")
    for name in (
        "archiveUriMustContainTheArtifactHash",
        "exactMappingMustMatchTheSnapshotSourceIdentity",
        "testOnlyMappingCannotRemainDraft",
        "futureLifecycleEventIsNotApplied",
        "validRevocationCanRollBackToTheDeclaredVersion",
        "manifestCannotSelfDeclareProductionAdmission",
        "lifecycleSequencesMustBeUniqueAndContiguous",
    ):
        require(f"fun {name}()" in hardening_test, f"Missing hardening test: {name}")


def validate_hardening_document() -> None:
    text = DOC.read_text(encoding="utf-8").lower()
    for token in (
        "no network client",
        "HASH_VERIFIED + DENY",
        "content-addressed",
        "read-only",
        "--replace-manifest",
        "not WORM storage",
        "qualified review",
        "exact-head hosted checks",
    ):
        require(token.lower() in text, f"Hardening documentation is missing: {token}")


def main() -> int:
    checks = (
        validate_capture_round_trip,
        validate_capture_input_guards,
        validate_capture_source_contract,
        validate_kotlin_hardening_contract,
        validate_hardening_document,
    )
    try:
        for check in checks:
            check()
            print(f"PASS {check.__name__}")
    except (ValidationError, OSError, subprocess.SubprocessError, json.JSONDecodeError) as error:
        print(f"FAIL {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
