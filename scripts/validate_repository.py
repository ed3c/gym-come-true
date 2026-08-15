#!/usr/bin/env python3
"""Fail-closed repository checks using only the Python standard library."""

from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]

FORBIDDEN_HOTLINKS = {
    "static.exercisedb.dev",
    "v1.exercisedb.io",
    "v2.exercisedb.io",
}
FORBIDDEN_MEDIA_SUFFIXES = {
    ".gif",
    ".mp4",
    ".mov",
    ".webm",
    ".glb",
    ".gltf",
    ".fbx",
    ".obj",
}
SECRET_PATTERNS = {
    "OpenAI-style secret": re.compile(r"\bsk-[A-Za-z0-9_-]{20,}\b"),
    "Google API key": re.compile(r"\bAIza[0-9A-Za-z_-]{20,}\b"),
    "private key": re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    "hard-coded provider key": re.compile(
        r"(?im)^\s*(?:OPENAI|ANTHROPIC|GEMINI|GOOGLE)_API_KEY\s*=\s*[^$\s][^\s]*"
    ),
}


class ValidationError(RuntimeError):
    pass


def load_json(path: str) -> Any:
    target = ROOT / path
    try:
        return json.loads(target.read_text(encoding="utf-8"))
    except FileNotFoundError as error:
        raise ValidationError(f"Missing required file: {path}") from error
    except json.JSONDecodeError as error:
        raise ValidationError(f"Invalid JSON in {path}: {error}") from error


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


def validate_source_registry() -> None:
    registry = load_json("legal/source-registry.json")
    require(registry.get("defaultPolicy") == "DENY", "Source registry must default to DENY")
    sources = registry.get("sources")
    require(isinstance(sources, list) and sources, "Source registry must contain reviewed records")

    ids: set[str] = set()
    for source in sources:
        source_id = source.get("id")
        require(isinstance(source_id, str) and source_id, "Every source needs an id")
        require(source_id not in ids, f"Duplicate source id: {source_id}")
        ids.add(source_id)
        require(source.get("status") in {"ALLOW", "REVIEW", "DENY"}, f"Invalid status: {source_id}")
        require(bool(source.get("scope")), f"Missing scope: {source_id}")
        require(bool(source.get("evidence")), f"Missing evidence requirement: {source_id}")


def validate_media_registry() -> None:
    registry = load_json("legal/media-registry.json")
    require(registry.get("defaultPolicy") == "DENY", "Media registry must default to DENY")
    required = set(registry.get("requiredAllowFields", []))
    require("sha256" in required and "licenseEvidenceRef" in required, "ALLOW contract is incomplete")

    for asset in registry.get("assets", []):
        require(asset.get("status") in {"ALLOW", "REVIEW", "DENY"}, "Invalid media status")
        if asset.get("status") != "ALLOW":
            continue
        missing = [field for field in required if not asset.get(field)]
        require(not missing, f"ALLOW asset {asset.get('id')} is missing {missing}")
        sha256 = asset["sha256"]
        require(bool(re.fullmatch(r"[0-9a-f]{64}", sha256)), f"Invalid SHA-256 for {asset.get('id')}")


def validate_seed_data() -> None:
    seed = load_json("data/seed/exercises.example.json")
    provenance = seed.get("provenance", {})
    require(provenance.get("sourceId") == "original-example-seed", "Seed must be repository-original")
    require(provenance.get("mediaIncluded") is False, "Seed must not claim bundled media")

    exercises = seed.get("exercises", [])
    require(isinstance(exercises, list) and exercises, "Seed exercises are missing")
    for exercise in exercises:
        require(exercise.get("mediaRef") is None, f"Example {exercise.get('id')} unexpectedly references media")
        require(exercise.get("medicalClaim") is False, f"Example {exercise.get('id')} contains a medical claim")


def iter_admitted_text_files() -> list[Path]:
    roots = [ROOT / "shared", ROOT / "androidApp", ROOT / "webApp", ROOT / "data"]
    paths: list[Path] = []
    for base in roots:
        if not base.exists():
            continue
        for path in base.rglob("*"):
            if path.is_file() and path.suffix.lower() in {
                ".kt", ".kts", ".xml", ".json", ".html", ".pro"
            }:
                paths.append(path)

    project_spec = (ROOT / "iosApp/project.yml").read_text(encoding="utf-8")
    for relative in (
        "iosApp/GymComeTrue/GymComeTrueApp.swift",
        "iosApp/GymComeTrue/ContentView.swift",
        "iosApp/GymComeTrue/NativeCapabilityBridge.swift",
    ):
        require(Path(relative).name in project_spec, f"Canonical iOS spec omits {relative}")
        paths.append(ROOT / relative)
    return paths


