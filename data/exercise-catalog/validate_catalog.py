#!/usr/bin/env python3
"""Fail-closed checks for the exercise taxonomy, top-50 catalog, and media intake fixture.

Standard library only, no network, no build. Issues #32, #33, #34, #48.

What this gate is for
---------------------
The Kotlin unit tests own the pure decision logic. This gate owns the facts that live *between*
artifacts and that no unit test can see:

* the Kotlin taxonomy and `taxonomy.v1.json` say the same words;
* every muscle region this repository claims to draw actually exists in the SVG on disk;
* the 50 authored records only use vocabulary that exists;
* no record, and no media row, has been hand-edited into an evidence state stronger than the
  external gate that would have to authorize it.

Run:
    python3 data/exercise-catalog/validate_catalog.py
    python3 data/exercise-catalog/validate_catalog.py --selftest
"""

from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
KOTLIN = ROOT / "shared/src/commonMain/kotlin/dev/ed3c/gymcometrue/catalog"

TAXONOMY_KT = KOTLIN / "ExerciseTaxonomy.kt"
VISUALIZATION_KT = KOTLIN / "MuscleVisualization.kt"
CATALOG_KT = KOTLIN / "ExerciseCatalog.kt"
PROVENANCE_KT = KOTLIN / "ExerciseProvenance.kt"

TAXONOMY_JSON = HERE / "taxonomy.v1.json"
CATALOG_JSON = HERE / "catalog.v1.json"
MEDIA_JSON = HERE / "media-intake.synthetic.json"
PROVENANCE_JSON = ROOT / "legal/provenance/exercise-catalog-v1.json"

EXPECTED_RECORD_COUNT = 50
SLUG = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
SHA256 = re.compile(r"^[0-9a-f]{64}$")
URL_LIKE = re.compile(r"(?i)(https?://|//[a-z0-9-]+\.[a-z]{2,}|www\.)")

VOCABULARY_ENUMS = {
    "muscle": "MuscleGroup",
    "movementPattern": "MovementPattern",
    "mechanics": "Mechanics",
    "force": "ForceVector",
    "laterality": "Laterality",
    "skillLevel": "SkillLevel",
    "equipment": "EquipmentClass",
    "intensity": "ActivationIntensity",
}

REQUIRED_LOCALES = ("en", "zh-Hant-TW")

# States a repository with no executed rights and no human reviewer may legitimately reach.
MAX_MEDIA_STATE = {"INTAKE", "QUARANTINED", "HASH_VERIFIED"}
MAX_CONTENT_REVIEW_STATE = {"DRAFT"}


