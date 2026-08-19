#!/usr/bin/env python3
"""Validate the repository-authored Taiwan nutrition fixtures.

This gate is intentionally standard-library only and network-free. It verifies the
transport shape and the evidence ceiling of the checked-in examples; it does not
admit a real nutrition source or replace the Kotlin FoodCatalogAdmissionValidator.

Exit codes:
  0 baseline (and, with --self-test, all planted defects) passed
  1 contract violation
  64 invalid CLI usage/input
"""

from __future__ import annotations

import argparse
import copy
import json
import sys
from datetime import date
from pathlib import Path
from typing import Any, Callable

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "data/nutrition-catalog/food-catalog.example.json"
SCHEMA = ROOT / "data/nutrition-catalog/schemas/food-catalog-entry.schema.json"
SOURCES = ROOT / "data/nutrition-catalog/source-candidates.example.json"

MASS_UNITS = {"MCG", "MG", "G", "IU", "UNKNOWN"}
REQUIRED_LOCALES = {"ZH_TW", "EN"}
SOURCE_TYPES = {"REGULATOR_DATASET", "REGULATOR_GUIDANCE"}
LICENSE_STATES = {"DENY", "REQUIRES_LEGAL_REVIEW"}


class ContractError(ValueError):
    pass


def load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise ContractError(f"{path.relative_to(ROOT)}: unreadable JSON: {error}") from error


def fail(condition: bool, message: str) -> None:
    if not condition:
        raise ContractError(message)


def is_number(value: Any) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def exact_keys(obj: Any, allowed: set[str], required: set[str], where: str) -> dict[str, Any]:
    fail(isinstance(obj, dict), f"{where}: expected object")
    unknown = set(obj) - allowed
    missing = required - set(obj)
    fail(not unknown, f"{where}: unknown keys {sorted(unknown)}")
    fail(not missing, f"{where}: missing keys {sorted(missing)}")
    return obj


def nonblank(value: Any, where: str) -> str:
    fail(isinstance(value, str) and bool(value.strip()), f"{where}: expected non-blank string")
    return value


def nonnegative(value: Any, where: str) -> float:
    fail(is_number(value) and value >= 0, f"{where}: expected non-negative number")
    return float(value)


def positive_or_null(value: Any, where: str) -> None:
    fail(value is None or (is_number(value) and value > 0), f"{where}: expected null or positive number")


def validate_schema(schema: Any) -> None:
    schema = exact_keys(
        schema,
        {"$schema", "$id", "title", "description", "type", "additionalProperties", "required", "properties", "$defs"},
        {"$schema", "$id", "type", "additionalProperties", "required", "properties", "$defs"},
        "schema",
    )
    fail(schema["$schema"] == "https://json-schema.org/draft/2020-12/schema", "schema: unexpected draft")
    fail(schema["type"] == "object", "schema: root type must be object")
    fail(schema["additionalProperties"] is False, "schema: root must reject additional properties")
    props = schema["properties"]
    fail(isinstance(props, dict), "schema.properties: expected object")
    fail(props.get("schemaVersion", {}).get("const") == 1, "schema: schemaVersion must be const 1")
    fail(props.get("jurisdiction", {}).get("const") == "TW", "schema: jurisdiction must be const TW")
    fail(props.get("defaultPolicy", {}).get("const") == "DENY", "schema: defaultPolicy must be const DENY")
    entry = schema["$defs"].get("foodCatalogEntry")
    fail(isinstance(entry, dict) and entry.get("additionalProperties") is False, "schema: foodCatalogEntry must fail closed")
    fail(bool(entry.get("allOf")), "schema: non-synthetic mapping requirement is missing")


