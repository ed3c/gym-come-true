#!/usr/bin/env python3
"""Fail-closed checks for the Taiwan consent-corpus, OCR-evaluation, and rule-pack gate contracts.

Covers Issues #24 (TW1), #25 (TW2), and #26 (TW3).

This validator is deliberately a second, independent implementation of the invariants that the
Kotlin domain code enforces. A single implementation checking itself proves only that it is
self-consistent.
"""

from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
DATA = "data/taiwan-supplement"
DOMAIN = "shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/domain"

SHA256 = re.compile(r"^[0-9a-f]{64}$")
HAN = re.compile("[㐀-䶿一-鿿]")
ISO_DATE = re.compile(r"^\d{4}-\d{2}-\d{2}$")

# Keys that must never appear anywhere in an aggregate that leaves an evaluation device.
FORBIDDEN_AGGREGATE_KEYS = {
    "rawtext",
    "rawtextsha256",
    "text",
    "image",
    "imagepath",
    "imagesha256",
    "imageuri",
    "path",
    "filepath",
    "url",
    "uri",
    "corpusrecordid",
    "corpusrecordids",
    "recordid",
    "recordids",
    "records",
    "observations",
    "expected",
    "observed",
    "corrected",
    "subjectpseudonymousid",
}

# The wire strings of an aggregate report; mirrors OcrAggregateLeakScanner.
AGGREGATE_STRING_FIELDS = (
    "runId",
    "engineVersion",
    "modelVersion",
    "deviceModel",
    "osVersion",
    "evaluatedAtIsoDate",
)


class ValidationError(RuntimeError):
    pass


def load(path: str) -> Any:
    target = ROOT / path
    try:
        return json.loads(target.read_text(encoding="utf-8"))
    except (FileNotFoundError, json.JSONDecodeError) as error:
        raise ValidationError(f"Cannot load {path}: {error}") from error


def read_text(path: str) -> str:
    target = ROOT / path
    try:
        return target.read_text(encoding="utf-8")
    except FileNotFoundError as error:
        raise ValidationError(f"Cannot load {path}: {error}") from error


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


def iso_key(value: Any) -> int | None:
    if not isinstance(value, str) or not ISO_DATE.fullmatch(value):
        return None
    year, month, day = (int(part) for part in value.split("-"))
    if not 1 <= month <= 12:
        return None
    leap = year % 400 == 0 or (year % 4 == 0 and year % 100 != 0)
    max_day = 29 if month == 2 and leap else [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month - 1]
    if not 1 <= day <= max_day:
        return None
    return year * 10000 + month * 100 + day


def validate_consent_grant() -> None:
    """Issue #24: the synthetic grant must hash real bytes and declare a bounded window."""
    grant = load(f"{DATA}/consent-grant.synthetic.json")
    require(grant.get("synthetic") is True, "The repository consent grant must be synthetic")
    require(grant.get("productionUse") == "DENY", "A synthetic consent grant must deny production use")
    require(grant.get("withdrawnAtIsoDate") is None, "The synthetic grant fixture must not be pre-withdrawn")
    require(bool(grant.get("scopes")), "Consent must declare at least one purpose scope")

    for hash_field, file_field, length_field in (
        ("consentReceiptSha256", "consentReceiptFile", "consentReceiptByteLength"),
        ("consentTextSha256", "consentTextFile", "consentTextByteLength"),
    ):
        declared = grant.get(hash_field, "")
        require(bool(SHA256.fullmatch(declared)), f"{hash_field} must be a lowercase SHA-256")
        name = grant.get(file_field)
        require(isinstance(name, str) and name, f"{file_field} must name the hashed bytes")
        payload = (ROOT / DATA / name).read_bytes() if (ROOT / DATA / name).exists() else None
        require(payload is not None, f"{file_field} points at missing bytes: {name}")
        actual = hashlib.sha256(payload).hexdigest()
        require(
            actual == declared,
            f"{hash_field} does not match {name}: declared {declared}, actual {actual}",
        )
        require(
            grant.get(length_field) == len(payload),
            f"{length_field} does not match the real byte length of {name}",
        )

    granted = iso_key(grant.get("grantedAtIsoDate"))
    expires = iso_key(grant.get("expiresAtIsoDate"))
    require(granted is not None, "Consent requires a valid grant date")
    require(expires is not None, "Consent requires a bounded expiry; unbounded consent fails closed")
    require(granted <= expires, "The consent window must not be inverted")

    gates = grant.get("externalGates", {})
    require(bool(gates), "The consent grant must record its external gates")
    require(
        all(state == "ABSENT" for state in gates.values()),
        "No consent external gate may claim a state stronger than ABSENT in this repository",
    )


