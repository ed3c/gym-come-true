#!/usr/bin/env python3
"""Fail-closed offline checks for the machine-verifiable stacked delivery manifest.

The manifest is the machine projection of ``docs/git/STACKED_PRS.md``. This validator
never reaches the network and never trusts the manifest's own claims about the
repository: digests, template surfaces, and Git Town runtime state are recomputed
from repository bytes.

``--self-test`` proves the mutation controls actually deny: each planted mutation
must be rejected, otherwise this validator itself fails.
"""

from __future__ import annotations

import copy
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = "docs/git/stacked-delivery-manifest.json"
SCHEMA = "docs/git/schemas/stacked-delivery-manifest.schema.json"
NARRATIVE = "docs/git/STACKED_PRS.md"
ISSUE_TEMPLATE = ".github/ISSUE_TEMPLATE/work-packet.md"
PR_TEMPLATE = ".github/pull_request_template.md"

SCHEMA_ID = "gym-come-true/stacked-delivery-manifest/v1"
ROOT_PARENT = "main"
SHA1 = re.compile(r"^[0-9a-f]{40}$")
SHA256 = re.compile(r"^[0-9a-f]{64}$")
PACKET_ID = re.compile(r"^[A-Z]+[0-9]+$")
TRANSITION = re.compile(r"^[A-Z0-9_]+ -> [A-Z0-9_]+$")
HEAD_BRANCH = re.compile(r"^(main|agent/[a-z0-9-]+)$")

PUBLISHED_STATUSES = {"OPEN_DRAFT_PR", "MERGED_TO_MAIN"}
ALL_STATUSES = PUBLISHED_STATUSES | {"PLANNED_WORK_PACKET", "DELIVERED_ON_MAIN"}

REQUIRED_PACKET_FIELDS = (
    "id",
    "status",
    "issue",
    "parents",
    "head",
    "transition",
    "pathLease",
    "evals",
    "negativeControls",
    "rollback",
    "humanAdmit",
)

REQUIRED_TEMPLATE_TOKENS = (
    "Work packet",
    "State transition",
    "Path lease",
    "Evidence lanes",
    "Negative controls",
    "Rollback",
    "Human Admit",
)

GIT_TOWN_CONFIG_SURFACES = (".git-town.toml", "git-town.toml", ".git-branches.toml")