class ValidationError(RuntimeError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


def load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as error:
        raise ValidationError(f"Missing required file: {path.relative_to(ROOT)}") from error
    except json.JSONDecodeError as error:
        raise ValidationError(f"Invalid JSON in {path.relative_to(ROOT)}: {error}") from error


# --------------------------------------------------------------------------- Kotlin source facts


def kotlin_enum(source: str, name: str) -> list[str]:
    """Enum members by brace matching. A regex over a Kotlin body mis-nests on single-line enums."""
    head = re.search(rf"\benum class {name}\b[^{{]*\{{", source)
    require(head is not None, f"Kotlin enum {name} not found")
    depth, index = 1, head.end()
    while depth:
        require(index < len(source), f"Unbalanced braces around enum {name}")
        char = source[index]
        depth += (char == "{") - (char == "}")
        index += 1
    body = source[head.end():index - 1].split(";")[0]
    members: list[str] = []
    for chunk in re.split(r",(?![^(]*\))", body):
        member = re.match(r"\s*([A-Z][A-Z0-9_]*)\s*(\(|$)", chunk)
        if member:
            members.append(member.group(1))
    require(bool(members), f"Kotlin enum {name} resolved to no members")
    return members


def kotlin_muscle_labels(source: str) -> dict[str, dict[str, str]]:
    pairs = re.findall(r"MuscleGroup\.([A-Z_]+) to bilingual\(\"([^\"]+)\", \"([^\"]+)\"\)", source)
    return {muscle: {"en": en, "zh-Hant-TW": zh} for muscle, en, zh in pairs}


def kotlin_regions(source: str) -> list[dict[str, Any]]:
    found = re.findall(
        r"region\(MuscleGroup\.([A-Z_]+), BodyView\.(FRONT|BACK), ((?:\"[a-z-]+\"(?:, )?)+)\)",
        source,
    )
    return [
        {"muscle": muscle, "view": view, "svgRegionIds": re.findall(r"\"([a-z-]+)\"", ids)}
        for muscle, view, ids in found
    ]


def kotlin_banned_phrases(source: str) -> list[str]:
    block = re.search(r"bannedPhrases: List<String> = listOf\((.*?)\n    \)", source, re.S)
    require(block is not None, "MedicalClaimScreen.bannedPhrases not found")
    return re.findall(r"\"([^\"]+)\"", block.group(1))


# --------------------------------------------------------------------------- checks


def check_taxonomy_matches_kotlin(taxonomy: dict[str, Any]) -> None:
    source = TAXONOMY_KT.read_text(encoding="utf-8")
    vocabulary = taxonomy.get("vocabulary")
    require(isinstance(vocabulary, dict), "taxonomy.v1.json is missing a vocabulary object")
    require(
        set(vocabulary) == set(VOCABULARY_ENUMS),
        f"Vocabulary keys drifted: {sorted(set(vocabulary) ^ set(VOCABULARY_ENUMS))}",
    )
    for key, enum_name in VOCABULARY_ENUMS.items():
        expected = kotlin_enum(source, enum_name)
        require(
            vocabulary[key] == expected,
            f"Vocabulary '{key}' drifted from Kotlin {enum_name}: "
            f"json={vocabulary[key]} kotlin={expected}",
        )

    expected_labels = kotlin_muscle_labels(source)
    require(
        taxonomy.get("muscleLabels") == expected_labels,
        "taxonomy.v1.json muscleLabels drifted from Kotlin MuscleLabels",
    )
    for muscle in vocabulary["muscle"]:
        entry = expected_labels.get(muscle, {})
        for locale in REQUIRED_LOCALES:
            require(bool(entry.get(locale)), f"Muscle {muscle} has no {locale} label")


def check_visualization_binding(taxonomy: dict[str, Any]) -> None:
    visualization = taxonomy.get("visualization")
    require(isinstance(visualization, dict), "taxonomy.v1.json is missing the visualization block")
    require(
        visualization.get("anatomicallyValidated") is False
        and visualization.get("diagnostic") is False,
        "The schematic must never claim anatomical validation or diagnostic use",
    )

    expected_regions = kotlin_regions(VISUALIZATION_KT.read_text(encoding="utf-8"))
    require(
        visualization.get("regions") == expected_regions,
        "taxonomy.v1.json regions drifted from Kotlin MuscleRegionMap",
    )

    asset = ROOT / visualization["assetPath"]
    require(asset.is_file(), f"Muscle map asset is missing: {visualization['assetPath']}")
    svg = asset.read_text(encoding="utf-8")

    svg_bindings: dict[str, str] = dict(
        re.findall(r'id="(muscle-[a-z-]+)"[^>]*data-muscle="([A-Z_]+)"', svg)
    )
    require(bool(svg_bindings), "The muscle map asset exposes no region bindings")

    mapped_ids: set[str] = set()
    for region in expected_regions:
        for region_id in region["svgRegionIds"]:
            require(
                region_id in svg_bindings,
                f"Region {region_id} is mapped in Kotlin but absent from the asset",
            )
            require(
                svg_bindings[region_id] == region["muscle"],
                f"Region {region_id} draws {svg_bindings[region_id]} "
                f"but is mapped to {region['muscle']}",
            )
            mapped_ids.add(region_id)
    unmapped = sorted(set(svg_bindings) - mapped_ids)
    require(not unmapped, f"The asset carries regions no map claims: {unmapped}")

    rendered = {region["muscle"] for region in expected_regions}
    expected_unrenderable = sorted(set(taxonomy["vocabulary"]["muscle"]) - rendered)
    require(
        visualization.get("unrenderableMuscles") == expected_unrenderable,
        f"unrenderableMuscles must be exactly {expected_unrenderable}",
    )

    semantics = taxonomy.get("intensitySemantics", {})
    for name, level, opacity in (("PRIMARY", 3, 0.9), ("SECONDARY", 2, 0.6), ("STABILIZER", 1, 0.3)):
        entry = semantics.get(name, {})
        require(
            entry.get("level") == level and abs(entry.get("opacity", -1) - opacity) < 1e-9,
            f"Intensity semantics for {name} must be level {level} and opacity {opacity}",
        )
    require(
        "not an emg measurement" in semantics.get("claim", "").lower(),
        "Intensity semantics must disclaim any physiological measurement",
    )


def always_required_provenance() -> set[str]:
    """Derived from the Kotlin enum so the two lists cannot disagree about what a record must carry."""
    fields = set(kotlin_enum(PROVENANCE_KT.read_text(encoding="utf-8"), "CatalogField"))
    require("MEDIA" in fields, "CatalogField no longer declares MEDIA")
    return fields - {"MEDIA"}


def check_catalog(catalog: dict[str, Any], taxonomy: dict[str, Any]) -> None:
    vocabulary = taxonomy["vocabulary"]
    banned = kotlin_banned_phrases(CATALOG_KT.read_text(encoding="utf-8"))
    require(bool(banned), "The medical-claim screen has an empty phrase list")
    required_provenance = always_required_provenance()

    require(catalog.get("schemaVersion") == 1, "Catalog schemaVersion must be 1")
    records = catalog.get("records")
    require(isinstance(records, list), "Catalog has no records array")
    require(
        len(records) == EXPECTED_RECORD_COUNT,
        f"Catalog must hold {EXPECTED_RECORD_COUNT} records, found {len(records)}",
    )

    gates = catalog.get("externalGates", {})
    for gate in ("editorialReview", "rightsReview", "clinicalReview", "media"):
        require(
            gates.get(gate) == "ABSENT",
            f"External gate '{gate}' must be recorded as ABSENT; it has not been executed",
        )

    safety_notes = catalog.get("safetyNotes", {})
    require(bool(safety_notes), "Catalog carries no shared safety notes")
    for ref, localized in safety_notes.items():
        for locale in REQUIRED_LOCALES:
            require(
                bool(localized.get(locale, "").strip()),
                f"Safety note {ref} is missing {locale}",
            )

    seen_ids: set[str] = set()
    seen_slugs: set[str] = set()
    for record in records:
        where = record.get("id", "<no id>")
        slug = record.get("slug", "")
        require(SLUG.match(slug) is not None, f"Record {where} has a malformed slug '{slug}'")
        require(record.get("id") == f"gct-{slug}", f"Record {where} id must be gct-{slug}")
        require(where not in seen_ids, f"Duplicate exercise id {where}")
        require(slug not in seen_slugs, f"Duplicate exercise slug {slug}")
        seen_ids.add(where)
        seen_slugs.add(slug)

        for field, values in (("movementPattern", "movementPattern"), ("mechanics", "mechanics"),
                              ("force", "force"), ("laterality", "laterality"),
                              ("skillLevel", "skillLevel")):
            token = record.get(field)
            require(
                token in vocabulary[values],
                f"Record {where} uses unknown {field} token '{token}'",
            )

        equipment = record.get("equipment", [])
        require(bool(equipment), f"Record {where} declares no equipment")
        require(len(set(equipment)) == len(equipment), f"Record {where} repeats an equipment class")
        for token in equipment:
            require(
                token in vocabulary["equipment"],
                f"Record {where} uses unknown equipment token '{token}'",
            )

        engagement = record.get("muscleEngagement", [])
        require(bool(engagement), f"Record {where} declares no muscle engagement")
        muscles = [entry["muscle"] for entry in engagement]
        require(len(set(muscles)) == len(muscles), f"Record {where} repeats a muscle")
        for entry in engagement:
            require(
                entry["muscle"] in vocabulary["muscle"],
                f"Record {where} uses unknown muscle token '{entry['muscle']}'",
            )
            require(
                entry["intensity"] in vocabulary["intensity"],
                f"Record {where} uses unknown intensity token '{entry['intensity']}'",
            )
        require(
            any(entry["intensity"] == "PRIMARY" for entry in engagement),
            f"Record {where} declares no PRIMARY muscle",
        )

        for field in ("name", "summary"):
            localized = record.get(field, {})
            require(
                set(localized) == set(REQUIRED_LOCALES),
                f"Record {where}.{field} must carry exactly {list(REQUIRED_LOCALES)}",
            )
            for locale, text in localized.items():
                require(bool(text.strip()), f"Record {where}.{field} is blank for {locale}")
                check_text(where, field, locale, text, banned)

        for field, low, high in (("steps", 3, 8), ("commonErrors", 1, 5)):
            localized = record.get(field, {})
            require(
                set(localized) == set(REQUIRED_LOCALES),
                f"Record {where}.{field} must carry exactly {list(REQUIRED_LOCALES)}",
            )
            sizes = {len(lines) for lines in localized.values()}
            require(
                len(sizes) == 1,
                f"Record {where}.{field} has a different entry count per locale: {sizes}",
            )
            size = sizes.pop()
            require(
                low <= size <= high,
                f"Record {where}.{field} must hold {low}..{high} entries, found {size}",
            )
            for locale, lines in localized.items():
                for line in lines:
                    require(bool(line.strip()), f"Record {where}.{field} has a blank {locale} entry")
                    check_text(where, field, locale, line, banned)

        require(
            record.get("safetyNoteRef") in safety_notes,
            f"Record {where} references unknown safety note {record.get('safetyNoteRef')}",
        )
        require(
            record.get("mediaRefs") == [],
            f"Record {where} cites media, but no media asset has been admitted",
        )
        check_provenance(where, record.get("fieldProvenance", []), required_provenance)


def check_text(where: str, field: str, locale: str, text: str, banned: list[str]) -> None:
    haystack = text.lower()
    for phrase in banned:
        require(
            phrase.lower() not in haystack,
            f"Record {where}.{field} ({locale}) contains claim phrase '{phrase}'",
        )
    require(
        URL_LIKE.search(text) is None,
        f"Record {where}.{field} ({locale}) contains a URL; catalog text carries no links",
    )


def check_provenance(where: str, provenance: list[dict[str, Any]], required: set[str]) -> None:
    require(bool(provenance), f"Record {where} carries no per-field provenance")
    fields = [entry.get("field") for entry in provenance]
    require(len(set(fields)) == len(fields), f"Record {where} repeats a provenance field")
    require(
        set(fields) == required,
        f"Record {where} provenance fields must be exactly {sorted(required)}, "
        f"found {sorted(f for f in fields if f)}",
    )
    for entry in provenance:
        field = entry.get("field")
        require(
            entry.get("licenseGrant") != "REPOSITORY_ROOT_LICENSE",
            f"Record {where} field {field} cites the repository-root licence, "
            "which never authorizes a record",
        )
        require(
            entry.get("licenseGrant") == "FIRST_PARTY_OWNERSHIP",
            f"Record {where} field {field} must rest on first-party ownership",
        )
        require(
            entry.get("authorship") in {"FIRST_PARTY_HUMAN_ORIGINAL", "FIRST_PARTY_AGENT_DRAFTED"},
            f"Record {where} field {field} has non first-party authorship "
            f"'{entry.get('authorship')}'",
        )
        require(
            entry.get("reviewState") in MAX_CONTENT_REVIEW_STATE,
            f"Record {where} field {field} claims review state '{entry.get('reviewState')}'; "
            "no editorial or rights review has been executed on this repository",
        )
        require(
            bool(entry.get("provenanceRecordId")),
            f"Record {where} field {field} has no provenance record id",
        )


def check_media_fixture(media: dict[str, Any]) -> None:
    records = media.get("records", [])
    require(bool(records), "Media intake fixture holds no records")
    for gate in ("executedCommercialRights", "commissionedFirstPartyArtwork", "legalReview"):
        require(
            media.get("externalGates", {}).get(gate) == "ABSENT",
            f"Media external gate '{gate}' must be recorded as ABSENT",
        )
    for record in records:
        media_id = record.get("mediaId", "<no id>")
        state = record.get("state")
        require(
            state in MAX_MEDIA_STATE,
            f"Media {media_id} is in state {state}; this repository holds no executed rights "
            "and no reviewer attestation, so nothing may pass HASH_VERIFIED",
        )
        require(
            record.get("productionAdmitted") is False,
            f"Media {media_id} self-declares productionAdmitted",
        )
        require(record.get("remoteUrl") is None, f"Media {media_id} carries a remote URL")
        require(
            record.get("reviewerAttestationSha256") is None,
            f"Media {media_id} carries a reviewer attestation that no reviewer produced",
        )
        require(bool(record.get("revocationKey")), f"Media {media_id} has no revocation key")

        if state == "HASH_VERIFIED":
            sha = record.get("originSha256")
            require(
                isinstance(sha, str) and SHA256.match(sha) is not None,
                f"Media {media_id} is hash verified without a 64-hex SHA-256",
            )
            storage = record.get("storageUri") or ""
            require(
                storage.startswith(("repo://", "evidence://")) and sha in storage,
                f"Media {media_id} needs a content-addressed repo:// or evidence:// storage URI",
            )
            target = record.get("verifiedAgainst")
            require(
                bool(target),
                f"Media {media_id} claims hash verification with nothing to verify against",
            )
            asset = ROOT / target
            require(asset.is_file(), f"Media {media_id} verifies against a missing file: {target}")
            actual = hashlib.sha256(asset.read_bytes()).hexdigest()
            require(
                actual == sha,
                f"Media {media_id} hash does not match {target}: recorded={sha} actual={actual}",
            )
            require(
                record.get("byteLength") == asset.stat().st_size,
                f"Media {media_id} byte length does not match {target}",
            )
            for locale in REQUIRED_LOCALES:
                require(
                    bool(record.get("altText", {}).get(locale, "").strip()),
                    f"Media {media_id} is missing {locale} alternative text",
                )


def check_catalog_provenance_record() -> None:
    record = load_json(PROVENANCE_JSON)
    license_record = record.get("license_record", {})
    require(
        license_record.get("decision") == "PENDING_REVIEW",
        "The catalog provenance record must stay PENDING_REVIEW until a human accepts it",
    )
    require(
        license_record.get("reviewer") is None,
        "The catalog provenance record must not name a reviewer that has not reviewed it",
    )
    require(
        license_record.get("authorship_method") == "FIRST_PARTY_AGENT_DRAFTED",
        "The catalog provenance record must state that the text was agent drafted",
    )


# --------------------------------------------------------------------------- self-test


def selftest() -> int:
    """Plant one defect at a time and assert the gate goes red for each."""
    taxonomy = load_json(TAXONOMY_JSON)
    catalog = load_json(CATALOG_JSON)
    media = load_json(MEDIA_JSON)

    def mutate_unknown_muscle(data: dict[str, Any]) -> None:
        data["records"][0]["muscleEngagement"][0]["muscle"] = "GLUTEUS_MAXIMUS_2"

    def mutate_duplicate_id(data: dict[str, Any]) -> None:
        data["records"][1]["id"] = data["records"][0]["id"]
        data["records"][1]["slug"] = data["records"][0]["slug"]

    def mutate_repo_licence(data: dict[str, Any]) -> None:
        data["records"][0]["fieldProvenance"][0]["licenseGrant"] = "REPOSITORY_ROOT_LICENSE"

    def mutate_missing_locale(data: dict[str, Any]) -> None:
        data["records"][0]["steps"].pop("zh-Hant-TW")

    def mutate_claim_phrase(data: dict[str, Any]) -> None:
        data["records"][0]["summary"]["en"] += " This movement prevents injury."

    def mutate_hotlink(data: dict[str, Any]) -> None:
        data["records"][0]["summary"]["en"] += " See https://example.test/demo.gif"

    def mutate_fabricated_review(data: dict[str, Any]) -> None:
        data["records"][0]["fieldProvenance"][0]["reviewState"] = "ADMITTED"

    def mutate_media_claim(data: dict[str, Any]) -> None:
        data["records"][0]["mediaRefs"] = ["media-does-not-exist"]

    def mutate_gate(data: dict[str, Any]) -> None:
        data["externalGates"]["rightsReview"] = "REVIEWED"

    def mutate_record_count(data: dict[str, Any]) -> None:
        data["records"].pop()

    catalog_defects = [
        ("unknown taxonomy token", mutate_unknown_muscle),
        ("duplicate exercise id", mutate_duplicate_id),
        ("repository-root licence", mutate_repo_licence),
        ("missing locale", mutate_missing_locale),
        ("medical claim phrase", mutate_claim_phrase),
        ("embedded hotlink", mutate_hotlink),
        ("fabricated review state", mutate_fabricated_review),
        ("unadmitted media reference", mutate_media_claim),
        ("external gate marked reviewed", mutate_gate),
        ("short record count", mutate_record_count),
    ]

    def mutate_region(data: dict[str, Any]) -> None:
        data["visualization"]["regions"][0]["svgRegionIds"] = ["muscle-front-does-not-exist"]

    def mutate_vocabulary(data: dict[str, Any]) -> None:
        data["vocabulary"]["muscle"].append("INVENTED_MUSCLE")

    def mutate_diagnostic_claim(data: dict[str, Any]) -> None:
        data["visualization"]["anatomicallyValidated"] = True

    taxonomy_defects = [
        ("region absent from the asset", mutate_region),
        ("vocabulary drift from Kotlin", mutate_vocabulary),
        ("anatomical-validation claim", mutate_diagnostic_claim),
    ]

    def mutate_media_state(data: dict[str, Any]) -> None:
        data["records"][0]["state"] = "ADMITTED"

    def mutate_media_hash(data: dict[str, Any]) -> None:
        data["records"][0]["originSha256"] = "0" * 64

    def mutate_media_admitted(data: dict[str, Any]) -> None:
        data["records"][0]["productionAdmitted"] = True

    def mutate_media_attestation(data: dict[str, Any]) -> None:
        data["records"][0]["reviewerAttestationSha256"] = "a" * 64

    media_defects = [
        ("fabricated media admission", mutate_media_state),
        ("hash that does not match the bytes", mutate_media_hash),
        ("self-declared production admission", mutate_media_admitted),
        ("fabricated reviewer attestation", mutate_media_attestation),
    ]

    failures: list[str] = []

    def expect_red(label: str, run) -> None:
        try:
            run()
        except ValidationError:
            print(f"PASS selftest rejects {label}")
            return
        failures.append(label)
        print(f"FAIL selftest accepted {label}", file=sys.stderr)

    for label, mutate in catalog_defects:
        broken = json.loads(json.dumps(catalog))
        mutate(broken)
        expect_red(label, lambda broken=broken: check_catalog(broken, taxonomy))

    for label, mutate in taxonomy_defects:
        broken = json.loads(json.dumps(taxonomy))
        mutate(broken)
        expect_red(
            label,
            lambda broken=broken: (
                check_taxonomy_matches_kotlin(broken),
                check_visualization_binding(broken),
            ),
        )

    for label, mutate in media_defects:
        broken = json.loads(json.dumps(media))
        mutate(broken)
        expect_red(label, lambda broken=broken: check_media_fixture(broken))

    if failures:
        print(f"FAIL {len(failures)} planted defects were not detected: {failures}", file=sys.stderr)
        return 1
    print(f"PASS selftest detected all {len(catalog_defects) + len(taxonomy_defects) + len(media_defects)} planted defects")
    return 0


def main(argv: list[str]) -> int:
    if "--selftest" in argv:
        try:
            return selftest()
        except ValidationError as error:
            print(f"FAIL selftest could not run against clean inputs: {error}", file=sys.stderr)
            return 1

    try:
        taxonomy = load_json(TAXONOMY_JSON)
        catalog = load_json(CATALOG_JSON)
        media = load_json(MEDIA_JSON)

        check_taxonomy_matches_kotlin(taxonomy)
        print("PASS taxonomy matches the Kotlin vocabulary")
        check_visualization_binding(taxonomy)
        print("PASS muscle regions match the first-party asset")
        check_catalog(catalog, taxonomy)
        print(f"PASS catalog holds {len(catalog['records'])} rights-clean draft records")
        check_media_fixture(media)
        print("PASS media intake fixture stays inside its evidence ceiling")
        check_catalog_provenance_record()
        print("PASS catalog provenance record claims no review that did not happen")
    except ValidationError as error:
        print(f"FAIL {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