def validate_catalog(catalog: Any) -> tuple[int, int]:
    catalog = exact_keys(
        catalog,
        {"schemaVersion", "jurisdiction", "defaultPolicy", "entries"},
        {"schemaVersion", "jurisdiction", "defaultPolicy", "entries"},
        "catalog",
    )
    fail(catalog["schemaVersion"] == 1, "catalog.schemaVersion must be 1")
    fail(catalog["jurisdiction"] == "TW", "catalog.jurisdiction must be TW")
    fail(catalog["defaultPolicy"] == "DENY", "catalog.defaultPolicy must remain DENY")
    entries = catalog["entries"]
    fail(isinstance(entries, list) and entries, "catalog.entries must be a non-empty array")

    ids: set[str] = set()
    unresolved = 0
    for index, raw in enumerate(entries):
        where = f"catalog.entries[{index}]"
        raw = exact_keys(
            raw,
            {"identity", "profile", "servings", "sourceId", "mappingId", "synthetic", "note"},
            {"identity", "profile", "sourceId", "synthetic", "note"},
            where,
        )
        identity = exact_keys(
            raw["identity"],
            {"foodId", "category", "market", "names"},
            {"foodId", "category", "market", "names"},
            f"{where}.identity",
        )
        food_id = nonblank(identity["foodId"], f"{where}.identity.foodId")
        fail(food_id not in ids, f"{where}: duplicate foodId {food_id}")
        ids.add(food_id)
        nonblank(identity["category"], f"{where}.identity.category")
        fail(identity["market"] == "TW", f"{where}.identity.market must be TW")

        names = identity["names"]
        fail(isinstance(names, list) and names, f"{where}.identity.names must be a non-empty array")
        locales: list[str] = []
        for nidx, name in enumerate(names):
            name = exact_keys(
                name,
                {"locale", "name"},
                {"locale", "name"},
                f"{where}.identity.names[{nidx}]",
            )
            locale = name["locale"]
            fail(locale in REQUIRED_LOCALES, f"{where}: unsupported locale {locale!r}")
            locales.append(locale)
            nonblank(name["name"], f"{where}.identity.names[{nidx}].name")
        fail(len(locales) == len(set(locales)), f"{where}: duplicate localized-name locale")
        fail(set(locales) == REQUIRED_LOCALES, f"{where}: repository sample must be bilingual zh-TW/en")

        profile = exact_keys(
            raw["profile"],
            {
                "energyKcalPer100g", "proteinGPer100g", "fatGPer100g",
                "carbohydrateGPer100g", "fiberGPer100g", "micronutrientsPer100g",
            },
            {"energyKcalPer100g", "proteinGPer100g", "fatGPer100g", "carbohydrateGPer100g"},
            f"{where}.profile",
        )
        for field in ("energyKcalPer100g", "proteinGPer100g", "fatGPer100g", "carbohydrateGPer100g"):
            nonnegative(profile[field], f"{where}.profile.{field}")
        if "fiberGPer100g" in profile and profile["fiberGPer100g"] is not None:
            nonnegative(profile["fiberGPer100g"], f"{where}.profile.fiberGPer100g")

        micronutrients = profile.get("micronutrientsPer100g", [])
        fail(isinstance(micronutrients, list), f"{where}.profile.micronutrientsPer100g must be an array")
        for midx, nutrient in enumerate(micronutrients):
            nutrient = exact_keys(
                nutrient,
                {"nutrientKey", "amount", "unit"},
                {"nutrientKey", "amount", "unit"},
                f"{where}.profile.micronutrientsPer100g[{midx}]",
            )
            nonblank(nutrient["nutrientKey"], f"{where}.micronutrient[{midx}].nutrientKey")
            nonnegative(nutrient["amount"], f"{where}.micronutrient[{midx}].amount")
            fail(nutrient["unit"] in MASS_UNITS, f"{where}.micronutrient[{midx}]: unknown unit")

        servings = raw.get("servings", [])
        fail(isinstance(servings, list), f"{where}.servings must be an array")
        resolved_serving = False
        for sidx, serving in enumerate(servings):
            serving = exact_keys(
                serving,
                {"label", "grams"},
                {"label", "grams"},
                f"{where}.servings[{sidx}]",
            )
            nonblank(serving["label"], f"{where}.servings[{sidx}].label")
            positive_or_null(serving["grams"], f"{where}.servings[{sidx}].grams")
            if serving["grams"] is not None:
                resolved_serving = True
        if not resolved_serving:
            unresolved += 1

        nonblank(raw["sourceId"], f"{where}.sourceId")
        fail(raw["synthetic"] is True, f"{where}: checked-in catalog fixture must stay synthetic/test-only")
        fail(raw.get("mappingId") is None, f"{where}: synthetic fixture must not claim a real source mapping")
        nonblank(raw["note"], f"{where}.note")
    return len(entries), unresolved