def validate_no_secrets_or_hotlinks() -> None:
    failures: list[str] = []
    for path in iter_admitted_text_files():
        text = path.read_text(encoding="utf-8")
        relative = path.relative_to(ROOT)
        for host in FORBIDDEN_HOTLINKS:
            if host in text:
                failures.append(f"forbidden hotlink {host} in {relative}")
        for label, pattern in SECRET_PATTERNS.items():
            if pattern.search(text):
                failures.append(f"{label} in {relative}")
    require(not failures, "; ".join(failures))


def validate_no_unregistered_binary_media() -> None:
    offenders = [
        path.relative_to(ROOT)
        for path in ROOT.rglob("*")
        if path.is_file()
        and path.suffix.lower() in FORBIDDEN_MEDIA_SUFFIXES
        and ".git" not in path.parts
        and "build" not in path.parts
    ]
    require(not offenders, f"Binary exercise media is not admitted: {offenders}")


def validate_llm_boundary() -> None:
    domain = (ROOT / "shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/domain/Domain.kt").read_text(
        encoding="utf-8"
    )
    require("val mayRecommendDose: Boolean = false" in domain, "LLM dose boundary is missing")
    require("val mayOverrideWarnings: Boolean = false" in domain, "LLM warning boundary is missing")
    require("MassUnit.IU" in domain and "-> null" in domain, "IU must not have a generic mass conversion")

    ledger = (ROOT / "shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/domain/DailyIntake.kt").read_text(
        encoding="utf-8"
    )
    require(
        "val mayBeComparedWithReviewedLimits: Boolean = false" in ledger,
        "Daily intake arithmetic must not imply reviewed limits",
    )


def validate_toolchain_and_platform_wiring() -> None:
    launcher = (ROOT / "gradlew").read_text(encoding="utf-8")
    require('required="9.5.0"' in launcher, "gradlew must pin Gradle 9.5.0")
    require("9.5.1" not in launcher, "unsupported Gradle 9.5.1 reference remains")

    catalog = (ROOT / "gradle/libs.versions.toml").read_text(encoding="utf-8")
    require('kotlin = "2.4.10"' in catalog, "Kotlin pin changed unexpectedly")
    require('agp = "9.1.0"' in catalog, "AGP pin changed unexpectedly")
    require("text-recognition-chinese" in catalog, "Bundled Chinese ML Kit dependency is missing")

    scanner = (
        ROOT
        / "androidApp/src/main/kotlin/dev/ed3c/gymcometrue/scan/AndroidLabelScanner.kt"
    ).read_text(encoding="utf-8")
    require("ChineseTextRecognizerOptions" in scanner, "Android scanner is not using Chinese OCR")
    require("file.delete()" in scanner, "Android temporary scan file deletion is missing")

    require(not (ROOT / "iosApp/project.safe.yml").exists(), "iOS safe-spec bypass must be removed")
    require(
        not (ROOT / "iosApp/GymComeTrue/NativeCapabilityBridgeV2.swift").exists(),
        "Duplicate iOS bridge must be removed",
    )
    project = (ROOT / "iosApp/project.yml").read_text(encoding="utf-8")
    require("NativeCapabilityBridge.swift" in project, "Canonical iOS bridge is not admitted")
    require("NativeCapabilityBridgeV2.swift" not in project, "Duplicate iOS bridge is admitted")


def validate_governance_docs() -> None:
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    agents = (ROOT / "AGENTS.md").read_text(encoding="utf-8")
    for heading in (
        "## Safety contract",
        "## Copyright and data admission",
        "## Honest capability matrix",
        "## Delivery state machine",
    ):
        require(heading in readme, f"README is missing {heading}")
    for invariant in (
        "OCR_IS_EVIDENCE_NOT_TRUTH",
        "MEDIA_DEFAULT_DENY",
        "LLM_EXPLANATION_ONLY",
        "NO_CLIENT_PROVIDER_SECRETS",
    ):
        require(invariant in agents, f"AGENTS.md is missing invariant {invariant}")

    for path in (
        "README.zh-TW.md",
        "CONTRIBUTING.md",
        "SECURITY.md",
        "docs/platform-capability-matrix.md",
        "docs/store-compliance.md",
    ):
        require((ROOT / path).is_file(), f"Missing governance document: {path}")


def print_manifest_digest() -> None:
    admitted = sorted(path.relative_to(ROOT).as_posix() for path in iter_admitted_text_files())
    digest = hashlib.sha256("\n".join(admitted).encode("utf-8")).hexdigest()
    print(f"admitted-text-manifest-sha256={digest}")


def main() -> int:
    checks = (
        validate_source_registry,
        validate_media_registry,
        validate_seed_data,
        validate_no_secrets_or_hotlinks,
        validate_no_unregistered_binary_media,
        validate_llm_boundary,
        validate_toolchain_and_platform_wiring,
        validate_governance_docs,
    )
    try:
        for check in checks:
            check()
            print(f"PASS {check.__name__}")
        print_manifest_digest()
    except ValidationError as error:
        print(f"FAIL {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
