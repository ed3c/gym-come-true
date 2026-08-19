#!/usr/bin/env python3
"""Fail closed when checked-in repository authority surfaces regress.

Offline and standard-library only. This validates checked-in consistency; it
does not discover live GitHub state or promote external evidence.
"""
from __future__ import annotations

import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class AuthorityError(ValueError): pass

def require(text: str, needle: str, subject: str) -> None:
    if needle not in text: raise AuthorityError(f"{subject}: required authority law missing: {needle!r}")

def reject(text: str, needle: str, subject: str) -> None:
    if needle in text: raise AuthorityError(f"{subject}: stale authority claim present: {needle!r}")


def validate(agents: str, status: str, readme: str, readme_zh: str, roadmap: str, git_readme: str, architecture: str, platform: str, store: str) -> None:
    combined = "\n".join((agents, status, readme, readme_zh, roadmap, git_readme, architecture, platform, store))

    reject(status, "(private, immutable repository ID", "implementation-status")
    require(status, "public, immutable repository ID `1334805292`", "implementation-status")
    reject(status, "**Current staged parent:** Draft PR #55", "implementation-status")
    reject(status, "**This SSOT packet:** Issue #56", "implementation-status")
    reject(status, "Complete Issue #56 only as this docs-only child", "implementation-status")
    require(status, "**Current top Draft evidence:** PR #69", "implementation-status")
    require(status, "**This reconciliation packet:** Issue #70", "implementation-status")
    require(status, "PR #69  SECONDARY_ROUTING_SURFACES_RECONCILED_DRAFT", "implementation-status")
    require(status, "PR #59  TRANSPORT_AND_SEMANTIC_IDENTITIES_SEPARATED_DRAFT", "implementation-status")
    require(status, "#56/#60/#62/#64/#66/#68", "implementation-status")
    require(status, "OPEN_ISSUE != ABSENT_IMPLEMENTATION", "implementation-status")

    require(agents, "Visibility: public", "AGENTS")
    for subject, text in (("README", readme), ("README.zh-TW", readme_zh)):
        reject(text, "Application code is currently proprietary", subject)
        reject(text, "Application code 目前為 proprietary", subject)
        require(text, "Apache License 2.0", subject)

    for claim in ("No hosted check has ever executed on this repository", "No workflow run on this repository has ever allocated a runner", "no hosted CI run has ever allocated a runner", "本 repo 至今沒有任何一次 hosted CI run 配置到 runner", "every workflow run on this repository so far ended before runner allocation", "本 repo 至今每一次 workflow run 都在配置 runner 之前就結束", "All published PRs remain Draft and unmerged", "hosted exact-head evidence still missing (Issue #45)"):
        reject(combined, claim, "authority surfaces")
    require(agents, "Hosted GitHub Actions now execute normally", "AGENTS")
    require(status, "PRE_RUN_BLOCKED", "implementation-status")
    require(readme, "Hosted GitHub Actions now execute normally", "README")
    require(readme_zh, "GitHub Actions hosted runners 現在能正常執行", "README.zh-TW")
    require(roadmap, "Historical Actions runs that ended before runner allocation remain `PRE_RUN_BLOCKED`", "roadmap")

    for claim in ("future Health Connect", "future HealthKit", "Not yet implemented: Health Connect", "Health data | Adapter boundary | Adapter boundary | N/A | Not implemented", "Health Connect／reliability 在 Issue #10", "HealthKit／AlarmKit 在 Issue #9", "no HealthKit/Health Connect integration", "| Health data | Boundary only | Boundary only | N/A |", "Future Apple Health:", "| Health data | not implemented | N/A | none |"):
        reject(combined, claim, "authority surfaces")
    require(combined, "Health Connect availability/permission/read adapters", "authority surfaces")
    require(combined, "NativeHealthReadBridge", "authority surfaces")
    require(combined, "real-device", "authority surfaces")
    require(architecture, "ADAPTER_PRESENT != REAL_DEVICE_VALIDATION", "architecture")
    require(platform, "ADAPTER_PRESENT != REAL_DEVICE_VALIDATION", "platform-capability-matrix")
    require(platform, "Health Connect availability/permission/read adapters", "platform-capability-matrix")
    require(platform, "`NativeHealthReadBridge` / HealthKit read adapter", "platform-capability-matrix")
    require(store, "`NSHealthShareUsageDescription` exists", "store-compliance")
    require(store, "`READ_WEIGHT` and `READ_EXERCISE`", "store-compliance")
    require(store, "DECLARED_PERMISSION != STORE_APPROVAL", "store-compliance")

    for claim in ("exact Git Town executable/version  ABSENT", "Exact Git Town version and executable | `ABSENT`", "Exact Git Town version/executable | `ABSENT`", "exact Git Town executable   ABSENT", "Git Town executable admission and live worktree/sync/conflict/publication canaries are `ABSENT`"):
        reject(combined, claim, "authority surfaces")
    require(combined, "v24.0.0", "authority surfaces")
    require(combined, "CANDIDATE_METADATA_VERIFIED_RUNTIME_BLOCKED", "authority surfaces")
    require(combined, "production_use: DENY", "authority surfaces")
    require(git_readme, "Git Town candidate          PINNED_CANDIDATE / v24.0.0", "docs/git/README")
    require(git_readme, "Git Town runtime admitted   false", "docs/git/README")
    require(git_readme, "consumer sync canary        NOT_EXERCISED", "docs/git/README")
    require(architecture, "candidate                  PINNED_CANDIDATE / v24.0.0", "architecture")

    reject(readme, "Issues #24–#48 are requirements and future work packets", "README")
    reject(readme_zh, "Issues #24–#48 是 requirements 與未來 work packets", "README.zh-TW")
    require(roadmap, "OPEN_ISSUE != ABSENT_IMPLEMENTATION", "roadmap")
    require(roadmap, "Historical delivery PRs #2, #15, #16, #20, and #22 are merged history", "roadmap")
    require(roadmap, "PR #67  LIVE_DELIVERY_GRAPH_RECONCILED_DRAFT", "roadmap")
    require(git_readme, "Historical PRs #2/#15/#16/#20/#22 are merged", "docs/git/README")
    require(git_readme, "`STACKED_PRS.md` owns it", "docs/git/README")

    require(agents, "HOSTED_PASS(commit A) != HOSTED_PASS(commit B)", "AGENTS")
    require(agents, "Human Admit for merge or promotion", "AGENTS")
    for law in ("HOSTED_PASS(commit A) != HOSTED_PASS(commit B)", "GITHUB_CHECK_PASS != HUMAN_ADMIT", "ADAPTER_PRESENT != REAL_DEVICE_VALIDATION", "GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED"):
        require(readme, law, "README"); require(readme_zh, law, "README.zh-TW")
    require(roadmap, "GITHUB_CHECK_PASS != HUMAN_ADMIT", "roadmap")
    require(git_readme, "HOSTED_PASS(commit A) != HOSTED_PASS(commit B)", "docs/git/README")
    require(git_readme, "GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED", "docs/git/README")
    require(architecture, "GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED", "architecture")
    require(store, "GITHUB_CHECK_PASS != HUMAN_ADMIT", "store-compliance")
    for token in ("legal", "clinical", "rights"): require(combined, token, "authority surfaces")