class ValidationError(RuntimeError):
    """Raised when the delivery graph or its evidence surfaces weaken a hard law."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


def load_json(relative_path: str) -> Any:
    path = ROOT / relative_path
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (FileNotFoundError, json.JSONDecodeError) as error:
        raise ValidationError(f"cannot load {relative_path}: {error}") from error


def lease_prefix(pattern: str) -> str:
    """Reduce a path lease entry to the prefix it actually reserves."""
    return pattern[:-2] if pattern.endswith("/**") else pattern


def leases_overlap(left: str, right: str) -> bool:
    a, b = lease_prefix(left), lease_prefix(right)
    if a == b:
        return True
    return a.startswith(b.rstrip("/") + "/") or b.startswith(a.rstrip("/") + "/")


def validate_schema_surface(manifest: Any) -> None:
    schema = load_json(SCHEMA)
    require(schema.get("$id") == SCHEMA_ID, "manifest schema $id drifted")
    require(isinstance(manifest, dict), "manifest must be an object")
    require(manifest.get("schema") == SCHEMA_ID, "manifest schema identifier drifted")
    eval_sets = manifest.get("evalSets")
    require(
        isinstance(eval_sets, list) and eval_sets and len(set(eval_sets)) == len(eval_sets),
        "evalSets must be a non-empty unique list",
    )
    packets = manifest.get("packets")
    require(isinstance(packets, list) and packets, "packets must be a non-empty list")

    ids: set[str] = set()
    for packet in packets:
        require(isinstance(packet, dict), "each packet must be an object")
        for field in REQUIRED_PACKET_FIELDS:
            require(field in packet, f"packet is missing required field {field}")
        pid = packet["id"]
        require(bool(PACKET_ID.match(pid)), f"invalid packet id: {pid}")
        require(pid not in ids, f"duplicate packet id: {pid}")
        ids.add(pid)
        require(packet["status"] in ALL_STATUSES, f"{pid} has unknown status {packet['status']}")
        require(isinstance(packet["issue"], int) and packet["issue"] > 0, f"{pid} needs an issue")
        require(bool(TRANSITION.match(packet["transition"])), f"{pid} transition is malformed")
        require(bool(HEAD_BRANCH.match(packet["head"])), f"{pid} head branch is malformed")
        for field in ("parents", "pathLease", "evals", "negativeControls", "humanAdmit"):
            value = packet[field]
            require(
                isinstance(value, list) and value and all(isinstance(x, str) and x for x in value),
                f"{pid} {field} must be a non-empty list of strings",
            )
        require(
            isinstance(packet["rollback"], str) and packet["rollback"],
            f"{pid} rollback subject is required",
        )
        unknown = set(packet["evals"]) - set(eval_sets)
        require(not unknown, f"{pid} references undeclared eval sets: {sorted(unknown)}")


def validate_publication_state(manifest: Any) -> None:
    """A planned packet may never carry publication evidence, and vice versa."""
    for packet in manifest["packets"]:
        pid, status = packet["id"], packet["status"]
        has_pr = "pullRequest" in packet
        has_merged = "mergedHeadSha" in packet
        if status == "PLANNED_WORK_PACKET":
            require(not has_pr, f"{pid} is planned but claims pull request {packet.get('pullRequest')}")
            require(not has_merged, f"{pid} is planned but claims a merged head")
            require("externalGate" in packet, f"{pid} is planned and must declare an external gate")
        elif status == "OPEN_DRAFT_PR":
            require(has_pr, f"{pid} is published and must declare its pull request")
            require(not has_merged, f"{pid} is an open draft and cannot claim a merged head")
        elif status == "MERGED_TO_MAIN":
            require(has_pr, f"{pid} was merged and must retain its pull request number")
            require(has_merged, f"{pid} was merged and must record the exact merged head")
            require(bool(SHA1.match(packet["mergedHeadSha"])), f"{pid} mergedHeadSha is not a SHA-1")
        else:  # DELIVERED_ON_MAIN
            require(not has_pr, f"{pid} was delivered on main and cannot claim a pull request")
            require(not has_merged, f"{pid} was delivered on main and cannot claim a merged head")
        if has_pr:
            require(
                isinstance(packet["pullRequest"], int) and packet["pullRequest"] > 0,
                f"{pid} pull request number must be a positive integer",
            )


def validate_graph(manifest: Any) -> None:
    packets = {p["id"]: p for p in manifest["packets"]}
    for pid, packet in packets.items():
        for parent in packet["parents"]:
            require(
                parent == ROOT_PARENT or parent in packets,
                f"{pid} references unknown parent {parent}",
            )
            require(parent != pid, f"{pid} is its own parent")

    # Depth-first cycle detection over the parent graph.
    state: dict[str, int] = {}

    def walk(pid: str, trail: tuple[str, ...]) -> None:
        if state.get(pid) == 2:
            return
        require(state.get(pid) != 1, f"delivery graph cycle: {' -> '.join(trail + (pid,))}")
        state[pid] = 1
        for parent in packets[pid]["parents"]:
            if parent != ROOT_PARENT:
                walk(parent, trail + (pid,))
        state[pid] = 2

    for pid in packets:
        walk(pid, ())

    # Every packet must reach main; an orphan island is not admissible.
    for pid in packets:
        seen: set[str] = set()
        frontier = [pid]
        reaches_root = False
        while frontier:
            current = frontier.pop()
            if current in seen:
                continue
            seen.add(current)
            for parent in packets[current]["parents"]:
                if parent == ROOT_PARENT:
                    reaches_root = True
                else:
                    frontier.append(parent)
        require(reaches_root, f"{pid} never reaches {ROOT_PARENT}")


def validate_sibling_path_leases(manifest: Any) -> None:
    """Siblings sharing a parent must not reserve overlapping paths."""
    packets = manifest["packets"]
    by_parent: dict[str, list[dict[str, Any]]] = {}
    for packet in packets:
        for parent in packet["parents"]:
            by_parent.setdefault(parent, []).append(packet)

    for parent, siblings in by_parent.items():
        for index, left in enumerate(siblings):
            for right in siblings[index + 1 :]:
                for left_path in left["pathLease"]:
                    for right_path in right["pathLease"]:
                        require(
                            not leases_overlap(left_path, right_path),
                            f"siblings {left['id']} and {right['id']} under {parent} "
                            f"both lease {left_path} / {right_path}",
                        )


def validate_narrative_digest(manifest: Any) -> None:
    source = manifest["narrativeSource"]
    require(source["path"] == NARRATIVE, "narrativeSource path drifted")
    declared = source["sha256"]
    require(bool(SHA256.match(declared)), "narrativeSource sha256 is malformed")
    path = ROOT / NARRATIVE
    require(path.is_file(), f"{NARRATIVE} is missing")
    actual = hashlib.sha256(path.read_bytes()).hexdigest()
    require(
        actual == declared,
        f"narrative digest drift: {NARRATIVE} is {actual}, manifest declares {declared}",
    )


def validate_git_town_not_admitted(manifest: Any) -> None:
    runtime = manifest["gitTownRuntime"]
    require(runtime["runtimeAdmitted"] is False, "Git Town runtime admission is not evidenced")
    require(runtime["executable"] in {"ABSENT", "PINNED_CANDIDATE"}, "Git Town executable is not admitted")
    require(runtime["canary"] == "NOT_EXERCISED", "no Git Town canary has executed in this repository")
    for surface in GIT_TOWN_CONFIG_SURFACES:
        require(not (ROOT / surface).exists(), f"consumer repository was configured for Git Town: {surface}")


def validate_templates(_: Any) -> None:
    for relative in (ISSUE_TEMPLATE, PR_TEMPLATE):
        path = ROOT / relative
        require(path.is_file(), f"{relative} is missing")
        text = path.read_text(encoding="utf-8")
        for token in REQUIRED_TEMPLATE_TOKENS:
            require(token in text, f"{relative} is missing required section: {token}")


def validate_ci_wiring(_: Any) -> None:
    workflow = (ROOT / ".github/workflows/verify.yml").read_text(encoding="utf-8")
    require(
        "python3 scripts/validate_stacked_delivery.py" in workflow,
        "verify workflow does not run the stacked delivery validator",
    )


CHECKS = (
    validate_schema_surface,
    validate_publication_state,
    validate_graph,
    validate_sibling_path_leases,
    validate_narrative_digest,
    validate_git_town_not_admitted,
    validate_templates,
    validate_ci_wiring,
)

# Each mutation must be denied. A mutation that survives means the gate is decorative.
MUTATIONS: tuple[tuple[str, Any], ...] = (
    ("schema identifier drift", lambda m: m.update({"schema": "something-else"})),
    ("unknown parent", lambda m: m["packets"][1]["parents"].__setitem__(0, "ZZ9")),
    ("self parent", lambda m: m["packets"][1]["parents"].__setitem__(0, m["packets"][1]["id"])),
    ("graph cycle", lambda m: _plant_cycle(m)),
    ("planned packet claims a pull request", lambda m: _first_planned(m).update({"pullRequest": 999})),
    ("planned packet claims a merged head", lambda m: _first_planned(m).update({"mergedHeadSha": "0" * 40})),
    ("merged packet drops its merged head", lambda m: _first_merged(m).pop("mergedHeadSha")),
    ("narrative digest drift", lambda m: m["narrativeSource"].update({"sha256": "0" * 64})),
    ("premature Git Town runtime admission", lambda m: m["gitTownRuntime"].update({"runtimeAdmitted": True})),
    ("unevidenced Git Town canary", lambda m: m["gitTownRuntime"].update({"canary": "EXECUTED"})),
    ("sibling path lease overlap", lambda m: _plant_lease_overlap(m)),
    ("undeclared eval set", lambda m: m["packets"][0]["evals"].append("E-IMAGINARY")),
    ("missing rollback subject", lambda m: m["packets"][0].pop("rollback")),
    ("missing negative controls", lambda m: m["packets"][0].__setitem__("negativeControls", [])),
    ("duplicate packet id", lambda m: m["packets"][1].__setitem__("id", m["packets"][0]["id"])),
    ("planned packet without external gate", lambda m: _first_planned(m).pop("externalGate")),
)


def _first_planned(manifest: Any) -> Any:
    return next(p for p in manifest["packets"] if p["status"] == "PLANNED_WORK_PACKET")


def _first_merged(manifest: Any) -> Any:
    return next(p for p in manifest["packets"] if p["status"] == "MERGED_TO_MAIN")


def _plant_cycle(manifest: Any) -> None:
    packets = manifest["packets"]
    packets[0]["parents"] = [packets[1]["id"]]
    packets[1]["parents"] = [packets[0]["id"]]


def _plant_lease_overlap(manifest: Any) -> None:
    packets = {p["id"]: p for p in manifest["packets"]}
    by_parent: dict[str, list[Any]] = {}
    for packet in manifest["packets"]:
        for parent in packet["parents"]:
            by_parent.setdefault(parent, []).append(packet)
    for siblings in by_parent.values():
        if len(siblings) >= 2:
            siblings[1]["pathLease"] = list(siblings[0]["pathLease"])
            return
    raise ValidationError("self-test fixture has no sibling pair to plant an overlap in")


def run_checks(manifest: Any, *, announce: bool) -> None:
    for check in CHECKS:
        check(manifest)
        if announce:
            print(f"PASS {check.__name__}")


def self_test(manifest: Any) -> None:
    for label, mutate in MUTATIONS:
        mutated = copy.deepcopy(manifest)
        try:
            mutate(mutated)
        except (KeyError, IndexError, StopIteration) as error:
            raise ValidationError(f"self-test could not plant '{label}': {error}") from error
        try:
            run_checks(mutated, announce=False)
        except ValidationError:
            continue
        raise ValidationError(f"mutation control did not deny: {label}")
    print(f"PASS self_test ({len(MUTATIONS)} mutation controls denied)")


def main(argv: list[str]) -> int:
    try:
        manifest = load_json(MANIFEST)
        run_checks(manifest, announce=True)
        if "--self-test" in argv:
            self_test(manifest)
    except (ValidationError, OSError, json.JSONDecodeError) as error:
        print(f"FAIL {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