def validate_sources(payload: Any) -> int:
    payload = exact_keys(
        payload,
        {
            "schemaVersion", "jurisdiction", "observedAt", "defaultPolicy", "medicalAdvice",
            "networkCaptureInCi", "note", "licenses", "sources",
        },
        {
            "schemaVersion", "jurisdiction", "observedAt", "defaultPolicy", "medicalAdvice",
            "networkCaptureInCi", "note", "licenses", "sources",
        },
        "sourceCandidates",
    )
    fail(payload["schemaVersion"] == 1, "sourceCandidates.schemaVersion must be 1")
    fail(payload["jurisdiction"] == "TW", "sourceCandidates.jurisdiction must be TW")
    fail(payload["defaultPolicy"] == "DENY", "sourceCandidates.defaultPolicy must remain DENY")
    fail(payload["medicalAdvice"] is False, "sourceCandidates.medicalAdvice must remain false")
    fail(payload["networkCaptureInCi"] is False, "sourceCandidates.networkCaptureInCi must remain false")
    try:
        date.fromisoformat(payload["observedAt"])
    except (TypeError, ValueError) as error:
        raise ContractError("sourceCandidates.observedAt must be YYYY-MM-DD") from error
    nonblank(payload["note"], "sourceCandidates.note")

    licenses = payload["licenses"]
    fail(isinstance(licenses, list) and licenses, "sourceCandidates.licenses must be non-empty")
    license_ids: set[str] = set()
    for index, license_entry in enumerate(licenses):
        where = f"sourceCandidates.licenses[{index}]"
        license_entry = exact_keys(
            license_entry,
            {"id", "title", "canonicalUrl", "observedPermissionBoundary", "productionAdmission"},
            {"id", "title", "canonicalUrl", "observedPermissionBoundary", "productionAdmission"},
            where,
        )
        license_id = nonblank(license_entry["id"], f"{where}.id")
        fail(license_id not in license_ids, f"{where}: duplicate license id {license_id}")
        license_ids.add(license_id)
        nonblank(license_entry["title"], f"{where}.title")
        url = nonblank(license_entry["canonicalUrl"], f"{where}.canonicalUrl")
        fail(url.startswith("https://"), f"{where}.canonicalUrl must use HTTPS")
        nonblank(license_entry["observedPermissionBoundary"], f"{where}.observedPermissionBoundary")
        fail(license_entry["productionAdmission"] in LICENSE_STATES, f"{where}: unsafe productionAdmission")

    sources = payload["sources"]
    fail(isinstance(sources, list) and sources, "sourceCandidates.sources must be non-empty")
    source_ids: set[str] = set()
    allowed_source_keys = {
        "id", "publisher", "title", "sourceType", "canonicalUrl", "datasetId", "licenseId",
        "snapshotState", "snapshotId", "snapshotSha256", "archiveUri", "productionUse",
        "modelGenerated", "allowedResearchUse", "prohibitedInference", "verificationNote",
    }
    for index, source in enumerate(sources):
        where = f"sourceCandidates.sources[{index}]"
        source = exact_keys(source, allowed_source_keys, allowed_source_keys, where)
        source_id = nonblank(source["id"], f"{where}.id")
        fail(source_id not in source_ids, f"{where}: duplicate source id {source_id}")
        source_ids.add(source_id)
        nonblank(source["publisher"], f"{where}.publisher")
        nonblank(source["title"], f"{where}.title")
        fail(source["sourceType"] in SOURCE_TYPES, f"{where}: unsupported sourceType")
        url = nonblank(source["canonicalUrl"], f"{where}.canonicalUrl")
        fail(url.startswith("https://"), f"{where}.canonicalUrl must use HTTPS")
        fail(source["licenseId"] in license_ids, f"{where}: unknown licenseId")
        fail(source["datasetId"] is None, f"{where}: example must not claim an exact dataset id")
        fail(source["snapshotState"] == "CANDIDATE", f"{where}: example source must stay CANDIDATE")
        for field in ("snapshotId", "snapshotSha256", "archiveUri"):
            fail(source[field] is None, f"{where}.{field} must remain null before capture")
        fail(source["productionUse"] == "DENY", f"{where}: productionUse must remain DENY")
        fail(source["modelGenerated"] is False, f"{where}: modelGenerated source evidence is forbidden")
        nonblank(source["allowedResearchUse"], f"{where}.allowedResearchUse")
        nonblank(source["prohibitedInference"], f"{where}.prohibitedInference")
        nonblank(source["verificationNote"], f"{where}.verificationNote")
    return len(sources)