def read_surfaces(root: Path) -> tuple[str, ...]:
    paths = ("AGENTS.md", "docs/implementation-status.md", "README.md", "README.zh-TW.md", "docs/roadmap.md", "docs/git/README.md", "docs/architecture.md", "docs/platform-capability-matrix.md", "docs/store-compliance.md")
    return tuple((root / p).read_text(encoding="utf-8") for p in paths)

def validate_paths(root: Path) -> None: validate(*read_surfaces(root))


def self_test() -> None:
    base=list(read_surfaces(ROOT)); validate(*base)
    mutations=[
        (0,"append","No hosted check has ever executed on this repository","agent-hosted-history"),(0,"replace","Visibility: public\0Visibility: private","agent-visibility"),(0,"append","future Health Connect","agent-health-connect"),(0,"append","exact Git Town executable/version  ABSENT","agent-git-town"),(0,"replace","HOSTED_PASS(commit A) != HOSTED_PASS(commit B)\0HOSTED_PASS","agent-exact-head-law"),(0,"replace","Human Admit for merge or promotion\0merge or promotion","agent-human-admit"),
        (1,"append","No workflow run on this repository has ever allocated a runner","status-hosted"),(1,"replace","public, immutable repository ID `1334805292`\0private, immutable repository ID `1334805292`","status-visibility"),(1,"append","**Current staged parent:** Draft PR #55","status-old-parent"),(1,"append","**This SSOT packet:** Issue #56","status-old-packet"),(1,"replace","**Current top Draft evidence:** PR #69\0**Current top Draft evidence:** PR #55","status-current-top"),(1,"replace","**This reconciliation packet:** Issue #70\0**This reconciliation packet:** Issue #56","status-current-packet"),(1,"replace","OPEN_ISSUE != ABSENT_IMPLEMENTATION\0OPEN_ISSUE","status-open-issue-law"),
        (2,"append","no hosted CI run has ever allocated a runner","readme-hosted"),(2,"replace","Apache License 2.0\0proprietary","readme-license"),(2,"append","Exact Git Town version/executable | `ABSENT`","readme-git-town"),(3,"append","Health Connect／reliability 在 Issue #10","readme-zh-health"),(2,"replace","GITHUB_CHECK_PASS != HUMAN_ADMIT\0GITHUB_CHECK_PASS","readme-human-admit-law"),(3,"replace","GIT_TOWN_CANDIDATE != GIT_TOWN_RUNTIME_ADMITTED\0GIT_TOWN_CANDIDATE","readme-zh-git-town-law"),
        (4,"append","All published PRs remain Draft and unmerged","roadmap-merged-history"),(4,"append","hosted exact-head evidence still missing (Issue #45)","roadmap-hosted-history"),(4,"replace","OPEN_ISSUE != ABSENT_IMPLEMENTATION\0OPEN_ISSUE","roadmap-open-issue-law"),(5,"append","exact Git Town executable   ABSENT","git-readme-candidate"),(5,"replace","Git Town runtime admitted   false\0Git Town runtime admitted   true","git-readme-runtime"),(5,"replace","Historical PRs #2/#15/#16/#20/#22 are merged\0Historical PRs are Draft","git-readme-history"),
        (6,"append","The current repository has no HealthKit/Health Connect integration","architecture-health-absent"),(6,"append","Git Town executable admission and live worktree/sync/conflict/publication canaries are `ABSENT`","architecture-git-town-absent"),(6,"replace","ADAPTER_PRESENT != REAL_DEVICE_VALIDATION\0ADAPTER_PRESENT","architecture-device-law"),
        (7,"append","| Health data | Boundary only | Boundary only | N/A |","platform-health-boundary-only"),(7,"replace","ADAPTER_PRESENT != REAL_DEVICE_VALIDATION\0ADAPTER_PRESENT","platform-device-law"),(7,"replace","AlarmKit not admitted\0AlarmKit implemented","platform-alarm-inflation"),
        (8,"append","Future Apple Health:","store-future-health"),(8,"append","| Health data | not implemented | N/A | none |","store-health-absent"),(8,"replace","DECLARED_PERMISSION != STORE_APPROVAL\0DECLARED_PERMISSION","store-approval-law"),(8,"replace","GITHUB_CHECK_PASS != HUMAN_ADMIT\0GITHUB_CHECK_PASS","store-human-admit-law")]
    for index,mode,payload,name in mutations:
        mutated=base.copy()
        if mode=="append": mutated[index]+="\n"+payload+"\n"
        else:
            old,new=payload.split("\0",1); mutated[index]=mutated[index].replace(old,new,1)
        try: validate(*mutated)
        except AuthorityError: print(f"PASS planted authority drift rejected: {name}")
        else: raise AssertionError(f"mutation was not rejected: {name}")
    print(f"PASS authority self-test: {len(mutations)} planted drifts rejected")


def main() -> int:
    parser=argparse.ArgumentParser(); parser.add_argument("--self-test",action="store_true"); args=parser.parse_args()
    if args.self_test: self_test()
    else: validate_paths(ROOT); print("PASS authority surfaces")
    return 0

if __name__ == "__main__": raise SystemExit(main())