def validate_deletion_counterexample() -> None:
    """Issue #24: deletion cannot be manifest-only, and the repository keeps the failing shape."""
    request = load(f"{DATA}/deletion-request.manifest-only-counterexample.json")
    require(request.get("fixtureKind") == "NEGATIVE_COUNTEREXAMPLE", "This fixture must stay a counterexample")
    require(request.get("admissible") is False, "The manifest-only fixture must never be admissible")
    require(request.get("expectedCompleteness") == "MANIFEST_ONLY", "Expected completeness changed")
    require(request.get("manifestUpdated") is True, "The counterexample needs the manifest flag set")
    require(request.get("receipts") == [], "The counterexample must carry no erasure receipt")

    declared = request.get("declaredLocations", [])
    require(bool(declared), "A deletion request must declare its storage locations")
    if request.get("usedForOcrEvaluation") is True:
        require(
            "DERIVED_OCR_METRICS" in declared,
            "A record used for OCR evaluation must declare DERIVED_OCR_METRICS for deletion",
        )


def _scan_keys(node: Any, findings: list[str], trail: str = "$") -> None:
    if isinstance(node, dict):
        for key, value in node.items():
            if key.lower() in FORBIDDEN_AGGREGATE_KEYS:
                findings.append(f"{trail}.{key}")
            _scan_keys(value, findings, f"{trail}.{key}")
    elif isinstance(node, list):
        for index, value in enumerate(node):
            _scan_keys(value, findings, f"{trail}[{index}]")


def validate_ocr_aggregate() -> None:
    """Issue #25: the aggregate carries counts and versions only, and claims no measurement."""
    report = load(f"{DATA}/ocr-evaluation-report.absent.json")
    require(report.get("measurement") == "ABSENT", "No OCR measurement exists in this repository")
    require(report.get("executedOnRealDevice") is False, "No authorized device run has occurred")
    require(report.get("recordCount") == 0, "A zero-run aggregate must report zero records")
    require(report.get("observationCount") == 0, "A zero-run aggregate must report zero observations")
    require(report.get("fieldMetrics") == [], "A zero-run aggregate must report no field metrics")
    require(
        report.get("firstPassExactAccuracy") == 0.0,
        "Zero observations must read as zero accuracy, never as perfect accuracy",
    )
    require(
        report.get("correctionCompletion") is None,
        "Nothing to correct must read as null completion, never as 1.0",
    )

    findings: list[str] = []
    _scan_keys(report, findings)
    require(not findings, f"Aggregate output carries forbidden keys: {sorted(findings)}")

    for field in AGGREGATE_STRING_FIELDS:
        value = report.get(field)
        require(isinstance(value, str), f"Aggregate field {field} must be a string")
        require(not HAN.search(value), f"Aggregate field {field} carries Han characters")
        require("/" not in value and "\\" not in value, f"Aggregate field {field} looks like a path")
        require("://" not in value, f"Aggregate field {field} carries a URI")
        require(len(value) <= 64, f"Aggregate field {field} is long enough to hide content")

    schema = load(f"{DATA}/schemas/ocr-evaluation-report.schema.json")
    require(
        schema.get("$schema") == "https://json-schema.org/draft/2020-12/schema",
        "Unexpected schema draft for the OCR aggregate",
    )
    require(schema.get("type") == "object", "The OCR aggregate schema root must be an object")
    require(
        schema.get("additionalProperties") is False,
        "The OCR aggregate schema must forbid additional properties",
    )
    allowed = set(schema.get("properties", {}))
    extra = set(report) - allowed
    require(not extra, f"The aggregate fixture carries keys the schema does not allow: {sorted(extra)}")