def validate_all(catalog: Any, schema: Any, sources: Any) -> tuple[int, int, int]:
    validate_schema(schema)
    entries, unresolved = validate_catalog(catalog)
    source_count = validate_sources(sources)
    return entries, unresolved, source_count


def mutation_matrix(catalog: Any, schema: Any, sources: Any) -> list[tuple[str, Any, Any, Any]]:
    cases: list[tuple[str, Any, Any, Any]] = []

    def mutated(name: str, target: str, mutate: Callable[[Any], None]) -> None:
        c, s, r = copy.deepcopy(catalog), copy.deepcopy(schema), copy.deepcopy(sources)
        obj = {"catalog": c, "schema": s, "sources": r}[target]
        mutate(obj)
        cases.append((name, c, s, r))

    mutated("catalog-default-policy", "catalog", lambda x: x.__setitem__("defaultPolicy", "ALLOW"))
    mutated("catalog-extra-root-field", "catalog", lambda x: x.__setitem__("productionAdmitted", True))
    mutated(
        "duplicate-locale",
        "catalog",
        lambda x: x["entries"][0]["identity"]["names"][1].__setitem__("locale", "ZH_TW"),
    )
    mutated(
        "negative-nutrient",
        "catalog",
        lambda x: x["entries"][0]["profile"].__setitem__("proteinGPer100g", -1),
    )
    mutated("non-synthetic-fixture", "catalog", lambda x: x["entries"][0].__setitem__("synthetic", False))
    mutated(
        "schema-policy-drift",
        "schema",
        lambda x: x["properties"]["defaultPolicy"].__setitem__("const", "ALLOW"),
    )
    mutated(
        "source-promoted",
        "sources",
        lambda x: x["sources"][0].__setitem__("snapshotState", "HASH_VERIFIED"),
    )
    mutated(
        "source-model-generated",
        "sources",
        lambda x: x["sources"][0].__setitem__("modelGenerated", True),
    )
    mutated(
        "candidate-has-hash",
        "sources",
        lambda x: x["sources"][0].__setitem__("snapshotSha256", "0" * 64),
    )
    mutated("ci-network-capture", "sources", lambda x: x.__setitem__("networkCaptureInCi", True))
    return cases


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true", help="plant defects and require every one to fail")
    args = parser.parse_args(argv)

    try:
        catalog, schema, sources = load_json(CATALOG), load_json(SCHEMA), load_json(SOURCES)
        entries, unresolved, source_count = validate_all(catalog, schema, sources)
        print(
            f"PASS nutrition fixture contract: entries={entries}, "
            f"unresolved_servings={unresolved}, source_candidates={source_count}"
        )
        if args.self_test:
            failures: list[str] = []
            for name, c, s, r in mutation_matrix(catalog, schema, sources):
                try:
                    validate_all(c, s, r)
                except ContractError:
                    print(f"PASS planted defect rejected: {name}")
                else:
                    failures.append(name)
            fail(not failures, f"self-test mutations escaped validation: {failures}")
            print(f"PASS self-test: {len(mutation_matrix(catalog, schema, sources))} planted defects rejected")
    except ContractError as error:
        print(f"FAIL {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
