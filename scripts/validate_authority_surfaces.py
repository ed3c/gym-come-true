#!/usr/bin/env python3
"""Fail closed when repository authority surfaces regress to known false states.

This validator is deliberately offline and standard-library only. It validates
checked-in authority consistency; it does not query GitHub or promote evidence.
"""
from __future__ import annotations

import argparse
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class AuthorityError(ValueError):
    pass


def require(text: str, needle: str, subject: str) -> None:
    if needle not in text:
        raise AuthorityError(f"{subject}: required authority law missing: {needle!r}")


def reject(text: str, needle: str, subject: str) -> None:
    if needle in text:
        raise AuthorityError(f"{subject}: stale authority claim present: {needle!r}")


def validate(agents: str, status: str) -> None:
    # Repository identity must not regress to the old private-repository claim.
    reject(status, "(private, immutable repository ID", "implementation-status")
    require(status, "public, immutable repository ID `1334805292`", "implementation-status")
    require(agents, "Visibility: public", "AGENTS")

    # Hosted execution is now real, while historical pre-run blocks remain distinct.
    reject(agents, "No hosted check has ever executed on this repository", "AGENTS")
    reject(status, "No workflow run on this repository has ever allocated a runner", "implementation-status")
    require(agents, "Hosted GitHub Actions now execute normally", "AGENTS")
    require(agents, "PRE_RUN_BLOCKED", "AGENTS")
    require(status, "PRE_RUN_BLOCKED", "implementation-status")

    # Platform adapters exist, but real-device/store/privacy admission must stay separate.
    reject(agents, "future Health Connect", "AGENTS")
    reject(agents, "future HealthKit", "AGENTS")
    reject(status, "Not yet implemented: Health Connect", "implementation-status")
    reject(status, "Health Connect or HealthKit;", "implementation-status")
    require(agents, "Health Connect availability/permission/read adapters and tests", "AGENTS")
    require(agents, "`NativeHealthReadBridge`", "AGENTS")
    require(agents, "real-device", "AGENTS")
    require(status, "real-device", "implementation-status")

    # Git Town has a pinned candidate, not an admitted consumer runtime.
    reject(agents, "exact Git Town executable/version  ABSENT", "AGENTS")
    reject(status, "Exact Git Town version and executable | `ABSENT`", "implementation-status")
    require(agents, "v24.0.0", "AGENTS")
    require(agents, "CANDIDATE_METADATA_VERIFIED_RUNTIME_BLOCKED", "AGENTS")
    require(status, "CANDIDATE_METADATA_VERIFIED_RUNTIME_BLOCKED", "implementation-status")
    require(agents, "production use                           DENY", "AGENTS")

    # Evidence lanes and Human Admit must never collapse into a generic DONE state.
    for law in (
        "HOSTED_PASS(commit A) != HOSTED_PASS(commit B)",
        "GITHUB_CHECK_PASS != HUMAN_ADMIT",
        "GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED",
    ):
        combined = agents + "\n" + status
        require(combined, law, "authority surfaces")
    require(agents, "legal / clinical / rights review", "AGENTS")
    require(agents, "Human Admit for merge or promotion", "AGENTS")


def validate_paths(root: Path) -> None:
    validate(
        (root / "AGENTS.md").read_text(encoding="utf-8"),
        (root / "docs/implementation-status.md").read_text(encoding="utf-8"),
    )


def self_test() -> None:
    agents = (ROOT / "AGENTS.md").read_text(encoding="utf-8")
    status = (ROOT / "docs/implementation-status.md").read_text(encoding="utf-8")
    validate(agents, status)

    mutations = [
        (agents + "\nNo hosted check has ever executed on this repository\n", status, "hosted-history"),
        (agents.replace("Visibility: public", "Visibility: private"), status, "visibility"),
        (agents + "\nfuture Health Connect\n", status, "health-connect"),
        (agents + "\nexact Git Town executable/version  ABSENT\n", status, "git-town"),
        (agents.replace("HOSTED_PASS(commit A) != HOSTED_PASS(commit B)", "HOSTED_PASS"), status, "exact-head-law"),
        (agents.replace("Human Admit for merge or promotion", "merge or promotion"), status, "human-admit"),
        (agents, status + "\nNo workflow run on this repository has ever allocated a runner\n", "status-hosted"),
        (agents, status.replace("public, immutable repository ID `1334805292`", "private, immutable repository ID `1334805292`"), "status-visibility"),
    ]
    for mutated_agents, mutated_status, name in mutations:
        try:
            validate(mutated_agents, mutated_status)
        except AuthorityError:
            print(f"PASS planted authority drift rejected: {name}")
        else:
            raise AssertionError(f"mutation was not rejected: {name}")
    print(f"PASS authority self-test: {len(mutations)} planted drifts rejected")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
    else:
        validate_paths(ROOT)
        print("PASS authority surfaces")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