def _kotlin_enum_values(source: str, name: str) -> list[str]:
    match = re.search(r"enum class " + name + r"\s*\{(.*?)\n\}", source, re.DOTALL)
    if match is None:
        raise ValidationError(f"Cannot find enum class {name} in the Kotlin domain")
    body = re.sub(r"/\*.*?\*/", "", match.group(1), flags=re.DOTALL)
    body = re.sub(r"//[^\n]*", "", body)
    return re.findall(r"^\s*([A-Z][A-Z0-9_]*)\s*,", body, re.MULTILINE)


def validate_external_gate_ledger() -> None:
    """Issue #26: every external gate is ABSENT, and the ledger names the exact Kotlin gates."""
    ledger = load(f"{DATA}/rule-pack-external-gates.absent.json")
    require(ledger.get("jurisdiction") == "TW", "The gate ledger jurisdiction must be TW")
    require(ledger.get("defaultPolicy") == "DENY", "The gate ledger must default deny")
    require(
        ledger.get("decision") == "EXTERNAL_GATES_ABSENT",
        "The repository decision for the Taiwan rule pack must remain EXTERNAL_GATES_ABSENT",
    )

    statuses = ledger.get("statuses", [])
    require(bool(statuses), "The gate ledger must enumerate its gates")
    listed = [entry.get("gate") for entry in statuses]
    require(len(listed) == len(set(listed)), "The gate ledger repeats a gate")

    for entry in statuses:
        gate = entry.get("gate")
        require(entry.get("state") == "ABSENT", f"Gate {gate} claims a state stronger than ABSENT")
        require(entry.get("evidenceRef") is None, f"Gate {gate} must not reference evidence that does not exist")
        require(bool(entry.get("note")), f"Gate {gate} must say what is missing")

    source = read_text(f"{DOMAIN}/TaiwanReviewedRulePackGate.kt")
    declared = _kotlin_enum_values(source, "ExternalGate")
    require(len(declared) >= 10, "ExternalGate lost gates; the admission surface shrank")
    require(
        sorted(declared) == sorted(listed),
        f"The ledger and ExternalGate disagree: kotlin={sorted(declared)} ledger={sorted(listed)}",
    )

    states = _kotlin_enum_values(source, "ExternalGateState")
    require("HUMAN_ADMITTED" in states, "ExternalGateState must keep an explicit human-admit state")
    admits = re.findall(r"ExternalGateState\.HUMAN_ADMITTED", source)
    require(
        len(admits) <= 2,
        "HUMAN_ADMITTED appears in more repository code paths than the two comparisons; "
        "no code may assign it",
    )
    # An assignment is `= HUMAN_ADMITTED`; a comparison is `==` or `!=` and is allowed.
    assignment = re.search(r"(?<![!=<>])=\s*ExternalGateState\.HUMAN_ADMITTED", source)
    require(assignment is None, "Repository code must never assign HUMAN_ADMITTED")


def validate_kotlin_contract() -> None:
    """The invariants the domain code must keep stating in executable form."""
    expected = {
        "TaiwanConsentCorpus.kt": (
            "enum class ConsentState",
            "UNVERIFIABLE",
            "WITHDRAWN",
            "Deletion cannot be manifest-only",
            "object ConsentResolver",
            "object CorpusDeletionValidator",
        ),
        "TaiwanOcrEvaluation.kt": (
            "object OcrAggregateLeakScanner",
            "executedOnRealDevice",
            "firstPassExactAccuracy",
            "correctionCompletion",
            "may not be evaluated",
        ),
        "TaiwanReviewedRulePackGate.kt": (
            "EXTERNAL_GATES_ABSENT",
            "ConflictOfInterestDeclaration",
            "bounded effective window",
            "fun gatesNotAdmitted",
        ),
    }
    for name, tokens in expected.items():
        text = read_text(f"{DOMAIN}/{name}")
        for token in tokens:
            require(token in text, f"Missing invariant in {name}: {token}")


def main() -> int:
    checks = (
        validate_consent_grant,
        validate_deletion_counterexample,
        validate_ocr_aggregate,
        validate_external_gate_ledger,
        validate_kotlin_contract,
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
