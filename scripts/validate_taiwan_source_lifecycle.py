#!/usr/bin/env python3
"""Fail-closed checks for Taiwan source capture, mapping, and release lifecycle."""

from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
import tempfile
from datetime import date
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
SHA256 = re.compile(r"^[0-9a-f]{64}$")
HTTPS = re.compile(r"^https://[^\s]+$")
EXPECTED_OFFICIAL_SOURCE_IDS = {
    "mohw-vitamin-mineral-tablet-capsule-labeling-2019",
    "tfda-imported-tablet-capsule-foods",
    "tfda-food-additive-limits",
    "tfda-food-business-registry",
}
HIGH_IMPACT_SCOPES = {
    "REGULATORY_TEXT",
    "REFERENCE_VALUE",
    "TOLERANCE_RANGE",
}


class ValidationError(RuntimeError):
    """Raised when an evidence lifecycle fixture weakens a hard law."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


def load_json(relative_path: str) -> Any:
    path = ROOT / relative_path
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (FileNotFoundError, json.JSONDecodeError) as error:
        raise ValidationError(f"cannot load {relative_path}: {error}") from error


def validate_iso_date(value: object, label: str) -> None:
    require(isinstance(value, str), f"{label} must be an ISO date string")
    try:
        date.fromisoformat(value)
    except ValueError as error:
        raise ValidationError(f"{label} is not a valid ISO date: {value}") from error


def validate_official_candidates() -> None:
    registry = load_json("legal/taiwan-official-resource-candidates.json")
    require(registry.get("jurisdiction") == "TW", "official candidate registry must use TW")
    require(registry.get("defaultPolicy") == "DENY", "official candidate registry must default DENY")
    require(registry.get("medicalAdvice") is False, "official candidate registry must reject medical-advice authority")
    require(registry.get("networkCaptureInCi") is False, "CI must not silently recapture mutable official URLs")
    validate_iso_date(registry.get("observedAt"), "registry observedAt")

    license_ids = {row.get("id") for row in registry.get("licenses", [])}
    require("OGL-TW-1.0" in license_ids, "OGL Taiwan license candidate is missing")
    require("MOHW-SITE-TERMS-PENDING" in license_ids, "MOHW terms-review boundary is missing")

    rows = registry.get("sources", [])
    require(isinstance(rows, list), "official source candidates must be a list")
    ids = [row.get("id") for row in rows]
    require(set(ids) == EXPECTED_OFFICIAL_SOURCE_IDS, f"unexpected official candidate set: {ids}")
    require(len(ids) == len(set(ids)), "official source candidate IDs must be unique")

    by_id = {row["id"]: row for row in rows}
    for row in rows:
        source_id = row["id"]
        require(row.get("snapshotState") == "CANDIDATE", f"{source_id} must remain CANDIDATE")
        require(row.get("snapshotId") is None, f"{source_id} must not fabricate snapshotId")
        require(row.get("snapshotSha256") is None, f"{source_id} must not fabricate snapshot SHA-256")
        require(row.get("archiveUri") is None, f"{source_id} must not fabricate archive URI")
        require(row.get("productionUse") == "DENY", f"{source_id} must remain production DENY")
        require(row.get("modelGenerated") is False, f"{source_id} cannot be model-generated evidence")
        require(bool(HTTPS.fullmatch(str(row.get("canonicalUrl", "")))), f"{source_id} canonical URL must use HTTPS")
        require(bool(row.get("allowedResearchUse")), f"{source_id} needs a narrow allowed-research boundary")
        require(bool(row.get("prohibitedInference")), f"{source_id} needs a prohibited-inference boundary")
        retrievals = row.get("retrievalCandidates", [])
        require(isinstance(retrievals, list) and retrievals, f"{source_id} needs retrieval candidates")
        for retrieval in retrievals:
            require(
                bool(HTTPS.fullmatch(str(retrieval.get("url", "")))),
                f"{source_id} retrieval candidate must use HTTPS",
            )
            require(
                retrieval.get("artifactKind") in {"PDF", "CSV", "JSON", "XML", "HTML", "ZIP", "TEXT"},
                f"{source_id} has an invalid artifact kind",
            )

    require(by_id["tfda-imported-tablet-capsule-foods"].get("upstreamInfoId") == "23", "TFDA 9047 infoId drift")
    require(by_id["tfda-food-additive-limits"].get("upstreamInfoId") == "61", "TFDA 9640 infoId drift")
    require(by_id["tfda-food-business-registry"].get("upstreamInfoId") == "97", "TFDA 8938 infoId drift")
    require(
        "成分" in by_id["tfda-imported-tablet-capsule-foods"].get("observedFields", []),
        "TFDA 9047 ingredient field is missing",
    )
    require(
        "使用食品範圍及限量" in by_id["tfda-food-additive-limits"].get("observedFields", []),
        "TFDA 9640 scope/limit field is missing",
    )
    require(
        "食品業者登錄字號" in by_id["tfda-food-business-registry"].get("observedFields", []),
        "TFDA 8938 registration field is missing",
    )


def validate_synthetic_snapshot() -> None:
    manifest = load_json("data/taiwan-supplement/source-snapshot.synthetic.json")
    artifact_path = ROOT / "data/taiwan-supplement/source-snapshots/synthetic-labeling-guidance-v1.txt"
    data = artifact_path.read_bytes()
    digest = hashlib.sha256(data).hexdigest()

    require(manifest.get("snapshotId") == "synthetic-tw-label-guidance-v1", "synthetic snapshot ID changed")
    require(manifest.get("sourceId") == "synthetic-tw-label-guidance", "synthetic source ID changed")
    require(manifest.get("state") == "HASH_VERIFIED", "synthetic fixture must be HASH_VERIFIED")
    require(manifest.get("productionUse") == "TEST_ONLY", "synthetic fixture must remain TEST_ONLY")
    require(manifest.get("synthetic") is True, "synthetic fixture flag is missing")
    require(manifest.get("modelGenerated") is False, "synthetic source fixture cannot be model generated")
    require(manifest.get("legalReviewRef") is None, "synthetic fixture must not fabricate legal review")
    require(manifest.get("sha256") == digest, "synthetic artifact SHA-256 does not match receipt")
    require(manifest.get("byteLength") == len(data), "synthetic artifact byte length does not match receipt")
    require(bool(SHA256.fullmatch(digest)), "synthetic artifact digest is invalid")
    require(digest in str(manifest.get("archiveUri", "")), "synthetic archive URI is not content addressed")


def validate_mapping_fixture() -> None:
    payload = load_json("data/taiwan-supplement/field-mapping.draft.example.json")
    require(payload.get("jurisdiction") == "TW", "mapping fixture jurisdiction must be TW")
    require(payload.get("defaultPolicy") == "DENY", "mapping fixture must default DENY")
    require(payload.get("productionAdmitted") is False, "mapping fixture cannot self-admit production")

    mappings = payload.get("mappings", [])
    require(isinstance(mappings, list) and mappings, "mapping fixture is empty")
    ids = [mapping.get("mappingId") for mapping in mappings]
    require(len(ids) == len(set(ids)), "mapping IDs must be unique")
    require(all(mapping.get("modelGenerated") is False for mapping in mappings), "model-generated mapping detected")
    require(all(mapping.get("productionUse") != "ALLOW" for mapping in mappings), "fixture cannot contain ALLOW mapping")

    official_ids = EXPECTED_OFFICIAL_SOURCE_IDS
    synthetic_rows = []
    for mapping in mappings:
        source_id = mapping.get("sourceId")
        selector = mapping.get("selector", {})
        require(source_id in official_ids | {"synthetic-tw-label-guidance"}, f"unknown mapping source: {source_id}")
        require(bool(mapping.get("targetField")), f"mapping {mapping.get('mappingId')} has no target field")
        require(bool(selector.get("locator")), f"mapping {mapping.get('mappingId')} has no exact locator")

        if selector.get("kind") in {"PDF_PAGE_LINE", "TEXT_RANGE"}:
            line_start = selector.get("lineStart")
            line_end = selector.get("lineEnd")
            require(isinstance(line_start, int) and line_start > 0, "line-range mapping needs lineStart")
            require(isinstance(line_end, int) and line_end >= line_start, "line-range mapping needs ordered lineEnd")
        if selector.get("kind") == "PDF_PAGE_LINE":
            require(isinstance(selector.get("pageNumber"), int), "PDF mapping needs pageNumber")

        if source_id in official_ids:
            require(mapping.get("status") == "DRAFT", f"official mapping {mapping.get('mappingId')} must remain DRAFT")
            require(mapping.get("snapshotId") is None, "official draft mapping must not fabricate snapshotId")
            require(mapping.get("evidenceExcerptSha256") is None, "official draft mapping must not fabricate excerpt hash")
            require(mapping.get("productionUse") == "DENY", "official draft mapping must remain DENY")
            if mapping.get("claimScope") in HIGH_IMPACT_SCOPES:
                require(
                    mapping.get("qualifiedReviewerAttestationSha256") is None,
                    "draft fixture must not fabricate qualified review",
                )
        else:
            synthetic_rows.append(mapping)

    require(len(synthetic_rows) == 1, "expected exactly one synthetic verified mapping")
    mapping = synthetic_rows[0]
    require(mapping.get("status") == "VERIFIED", "synthetic mapping must be VERIFIED")
    require(mapping.get("productionUse") == "TEST_ONLY", "synthetic mapping must remain TEST_ONLY")
    require(mapping.get("snapshotId") == "synthetic-tw-label-guidance-v1", "synthetic mapping snapshot mismatch")
    excerpt_hash = mapping.get("evidenceExcerptSha256")
    require(bool(SHA256.fullmatch(str(excerpt_hash))), "synthetic mapping excerpt hash is invalid")

    artifact_lines = (
        ROOT / "data/taiwan-supplement/source-snapshots/synthetic-labeling-guidance-v1.txt"
    ).read_text(encoding="utf-8").splitlines()
    selector = mapping["selector"]
    selected = "\n".join(artifact_lines[selector["lineStart"] - 1 : selector["lineEnd"]])
    require(hashlib.sha256(selected.encode("utf-8")).hexdigest() == excerpt_hash, "synthetic exact excerpt hash mismatch")


def validate_lifecycle_fixture() -> None:
    payload = load_json("data/taiwan-supplement/lifecycle.draft.example.json")
    candidate = payload.get("candidate", {})
    require(payload.get("jurisdiction") == "TW", "lifecycle fixture jurisdiction must be TW")
    require(payload.get("requiredPromotionOrder") == ["DRAFT", "REVIEWED", "STAGED", "ACTIVE"], "promotion order changed")
    require(payload.get("events") == [], "draft lifecycle fixture must not fabricate signed events")
    require(payload.get("resolvedState") == "DRAFT", "draft lifecycle fixture must resolve to DRAFT")
    require(payload.get("productionAdmitted") is False, "draft lifecycle fixture cannot be admitted")
    require(candidate.get("modelUsedForDecision") is False, "model cannot own lifecycle decision")
    require(candidate.get("productionAdmitted") is False, "candidate cannot self-declare production admission")
    for field in (
        "contentSha256",
        "sourceBundleSha256",
        "testSuiteSha256",
        "reviewerAttestationSha256",
        "userFacingWordingSha256",
        "rollbackToVersion",
    ):
        require(candidate.get(field) is None, f"draft lifecycle fixture must not fabricate {field}")


def validate_schema_contracts() -> None:
    for path in (
        "data/taiwan-supplement/schemas/source-snapshot.schema.json",
        "data/taiwan-supplement/schemas/source-field-mapping.schema.json",
        "data/taiwan-supplement/schemas/rule-pack-lifecycle.schema.json",
    ):
        schema = load_json(path)
        require(
            schema.get("$schema") == "https://json-schema.org/draft/2020-12/schema",
            f"unexpected JSON Schema draft: {path}",
        )
        require(schema.get("type") == "object", f"schema root must be object: {path}")
        require(schema.get("additionalProperties") is False, f"schema must reject unknown root fields: {path}")


def validate_capture_command() -> None:
    script = ROOT / "scripts/capture_taiwan_source.py"
    require(script.is_file(), "source capture command is missing")
    with tempfile.TemporaryDirectory(prefix="tw-source-capture-") as directory:
        temporary = Path(directory)
        source = temporary / "input.txt"
        source.write_bytes(b"synthetic capture smoke\n")
        archive_root = temporary / "archive"
        manifest_out = temporary / "receipt.json"
        command = [
            sys.executable,
            str(script),
            "--source-id",
            "synthetic-capture-smoke",
            "--snapshot-id",
            "synthetic-capture-smoke-v1",
            "--input",
            str(source),
            "--artifact-kind",
            "TEXT",
            "--canonical-url",
            "repo://synthetic/capture-smoke",
            "--captured-at",
            "2026-08-15",
            "--media-type",
            "text/plain",
            "--license-id",
            "REPOSITORY_SYNTHETIC",
            "--attribution-text",
            "Repository-authored smoke fixture",
            "--note",
            "Validator-owned temporary fixture.",
            "--archive-root",
            str(archive_root),
            "--archive-uri-prefix",
            "evidence://test/taiwan-source-snapshots",
            "--manifest-out",
            str(manifest_out),
            "--synthetic",
            "--redistributable",
        ]
        result = subprocess.run(command, capture_output=True, text=True, check=False)
        require(result.returncode == 0, f"capture command failed: {result.stderr.strip()}")
        receipt = json.loads(manifest_out.read_text(encoding="utf-8"))
        console = json.loads(result.stdout)
        expected_hash = hashlib.sha256(source.read_bytes()).hexdigest()
        archive = Path(console["localArchive"])
        require(receipt.get("sha256") == expected_hash, "capture receipt hash mismatch")
        require(receipt.get("state") == "HASH_VERIFIED", "capture receipt must be HASH_VERIFIED")
        require(receipt.get("productionUse") == "DENY", "capture command must always default DENY")
        require(receipt.get("legalReviewRef") is None, "capture command cannot fabricate legal review")
        require(receipt.get("modelGenerated") is False, "capture command cannot emit model-generated evidence")
        require(archive.is_file(), "capture command did not create local archive")
        require(hashlib.sha256(archive.read_bytes()).hexdigest() == expected_hash, "captured archive bytes changed")


def validate_kotlin_contract() -> None:
    main_path = ROOT / "shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/domain/TaiwanSourceLifecycle.kt"
    test_path = ROOT / "shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/domain/TaiwanSourceLifecycleTest.kt"
    main_text = main_path.read_text(encoding="utf-8")
    test_text = test_path.read_text(encoding="utf-8")
    for token in (
        "SourceSnapshotState.LEGAL_REVIEWED",
        "ProductionEvidenceUse.TEST_ONLY",
        "A model cannot own the release decision.",
        "productionAdmitted = production &&",
        "Rollback target must equal rollbackToVersion.",
        "Lifecycle event sequences must be contiguous from 1.",
        "Mapping sourceId does not match the referenced snapshot.",
    ):
        require(token in main_text, f"missing Kotlin source-lifecycle invariant: {token}")
    require(test_text.count("@Test") == 12, "Taiwan source lifecycle must retain 12 contract tests")


def main() -> int:
    checks = (
        validate_official_candidates,
        validate_synthetic_snapshot,
        validate_mapping_fixture,
        validate_lifecycle_fixture,
        validate_schema_contracts,
        validate_capture_command,
        validate_kotlin_contract,
    )
    try:
        for check in checks:
            check()
            print(f"PASS {check.__name__}")
    except (ValidationError, OSError, json.JSONDecodeError) as error:
        print(f"FAIL {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
