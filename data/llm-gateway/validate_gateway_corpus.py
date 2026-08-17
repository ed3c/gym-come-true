#!/usr/bin/env python3
"""Fail-closed checks binding the adversarial corpus to the Kotlin explanation gateway.

Standard library only, no network. The Kotlin harness is the executable authority; this validator
proves the JSON corpus, the harness, and the admitted template catalogue have not drifted apart,
and that the corpus stays synthetic and unadmitted.

Usage: python3 data/llm-gateway/validate_gateway_corpus.py
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
CORPUS = ROOT / "data/llm-gateway/adversarial-corpus.v1.json"
CONTRACT = ROOT / "shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/explanation/ExplanationGatewayContract.kt"
BOUNDARY = ROOT / "shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/explanation/ExplanationProviderBoundary.kt"
HARNESS = ROOT / "shared/src/commonTest/kotlin/dev/ed3c/gymcometrue/explanation/AdversarialExplanationEvalTest.kt"

CASE_RE = re.compile(
    r'AdversarialCase\(\s*id = "([^"]+)",\s*category = "([^"]+)",'
    r"\s*expectedOutcome = GatewayOutcomeKind\.(\w+),"
    r"\s*expectedRejection = (?:GatewayRejection\.(\w+)|null),"
)
FORBIDDEN_TEMPLATE_WORDS = ("dose", "diagnos", "treat", "cure", "prescri")
FORBIDDEN_CONTRACT_TOKENS = ("apiKey", "api_key", "Authorization", "Bearer ", "http://", "https://")
REQUIRED_CATEGORIES = (
    "MISSING_EVIDENCE",
    "IU",
    "MEDICATION_SYMPTOM",
    "PROMPT_INJECTION",
    "DOSE_REQUEST",
    "INVENTED_EVIDENCE",
    "WARNING_SUPPRESSION",
    "PROVIDER_FAILURE",
)


class ValidationError(RuntimeError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


def read(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except FileNotFoundError as error:
        raise ValidationError(f"Missing required file: {path.relative_to(ROOT)}") from error


def load_corpus() -> dict[str, Any]:
    try:
        document = json.loads(read(CORPUS))
    except json.JSONDecodeError as error:
        raise ValidationError(f"Invalid JSON in {CORPUS.relative_to(ROOT)}: {error}") from error
    require(isinstance(document, dict), "The corpus must be a JSON object")
    return document


def enum_members(source: str, name: str) -> set[str]:
    match = re.search(rf"enum class {name} \{{(.*?)\n\}}", source, re.DOTALL)
    require(match is not None, f"enum class {name} is missing")
    assert match is not None
    return set(re.findall(r"^\s{4}([A-Z][A-Z_0-9]*),", match.group(1), re.MULTILINE))


def kotlin_cases() -> list[tuple[str, str, str, str | None]]:
    source = read(HARNESS)
    cases = [
        (case_id, category, outcome, rejection or None)
        for case_id, category, outcome, rejection in CASE_RE.findall(source)
    ]
    require(bool(cases), "No AdversarialCase entries were parsed from the Kotlin harness")
    return cases


def validate_corpus_is_synthetic_and_unadmitted() -> None:
    document = load_corpus()
    require(document.get("syntheticOnly") is True, "The corpus must declare syntheticOnly=true")
    require(
        document.get("productionAdmitted") is False,
        "An input corpus can never declare productionAdmitted=true",
    )
    require(
        document.get("containsRealUserData") is False,
        "The corpus must declare containsRealUserData=false",
    )
    require(document.get("evidenceState") == "DRAFT", "The corpus evidence state must stay DRAFT")
    subject = document.get("subjectUnderEval", {})
    require(
        subject.get("realProviderExercised") == "ABSENT",
        "No real provider has been exercised; the corpus must say so",
    )
    require(
        subject.get("redTeamReview") == "HUMAN_ADMIT_REQUIRED",
        "Red-team review is an external gate and must stay HUMAN_ADMIT_REQUIRED",
    )
    raw = read(CORPUS)
    offenders = [token for token in FORBIDDEN_CONTRACT_TOKENS if token in raw]
    require(not offenders, f"The corpus must not carry credentials or live URLs: {offenders}")


def validate_corpus_matches_kotlin_harness() -> None:
    document = load_corpus()
    cases = document.get("cases")
    require(isinstance(cases, list) and cases, "The corpus needs cases")

    ids = [case.get("id") for case in cases]
    require(len(ids) == len(set(ids)), "Duplicate case id in the corpus")
    for case in cases:
        require(bool(case.get("attack")), f"Case {case.get('id')} has no attack description")

    json_view = [
        (case["id"], case["category"], case["expectedOutcome"], case.get("expectedRejection"))
        for case in cases
    ]
    kotlin_view = kotlin_cases()
    require(
        sorted(json_view) == sorted(kotlin_view),
        "The JSON corpus and the Kotlin harness disagree: "
        f"json-only={sorted(set(json_view) - set(kotlin_view))} "
        f"kotlin-only={sorted(set(kotlin_view) - set(json_view))}",
    )


def validate_expectations_reference_real_states() -> None:
    document = load_corpus()
    rejections = enum_members(read(CONTRACT), "GatewayRejection")
    outcomes = enum_members(read(BOUNDARY), "GatewayOutcomeKind")
    for case in document["cases"]:
        require(
            case["expectedOutcome"] in outcomes,
            f"Case {case['id']} expects unknown outcome {case['expectedOutcome']}",
        )
        expected_rejection = case.get("expectedRejection")
        require(
            expected_rejection is None or expected_rejection in rejections,
            f"Case {case['id']} expects unknown rejection {expected_rejection}",
        )
        if case["expectedOutcome"] == "MODEL_PLAN_ACCEPTED":
            require(
                expected_rejection is None,
                f"Case {case['id']} cannot be accepted and rejected at once",
            )


def validate_coverage_and_controls() -> None:
    document = load_corpus()
    cases = document["cases"]
    categories = {case["category"] for case in cases}
    missing = [category for category in REQUIRED_CATEGORIES if category not in categories]
    require(not missing, f"The corpus is missing required categories: {missing}")

    accepted = [case for case in cases if case["expectedOutcome"] == "MODEL_PLAN_ACCEPTED"]
    require(accepted, "Without a positive control the suite could pass by rejecting everything")
    adversarial = [case for case in cases if case["expectedOutcome"] != "MODEL_PLAN_ACCEPTED"]
    require(len(adversarial) >= 20, f"Only {len(adversarial)} adversarial cases remain")


def validate_no_dose_or_diagnosis_template_is_admitted() -> None:
    source = read(CONTRACT)
    templates = set(re.findall(r'"(tpl\.[a-z0-9.\-]+)"', source))
    require(bool(templates), "No admitted templates were found in the gateway contract")
    offenders = [
        template
        for template in templates
        if any(word in template for word in FORBIDDEN_TEMPLATE_WORDS)
    ]
    require(not offenders, f"Dose or diagnosis templates must never be admitted: {offenders}")


def validate_shared_code_has_no_provider_secrets() -> None:
    for path in (CONTRACT, BOUNDARY):
        source = read(path)
        offenders = [token for token in FORBIDDEN_CONTRACT_TOKENS if token in source]
        require(
            not offenders,
            f"{path.relative_to(ROOT)} must not carry credentials or network endpoints: {offenders}",
        )
    require(
        "PROVIDER_NOT_ADMITTED" in read(BOUNDARY) and "killSwitchEngaged" in read(BOUNDARY),
        "The provider boundary must keep the admission gate and the kill switch",
    )


def main() -> int:
    checks = (
        validate_corpus_is_synthetic_and_unadmitted,
        validate_corpus_matches_kotlin_harness,
        validate_expectations_reference_real_states,
        validate_coverage_and_controls,
        validate_no_dose_or_diagnosis_template_is_admitted,
        validate_shared_code_has_no_provider_secrets,
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
