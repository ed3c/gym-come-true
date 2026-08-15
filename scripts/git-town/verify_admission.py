#!/usr/bin/env python3
"""Fail-closed verifier for the pinned Git Town candidate admission packet.

The default mode validates repository-owned metadata only. Optional archive and binary
arguments strengthen the receipt but never set runtimeAdmitted or authorize use in the
consumer repository. Live stack, conflict, worktree, lease, publication, merge, ship,
and release evidence remain separate lanes.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
import re
import stat
import subprocess
import sys
import tarfile
import tempfile
from dataclasses import dataclass
from datetime import date
from pathlib import Path
from typing import Any, NoReturn

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_CANDIDATE = ROOT / "docs/git/admission/git-town-v24.0.0-linux-x86_64.json"
WORKFLOW = ROOT / ".github/workflows/git-town-admission.yml"
CANARY = ROOT / "scripts/git-town/run_disposable_canary.sh"
EXPECTED_ACTION_PINS = (
    "actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1",
    "actions/setup-python@5fda3b95a4ea91299a34e894583c3862153e4b97",
    "actions/upload-artifact@043fb46d1a93c77aae656e7c1c64a875d1fc6a0a",
)
SHA256 = re.compile(r"^[0-9a-f]{64}$")
COMMIT_SHA = re.compile(r"^[0-9a-f]{40}$")
HTTPS = re.compile(r"^https://[^\s]+$")
EXPECTED = {
    "schema": "gym-come-true/git-town-admission-candidate/v1",
    "candidateState": "CANDIDATE_METADATA_VERIFIED",
    "releaseRepository": "git-town/git-town",
    "releaseId": 358702660,
    "tag": "v24.0.0",
    "tagCommit": "0f3e55f5a6bae5b319dd713a0606263d0551af66",
    "archiveAssetId": 487215105,
    "archiveName": "git-town_linux_intel_64.tar.gz",
    "archiveSize": 7640994,
    "archiveSha256": "0ed4936f010b42db2ef573e4b2abd951289f4980d95b8236a619429e2501cbc7",
    "archiveUrl": "https://github.com/git-town/git-town/releases/download/v24.0.0/git-town_linux_intel_64.tar.gz",
    "checksumsAssetId": 487215219,
    "checksumsSha256": "7532377166cb59dc01c74f86e3a71c54ba9567a461313a5d203a1ea99c571b24",
    "checksumsUrl": "https://github.com/git-town/git-town/releases/download/v24.0.0/checksums.txt",
    "licenseSha256": "eec8a092b92231375231488d27b959e2fa2be80559c97db60c1b0458d3298791",
    "goModSha256": "5a7627e581f45c29750ceef8116ee0bdf61f0c36ead5b31d8f1f3fe33753c721",
}
NOT_EXERCISED = {"NOT_EXERCISED", "ABSENT", "NOT_IMPLEMENTED", "REVIEW_REQUIRED"}


class AdmissionError(RuntimeError):
    """Raised when candidate evidence or optional runtime input fails closed."""


def fail(message: str) -> NoReturn:
    raise AdmissionError(message)


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def load_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        fail(f"cannot load candidate {path}: {error}")
    require(isinstance(value, dict), "candidate root must be an object")
    return value


def nested(payload: dict[str, Any], *keys: str) -> Any:
    current: Any = payload
    for key in keys:
        require(isinstance(current, dict) and key in current, f"missing field: {'.'.join(keys)}")
        current = current[key]
    return current


def is_iso_date(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    try:
        return date.fromisoformat(value).isoformat() == value
    except ValueError:
        return False


def validate_candidate(payload: dict[str, Any]) -> None:
    require(payload.get("schema") == EXPECTED["schema"], "unexpected candidate schema")
    require(payload.get("candidateState") == EXPECTED["candidateState"], "candidate state drift")
    require(payload.get("productionUse") == "DENY", "candidate must remain production DENY")
    require(payload.get("runtimeAdmitted") is False, "metadata cannot self-admit runtime")
    require(is_iso_date(payload.get("observedAt")), "observedAt must be a real ISO date")

    release = nested(payload, "release")
    require(release.get("repository") == EXPECTED["releaseRepository"], "upstream repository drift")
    require(release.get("releaseId") == EXPECTED["releaseId"], "release ID drift")
    require(release.get("tag") == EXPECTED["tag"], "release tag drift")
    require(release.get("tagCommit") == EXPECTED["tagCommit"], "tag commit drift")
    require(bool(COMMIT_SHA.fullmatch(str(release.get("tagCommit", "")))), "invalid tag commit")
    require(release.get("immutable") is True, "release must be marked immutable by upstream metadata")
    require(release.get("prerelease") is False, "prerelease is not admitted as this candidate")
    require(bool(HTTPS.fullmatch(str(release.get("apiUrl", "")))), "release API URL must be HTTPS")

    archive = nested(payload, "archive")
    require(archive.get("assetId") == EXPECTED["archiveAssetId"], "archive asset ID drift")
    require(archive.get("name") == EXPECTED["archiveName"], "archive name drift")
    require(archive.get("platform") == "linux", "candidate platform must be linux")
    require(archive.get("architecture") == "x86_64", "candidate architecture must be x86_64")
    require(archive.get("sizeBytes") == EXPECTED["archiveSize"], "archive size drift")
    require(archive.get("sha256") == EXPECTED["archiveSha256"], "archive SHA-256 drift")
    require(bool(SHA256.fullmatch(str(archive.get("sha256", "")))), "invalid archive SHA-256")
    require(archive.get("downloadUrl") == EXPECTED["archiveUrl"], "archive URL drift")
    require(bool(HTTPS.fullmatch(str(archive.get("downloadUrl", "")))), "archive URL must be HTTPS")
    require("latest" not in str(archive.get("downloadUrl", "")).lower(), "mutable latest URL is forbidden")

    checksums = nested(payload, "checksumsAsset")
    require(checksums.get("assetId") == EXPECTED["checksumsAssetId"], "checksums asset ID drift")
    require(checksums.get("name") == "checksums.txt", "checksums asset name drift")
    require(checksums.get("sizeBytes") == 1442, "checksums asset size drift")
    require(checksums.get("sha256") == EXPECTED["checksumsSha256"], "checksums asset digest drift")
    require(checksums.get("downloadUrl") == EXPECTED["checksumsUrl"], "checksums URL drift")
    require(bool(SHA256.fullmatch(str(checksums.get("sha256", "")))), "invalid checksums SHA-256")

    license_record = nested(payload, "license")
    require(license_record.get("spdx") == "MIT", "direct license SPDX drift")
    require(license_record.get("sourceRef") == EXPECTED["tag"], "license ref must match release tag")
    require(license_record.get("path") == "LICENSE", "unexpected license path")
    require(license_record.get("sha256") == EXPECTED["licenseSha256"], "license digest drift")
    require(license_record.get("reviewState") == "DIRECT_LICENSE_IDENTIFIED", "direct license state drift")
    license_path = safe_repo_evidence_path(license_record.get("localEvidencePath"), "license")
    license_digest, license_size = sha256_file(license_path)
    require(license_digest == license_record.get("sha256"), "local license bytes do not match recorded digest")
    require(license_size == license_record.get("byteLength"), "local license bytes do not match recorded length")

    dependencies = nested(payload, "dependencies")
    require(dependencies.get("manifestPath") == "go.mod", "dependency manifest drift")
    require(dependencies.get("goVersion") == "1.26.1", "Go version drift")
    require(dependencies.get("sourceRef") == EXPECTED["tag"], "go.mod ref must match release tag")
    require(dependencies.get("sha256") == EXPECTED["goModSha256"], "go.mod digest drift")
    require(dependencies.get("directModuleCount") == 24, "direct dependency count drift")
    require(dependencies.get("indirectModuleCount") == 39, "indirect dependency count drift")
    require(dependencies.get("sbomState") == "ABSENT", "SBOM must not be fabricated")
    require(dependencies.get("transitiveLicenseReview") == "REVIEW_REQUIRED", "transitive review must remain required")
    require(dependencies.get("noticesReview") == "REVIEW_REQUIRED", "notices review must remain required")
    go_mod_path = safe_repo_evidence_path(dependencies.get("localEvidencePath"), "go.mod")
    go_mod_digest, go_mod_size = sha256_file(go_mod_path)
    require(go_mod_digest == dependencies.get("sha256"), "local go.mod bytes do not match recorded digest")
    require(go_mod_size == dependencies.get("byteLength"), "local go.mod bytes do not match recorded length")

    provenance = nested(payload, "provenance")
    require(provenance.get("releaseImmutableMetadata") is True, "immutable release metadata evidence missing")
    require(provenance.get("assetDigestFromGitHubReleaseMetadata") is True, "asset digest source missing")
    require(provenance.get("artifactAttestation") == "ABSENT", "artifact attestation must not be fabricated")
    require(provenance.get("artifactSignature") == "ABSENT", "artifact signature must not be fabricated")
    require(provenance.get("organizationLegalApproval") == "ABSENT", "legal approval must remain external")

    runtime = nested(payload, "runtime")
    require(runtime.get("archiveMaterialized") is False, "repository candidate must not claim archive materialization")
    require(runtime.get("archiveHashVerified") is False, "repository candidate must not claim archive verification")
    require(runtime.get("binaryExtracted") is False, "repository candidate must not claim binary extraction")
    require(runtime.get("binarySha256") == "NOT_EXERCISED", "binary digest must remain not exercised")
    require(runtime.get("versionCommand") == "NOT_EXERCISED", "version command must remain not exercised")
    require(runtime.get("configState") == "NOT_IMPLEMENTED", "consumer config must remain absent")
    require(runtime.get("consumerRepositorySync") == "NOT_EXERCISED", "consumer sync must remain not exercised")

    canaries = nested(payload, "canaries")
    require(isinstance(canaries, dict) and canaries, "canary state map is required")
    for name, value in canaries.items():
        require(value in NOT_EXERCISED, f"candidate cannot fabricate canary success: {name}={value}")

    policy = nested(payload, "policy")
    for field in (
        "mutableLatestReference",
        "autoResolve",
        "defaultPush",
        "backgroundSyncEnabled",
        "publicationEnabled",
        "mergeEnabled",
        "shipEnabled",
    ):
        require(policy.get(field) is False, f"unsafe candidate policy: {field}")

    sources = payload.get("sources")
    require(isinstance(sources, list) and len(sources) >= 4, "candidate needs exact source references")
    for source in sources:
        require(bool(HTTPS.fullmatch(str(source))), f"non-HTTPS evidence source: {source}")
        require("@" not in str(source).split("//", 1)[-1].split("/", 1)[0], "credential-bearing evidence URL")


def validate_repository_surfaces() -> None:
    require(WORKFLOW.is_file() and not WORKFLOW.is_symlink(), "manual candidate workflow is missing or unsafe")
    require(CANARY.is_file() and not CANARY.is_symlink(), "disposable canary is missing or unsafe")
    require(not (ROOT / ".git-town.toml").exists(), "consumer .git-town.toml must remain absent")
    require(not (ROOT / "git-town.toml").exists(), "consumer git-town.toml must remain absent")

    workflow = WORKFLOW.read_text(encoding="utf-8")
    require("workflow_dispatch:" in workflow, "candidate workflow must be manual-only")
    require("EXECUTE_PINNED_CANDIDATE_IN_EPHEMERAL_RUNNER" in workflow, "manual acknowledgement is missing")
    require("pull_request:" not in workflow, "candidate workflow cannot run on pull_request")
    require("\n  push:" not in workflow, "candidate workflow cannot run on push")
    require("permissions:\n  contents: read" in workflow, "workflow permissions must be read-only")
    require("secrets." not in workflow, "candidate workflow cannot consume repository secrets")
    require("@v" not in workflow, "workflow actions must be pinned to exact commit SHAs")
    for action in EXPECTED_ACTION_PINS:
        require(action in workflow, f"workflow action pin drift: {action}")
    for token in (
        "--proto-redir '=https'",
        "sha256sum --check --strict",
        "--archive",
        "--binary",
        "run_disposable_canary.sh",
        "test ! -e .git-town.toml",
        "git diff --exit-code",
    ):
        require(token in workflow, f"workflow safety step is missing: {token}")

    canary = CANARY.read_text(encoding="utf-8")
    for token in (
        "mktemp -d",
        "git init --bare",
        "--stack --dry-run --non-interactive --no-auto-resolve --no-push",
        "--stack --non-interactive --no-auto-resolve --no-push",
        "remote_digest",
        "semanticConflictFailClosed",
        '"consumerRepositorySync": "NOT_EXERCISED"',
        '"publication": "NOT_EXERCISED"',
        '"runtimeAdmitted": false',
    ):
        require(token in canary, f"disposable canary invariant is missing: {token}")
    for invocation in (
        '"$binary" ship',
        '"$binary" continue',
        '"$binary" skip',
        '"$binary" undo',
    ):
        require(invocation not in canary, f"forbidden Git Town recovery/publication invocation: {invocation}")
    require("https://github.com/ed3c/gym-come-true" not in canary, "canary must not target the consumer remote")

def safe_repo_evidence_path(value: Any, label: str) -> Path:
    require(isinstance(value, str) and value, f"{label} local evidence path is required")
    relative = Path(value)
    require(not relative.is_absolute(), f"{label} local evidence path must be relative")
    require(".." not in relative.parts, f"{label} local evidence path cannot traverse parents")
    resolved = (ROOT / relative).resolve()
    require(resolved.is_relative_to(ROOT.resolve()), f"{label} evidence path leaves repository root")
    return resolved


def sha256_file(path: Path) -> tuple[str, int]:
    require(path.exists(), f"file does not exist: {path}")
    require(not path.is_symlink(), f"symlink input is forbidden: {path}")
    metadata = path.stat()
    require(stat.S_ISREG(metadata.st_mode), f"input is not a regular file: {path}")
    digest = hashlib.sha256()
    size = 0
    with path.open("rb") as stream:
        while chunk := stream.read(1024 * 1024):
            digest.update(chunk)
            size += len(chunk)
    return digest.hexdigest(), size


def verify_archive(path: Path, payload: dict[str, Any]) -> dict[str, Any]:
    digest, size = sha256_file(path)
    archive = nested(payload, "archive")
    require(digest == archive["sha256"], "archive bytes do not match pinned SHA-256")
    require(size == archive["sizeBytes"], "archive bytes do not match pinned length")
    with tarfile.open(path, mode="r:gz") as bundle:
        members = bundle.getmembers()
        require(bool(members), "archive is empty")
        binary_members = []
        for member in members:
            name = member.name
            normalized = Path(name)
            require(not normalized.is_absolute(), f"archive contains absolute path: {name}")
            require(".." not in normalized.parts, f"archive contains parent traversal: {name}")
            require(not member.issym() and not member.islnk(), f"archive contains link: {name}")
            if normalized.name == "git-town" and member.isfile():
                binary_members.append(name)
        require(len(binary_members) == 1, f"expected one git-town binary, found {binary_members}")
    return {
        "state": "ARCHIVE_VERIFIED",
        "sha256": digest,
        "byteLength": size,
        "binaryMember": binary_members[0],
    }


def verify_binary(path: Path, payload: dict[str, Any]) -> dict[str, Any]:
    digest, size = sha256_file(path)
    require(os.access(path, os.X_OK), f"binary is not executable: {path}")
    env = os.environ.copy()
    env.update(
        {
            "GIT_TERMINAL_PROMPT": "0",
            "GIT_EDITOR": ":",
            "GIT_SEQUENCE_EDITOR": ":",
            "GCM_INTERACTIVE": "Never",
        }
    )
    try:
        result = subprocess.run(
            [str(path), "--version"],
            capture_output=True,
            text=True,
            env=env,
            timeout=10,
            check=False,
        )
    except subprocess.TimeoutExpired as error:
        fail(f"version command timed out: {error}")
    output = (result.stdout + "\n" + result.stderr).strip()
    require(result.returncode == 0, f"version command failed with exit {result.returncode}: {output}")
    require(re.search(r"(?<!\d)24\.0\.0(?!\d)", output) is not None, f"unexpected version output: {output}")
    return {
        "state": "BINARY_VERSION_VERIFIED",
        "sha256": digest,
        "byteLength": size,
        "versionOutput": output,
        "releaseTag": nested(payload, "release", "tag"),
    }


def expect_mutation_failure(payload: dict[str, Any], path: tuple[str, ...], value: Any) -> None:
    mutated = copy.deepcopy(payload)
    target: Any = mutated
    for key in path[:-1]:
        target = target[key]
    target[path[-1]] = value
    try:
        validate_candidate(mutated)
    except AdmissionError:
        return
    fail(f"mutation control did not fail: {'.'.join(path)}={value!r}")


def run_self_test(payload: dict[str, Any]) -> dict[str, Any]:
    mutations = [
        (("release", "tag"), "latest"),
        (("release", "tagCommit"), "0" * 40),
        (("archive", "sha256"), "0" * 64),
        (("license", "sha256"), "1" * 64),
        (("license", "localEvidencePath"), "../../etc/passwd"),
        (("dependencies", "sbomState"), "PASS"),
        (("runtimeAdmitted",), True),
        (("runtime", "versionCommand"), "PASS"),
        (("canaries", "semanticConflictFailClosed"), "PASS"),
        (("policy", "autoResolve"), True),
        (("policy", "defaultPush"), True),
        (("policy", "backgroundSyncEnabled"), True),
        (("policy", "publicationEnabled"), True),
        (("policy", "shipEnabled"), True),
    ]
    for path, value in mutations:
        expect_mutation_failure(payload, path, value)
    return {"state": "PASS", "mutationControls": len(mutations)}


def write_receipt(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    require(not path.is_symlink(), f"receipt path cannot be a symlink: {path}")
    text = json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", dir=path.parent, delete=False) as stream:
        temporary = Path(stream.name)
        stream.write(text)
        stream.flush()
        os.fsync(stream.fileno())
    os.replace(temporary, path)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--candidate", type=Path, default=DEFAULT_CANDIDATE)
    parser.add_argument("--archive", type=Path)
    parser.add_argument("--binary", type=Path)
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--receipt", type=Path)
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        payload = load_json(args.candidate)
        validate_candidate(payload)
        validate_repository_surfaces()
        receipt: dict[str, Any] = {
            "schema": "gym-come-true/git-town-candidate-verification-receipt/v1",
            "candidate": str(args.candidate),
            "candidateFileSha256": sha256_file(args.candidate)[0],
            "metadata": "PASS",
            "repositorySurfaces": "PASS",
            "archive": "NOT_EXERCISED",
            "binary": "NOT_EXERCISED",
            "selfTest": "NOT_EXERCISED",
            "runtimeAdmitted": False,
            "productionUse": "DENY",
        }
        if args.archive:
            receipt["archive"] = verify_archive(args.archive, payload)
        if args.binary:
            receipt["binary"] = verify_binary(args.binary, payload)
        if args.self_test:
            receipt["selfTest"] = run_self_test(payload)
        if args.receipt:
            write_receipt(args.receipt, receipt)
        print(json.dumps(receipt, ensure_ascii=False, sort_keys=True))
        return 0
    except (AdmissionError, OSError, json.JSONDecodeError, tarfile.TarError) as error:
        print(f"FAIL {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
