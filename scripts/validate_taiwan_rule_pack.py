#!/usr/bin/env python3
"""Fail-closed structural checks for Taiwan supplement evidence fixtures."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
SHA256 = re.compile(r"^[0-9a-f]{64}$")
REQUIRED_CASES = {
    "iu-unresolved",
    "missing-serving",
    "duplicate-ingredient",
    "proprietary-blend",
    "medication-context",
    "adverse-symptom",
    "source-conflict",
}


class ValidationError(RuntimeError):
    pass


def load(path: str) -> Any:
    target = ROOT / path
    try:
        return json.loads(target.read_text(encoding="utf-8"))
    except (FileNotFoundError, json.JSONDecodeError) as error:
        raise ValidationError(f"Cannot load {path}: {error}") from error


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


def validate_registry() -> None:
    registry = load("legal/taiwan-supplement-source-registry.json")
    require(registry.get("jurisdiction") == "TW", "Taiwan registry jurisdiction must be TW")
    require(registry.get("defaultPolicy") == "DENY", "Taiwan registry must default deny")
    sources = registry.get("sources", [])
    require(sources, "Taiwan registry has no source candidates")
    ids: set[str] = set()
    for source in sources:
        source_id = source.get("id")
        require(source_id and source_id not in ids, f"Invalid or duplicate source id: {source_id}")
        ids.add(source_id)
        require(source.get("status") in {"REVIEW", "DENY"}, f"Unreviewed source cannot be ALLOW: {source_id}")
        require(str(source.get("canonicalUrl", "")).startswith("https://"), f"Non-HTTPS source: {source_id}")
        require(source.get("snapshotSha256") is None, f"Fixture must not pretend a snapshot was archived: {source_id}")
        require(bool(source.get("prohibitedUse")), f"Missing prohibited-use boundary: {source_id}")


def validate_corpus_fixture() -> None:
    record = load("data/taiwan-supplement/corpus-manifest.example.json")
    require(record.get("synthetic") is True, "Repository corpus fixture must be synthetic")
    require(record.get("consent") == "SYNTHETIC", "Synthetic corpus consent is missing")
    require(record.get("storesRawImage") is False, "Repository fixture must not store a raw label image")
    require(record.get("productionUse") == "DENY", "Synthetic fixture must be denied for production")
    require(bool(SHA256.fullmatch(record.get("rawTextSha256", ""))), "Invalid raw-text SHA-256")
    require(record.get("product", {}).get("market") == "TW", "Synthetic product market must be TW")


def validate_draft_pack() -> None:
    pack = load("data/taiwan-supplement/rule-pack.draft.example.json")
    require(pack.get("jurisdiction") == "TW", "Draft pack jurisdiction must be TW")
    require(pack.get("status") == "DRAFT", "Repository pack fixture must remain DRAFT")
    require(pack.get("productionAdmitted") is False, "Draft pack must not be admitted")
    require(pack.get("contentSha256") is None, "Draft fixture must not fabricate a pack hash")
    require(pack.get("reviewerAttestation") is None, "Draft fixture must not fabricate review")
    require(pack.get("rollbackToVersion") is None, "Draft fixture must not fabricate rollback evidence")
    require(set(pack.get("testCaseIds", [])) == REQUIRED_CASES, "Draft pack safety-case contract changed")
    require(pack.get("rules") == [], "No unreviewed production rule may be seeded")


def validate_schemas() -> None:
    for path in (
        "data/taiwan-supplement/schemas/product-variant.schema.json",
        "data/taiwan-supplement/schemas/corpus-record.schema.json",
        "data/taiwan-supplement/schemas/rule-pack.schema.json",
    ):
        schema = load(path)
        require(schema.get("$schema") == "https://json-schema.org/draft/2020-12/schema", f"Unexpected schema draft: {path}")
        require(schema.get("type") == "object", f"Schema root must be object: {path}")


def validate_kotlin_contract() -> None:
    path = ROOT / "shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/domain/TaiwanSupplementEvidence.kt"
    text = path.read_text(encoding="utf-8")
    for token in (
        "RulePackStatus.CLINICALLY_REVIEWED",
        "modelUsedForDecision: Boolean = false",
        "requiredSafetyCaseIds",
        "RulePackAdmission.REVIEW_REQUIRED",
        "A model may explain a receipt but cannot own the decision.",
    ):
        require(token in text, f"Missing Taiwan evidence invariant: {token}")


def main() -> int:
    try:
        for check in (validate_registry, validate_corpus_fixture, validate_draft_pack, validate_schemas, validate_kotlin_contract):
            check()
            print(f"PASS {check.__name__}")
    except ValidationError as error:
        print(f"FAIL {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
