#!/usr/bin/env python3
"""Fail closed when checked-in repository authority surfaces regress.

Offline and standard-library only. This validates checked-in consistency; it
does not discover live GitHub state or promote external evidence.
"""
from __future__ import annotations

import argparse
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


def validate(agents: str, status: str, readme: str, readme_zh: str) -> None:
    combined = "\n".join((agents, status, readme, readme_zh))

    # Repository identity and license truth.
    reject(status, "(private, immutable repository ID", "implementation-status")
    require(status, "public, immutable repository ID `1334805292`", "implementation-status")
    require(agents, "Visibility: public", "AGENTS")
    for subject, text in (("README", readme), ("README.zh-TW", readme_zh)):
        reject(text, "Application code is currently proprietary", subject)
        reject(text, "Application code 目前為 proprietary", subject)
        require(text, "Apache License 2.0", subject)

    # Hosted execution is now real; historical pre-run blocks remain distinct.
    stale_hosted = (
        "No hosted check has ever executed on this repository",
        "No workflow run on this repository has ever allocated a runner",
        "no hosted CI run has ever allocated a runner",
        "本 repo 至今沒有任何一次 hosted CI run 配置到 runner",
        "every workflow run on this repository so far ended before runner allocation",
        "本 repo 至今每一次 workflow run 都在配置 runner 之前就結束",
    )
    for claim in stale_hosted:
        reject(combined, claim, "authority surfaces")
    require(agents, "Hosted GitHub Actions now execute normally", "AGENTS")
    require(status, "PRE_RUN_BLOCKED", "implementation-status")
    require(readme, "Hosted GitHub Actions now execute normally", "README")
    require(readme_zh, "GitHub Actions hosted runners 現在能正常執行", "README.zh-TW")
    require(combined, "PRE_RUN_BLOCKED", "authority surfaces")

    # Platform adapters exist, while device/store/privacy admission remains separate.
    for claim in (
        "future Health Connect",
        "future HealthKit",
        "Not yet implemented: Health Connect",
        "Health Connect or HealthKit;",
        "Health data | Adapter boundary | Adapter boundary | N/A | Not implemented",
        "Health Connect／reliability 在 Issue #10",
        "HealthKit／AlarmKit 在 Issue #9",
    ):
        reject(combined, claim, "authority surfaces")
    require(combined, "Health Connect availability/permission/read adapters", "authority surfaces")
    require(combined, "NativeHealthReadBridge", "authority surfaces")
    require(combined, "real-device", "authority surfaces")

    # Git Town has a pinned candidate, not an admitted consumer runtime.
    for claim in (
        "exact Git Town executable/version  ABSENT",
        "Exact Git Town version and executable | `ABSENT`",
        "Exact Git Town version/executable | `ABSENT`",
    ):
        reject(combined, claim, "authority surfaces")
    require(combined, "v24.0.0", "authority surfaces")
    require(combined, "CANDIDATE_METADATA_VERIFIED_RUNTIME_BLOCKED", "authority surfaces")
    require(combined, "production_use: DENY", "authority surfaces")

    # Open issues cannot be globally described as future implementation when merged/staged code exists.
    reject(readme, "Issues #24–#48 are requirements and future work packets", "README")
    reject(readme_zh, "Issues #24–#48 是 requirements 與未來 work packets", "README.zh-TW")

    # Evidence separation and Human Admit laws must remain visible.
    for law in (
        "HOSTED_PASS(commit A) != HOSTED_PASS(commit B)",
        "GITHUB_CHECK_PASS != HUMAN_ADMIT",
        "ADAPTER_PRESENT != REAL_DEVICE_VALIDATION",
        "GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED",
    ):
        require(combined, law, "authority surfaces")
    require(combined, "HUMAN_ADMIT", "authority surfaces")
    require(combined, "legal", "authority surfaces")
    require(combined, "clinical", "authority surfaces")
    require(combined, "rights", "authority surfaces")


def validate_paths(root: Path) -> None:
    validate(
        (root / "AGENTS.md").read_text(encoding="utf-8"),
        (root / "docs/implementation-status.md").read_text(encoding="utf-8"),
        (root / "README.md").read_text(encoding="utf-8"),
        (root / "README.zh-TW.md").read_text(encoding="utf-8"),
    )


def self_test() -> None:
    agents = (ROOT / "AGENTS.md").read_text(encoding="utf-8")
    status = (ROOT / "docs/implementation-status.md").read_text(encoding="utf-8")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    readme_zh = (ROOT / "README.zh-TW.md").read_text(encoding="utf-8")
    validate(agents, status, readme, readme_zh)

    mutations = [
        (agents + "\nNo hosted check has ever executed on this repository\n", status, readme, readme_zh, "agent-hosted-history"),
        (agents.replace("Visibility: public", "Visibility: private"), status, readme, readme_zh, "agent-visibility"),
        (agents + "\nfuture Health Connect\n", status, readme, readme_zh, "agent-health-connect"),
        (agents + "\nexact Git Town executable/version  ABSENT\n", status, readme, readme_zh, "agent-git-town"),
        (agents.replace("HOSTED_PASS(commit A) != HOSTED_PASS(commit B)", "HOSTED_PASS"), status, readme, readme_zh, "agent-exact-head-law"),
        (agents.replace("Human Admit for merge or promotion", "merge or promotion"), status, readme, readme_zh, "agent-human-admit"),
        (agents, status + "\nNo workflow run on this repository has ever allocated a runner\n", readme, readme_zh, "status-hosted"),
        (agents, status.replace("public, immutable repository ID `1334805292`", "private, immutable repository ID `1334805292`"), readme, readme_zh, "status-visibility"),
        (agents, status, readme + "\nno hosted CI run has ever allocated a runner\n", readme_zh, "readme-hosted"),
        (agents, status, readme.replace("Apache License 2.0", "proprietary"), readme_zh, "readme-license"),
        (agents, status, readme + "\nExact Git Town version/executable | `ABSENT`\n", readme_zh, "readme-git-town"),
        (agents, status, readme, readme_zh + "\nHealth Connect／reliability 在 Issue #10\n", "readme-zh-health"),
    ]
    for mutated_agents, mutated_status, mutated_readme, mutated_readme_zh, name in mutations:
        try:
            validate(mutated_agents, mutated_status, mutated_readme, mutated_readme_zh)
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
