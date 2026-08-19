#!/usr/bin/env python3
"""Fail closed when MVP product/safety authority surfaces contradict each other.

Offline and standard-library only. This validates checked-in product-positioning
consistency; it does not provide legal, clinical, store, device, or provider approval.
"""
from __future__ import annotations

import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class ProductSafetyAuthorityError(ValueError):
    pass


def require(text: str, needle: str, subject: str) -> None:
    if needle not in text:
        raise ProductSafetyAuthorityError(
            f"{subject}: required product/safety authority missing: {needle!r}"
        )


def reject(text: str, needle: str, subject: str) -> None:
    if needle in text:
        raise ProductSafetyAuthorityError(
            f"{subject}: contradictory or retired product authority present: {needle!r}"
        )


def validate(product: str, disclaimer: str, health: str) -> None:
    # Owner product decision.
    require(product, "Authority: repository owner (Human Admit)", "mvp-redesign")
    require(product, "information and logging tool", "mvp-redesign")
    require(product, "no clinical rule pack is required", "mvp-redesign")
    require(product, "#26 reviewed TW rule pack", "mvp-redesign")
    require(product, "AI responses are general information only and are not medical advice", "mvp-redesign")

    # User-facing disclaimer SSOT.
    require(disclaimer, "user notice and disclaimer (SSOT)", "DISCLAIMER")
    require(disclaimer, "information and logging tool", "DISCLAIMER")
    require(disclaimer, "it renders no safety verdicts", "DISCLAIMER")
    require(disclaimer, "AI responses are general information only and are not medical advice", "DISCLAIMER")
    require(disclaimer, "Final terms-of-service language should get legal review before store submission", "DISCLAIMER")

    # Health/safety implementation contract must follow the owner decision.
    require(health, "INFORMATION_OR_LOGGING != SAFETY_VERDICT", "health-safety")
    require(health, "ARITHMETIC_RESULT != DOSE_RECOMMENDATION", "health-safety")
    require(health, "REVIEWED_RULE_PACK_CONTRACT_PRESENT != MVP_RULE_PACK_REQUIRED", "health-safety")
    require(health, "MODEL_EXPLANATION != MEDICAL_AUTHORITY", "health-safety")
    require(health, "a reviewed Taiwan clinical rule pack is **not** required", "health-safety")
    require(health, "The MVP does not implement medication-interaction lookup", "health-safety")
    require(health, "OpenAI (ChatGPT) and Anthropic (Claude)", "health-safety")
    require(health, "ADAPTER_PRESENT != REAL_DEVICE_VALIDATION", "health-safety")
    require(health, "DECLARED_PERMISSION != STORE_APPROVAL", "health-safety")
    require(health, "GITHUB_CHECK_PASS != HUMAN_ADMIT", "health-safety")
    require(health, "DISCLAIMER_PRESENT != LEGAL_APPROVAL", "health-safety")
    require(health, "NO_SAFETY_VERDICT_MVP != NO_REGULATORY_OBLIGATIONS", "health-safety")

    # Retired/contradictory MVP authority must not silently return.
    for claim in (
        "Supplement intelligence cannot move from foundation to production until",
        "a production Taiwan rule pack needs all of the following",
        "reviewed regional rule-pack lookup\n  -> log-only, review-required, or block receipt",
        "apply reviewed deterministic rules that produce `LOG_ONLY`, `REVIEW_REQUIRED`, or `BLOCK_AUTOMATION`",
        "The receipt is the authority passed to an explanation model",
    ):
        reject(health.lower(), claim.lower(), "health-safety")

    # Safety invariants stay conservative despite retiring the clinical verdict lane.
    for required in (
        "must not diagnose",
        "must not",
        "IU",
        "not a current MVP gate",
        "real-device",
        "privacy",
        "rights evidence",
        "signed release builds",
    ):
        require(health, required, "health-safety")


def read_surfaces(root: Path) -> tuple[str, str, str]:
    return (
        (root / "docs/product/mvp-redesign.md").read_text(encoding="utf-8"),
        (root / "legal/DISCLAIMER.md").read_text(encoding="utf-8"),
        (root / "docs/health-safety.md").read_text(encoding="utf-8"),
    )


def self_test() -> None:
    base = list(read_surfaces(ROOT))
    validate(*base)
    mutations: list[tuple[int, str, str, str]] = [
        (0, "replace", "information and logging tool\0medical safety tool", "product-positioning"),
        (0, "replace", "no clinical rule pack is required\0a clinical rule pack is required", "product-retired-rule-pack"),
        (1, "replace", "it renders no safety verdicts\0it renders safety verdicts", "disclaimer-verdict"),
        (1, "replace", "Final terms-of-service language should get legal review before store submission\0No legal review is required", "disclaimer-legal-gate"),
        (2, "append", "Supplement intelligence cannot move from foundation to production until a reviewed rule pack is active.", "health-old-production-gate"),
        (2, "replace", "REVIEWED_RULE_PACK_CONTRACT_PRESENT != MVP_RULE_PACK_REQUIRED\0REVIEWED_RULE_PACK_CONTRACT_PRESENT", "health-rule-pack-law"),
        (2, "replace", "ARITHMETIC_RESULT != DOSE_RECOMMENDATION\0ARITHMETIC_RESULT = DOSE_RECOMMENDATION", "health-dose-law"),
        (2, "replace", "MODEL_EXPLANATION != MEDICAL_AUTHORITY\0MODEL_EXPLANATION = MEDICAL_AUTHORITY", "health-model-law"),
        (2, "replace", "The MVP does not implement medication-interaction lookup\0The MVP implements medication-interaction lookup", "health-medication-lookup"),
        (2, "replace", "ADAPTER_PRESENT != REAL_DEVICE_VALIDATION\0ADAPTER_PRESENT = REAL_DEVICE_VALIDATION", "health-device-law"),
        (2, "replace", "DECLARED_PERMISSION != STORE_APPROVAL\0DECLARED_PERMISSION = STORE_APPROVAL", "health-store-law"),
        (2, "replace", "DISCLAIMER_PRESENT != LEGAL_APPROVAL\0DISCLAIMER_PRESENT = LEGAL_APPROVAL", "health-legal-law"),
    ]
    for index, mode, payload, name in mutations:
        mutated = base.copy()
        if mode == "append":
            mutated[index] += "\n" + payload + "\n"
        else:
            old, new = payload.split("\0", 1)
            if old not in mutated[index]:
                raise AssertionError(f"self-test token missing before mutation: {name}")
            mutated[index] = mutated[index].replace(old, new, 1)
        try:
            validate(*mutated)
        except ProductSafetyAuthorityError:
            print(f"PASS planted product/safety drift rejected: {name}")
        else:
            raise AssertionError(f"mutation was not rejected: {name}")
    print(f"PASS product/safety self-test: {len(mutations)} planted drifts rejected")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
    else:
        validate(*read_surfaces(ROOT))
        print("PASS product/safety authority")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
