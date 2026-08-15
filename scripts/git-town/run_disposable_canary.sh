#!/usr/bin/env bash
set -euo pipefail

# Disposable Git Town v24 candidate canary.
# This script never targets ed3c/gym-come-true and never publishes a branch.

usage() {
  echo "usage: $0 <git-town-binary> <candidate-json> <receipt-dir>" >&2
  exit 64
}

[[ $# -eq 3 ]] || usage

binary="$1"
candidate="$2"
receipt_dir="$3"
script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
verifier="$script_dir/verify_admission.py"

[[ -x "$binary" ]] || { echo "FAIL binary is not executable" >&2; exit 1; }
[[ -f "$candidate" && ! -L "$candidate" ]] || { echo "FAIL candidate is missing or a symlink" >&2; exit 1; }
mkdir -p "$receipt_dir"
[[ ! -L "$receipt_dir" ]] || { echo "FAIL receipt directory is a symlink" >&2; exit 1; }

export GIT_TERMINAL_PROMPT=0
export GIT_EDITOR=:
export GIT_SEQUENCE_EDITOR=:
export GCM_INTERACTIVE=Never
export LC_ALL=C.UTF-8
export LANG=C.UTF-8

root="$(mktemp -d -t gym-git-town-canary.XXXXXX)"
cleanup_state="PENDING"
cleanup() {
  local code=$?
  rm -rf -- "$root"
  cleanup_state="PASS"
  exit "$code"
}
trap cleanup EXIT INT TERM

hash_text() {
  sha256sum | awk '{print $1}'
}

remote_digest() {
  local bare="$1"
  git --git-dir="$bare" for-each-ref --format='%(refname) %(objectname)' refs/heads | sort | hash_text
}

write_config() {
  cat > git-town.toml <<'EOF'
[branches]
main = "main"
default-type = "feature"
perennials = []

[create]
share-new-branches = "no"

[hosting]
dev-remote = "origin"
forge-type = ""

[sync]
auto-resolve = false
auto-sync = false
feature-strategy = "rebase"
perennial-strategy = "ff-only"
push-hook = true
tags = false
upstream = false
EOF
}

init_repo() {
  local repo="$1"
  local bare="$2"
  mkdir -p "$repo"
  git init --bare "$bare" >/dev/null
  git -C "$repo" init -b main >/dev/null
  git -C "$repo" config user.name "Git Town Canary"
  git -C "$repo" config user.email "git-town-canary@example.invalid"
  git -C "$repo" remote add origin "$bare"
}

commit_file() {
  local repo="$1"
  local path="$2"
  local text="$3"
  local message="$4"
  printf '%s\n' "$text" > "$repo/$path"
  git -C "$repo" add "$path"
  git -C "$repo" commit -m "$message" >/dev/null
}

# Candidate metadata and binary version are verified before any Git operation.
python3 "$verifier" \
  --candidate "$candidate" \
  --binary "$binary" \
  --self-test \
  --receipt "$receipt_dir/binary-verification.json" >/dev/null

version_output="$($binary --version 2>&1)"
version_sha="$(printf '%s' "$version_output" | hash_text)"

# Clean stack canary with a local bare remote.
clean_repo="$root/clean"
clean_origin="$root/clean-origin.git"
init_repo "$clean_repo" "$clean_origin"
commit_file "$clean_repo" README.md "base" "base"
git -C "$clean_repo" push -u origin main >/dev/null
(
  cd "$clean_repo"
  write_config
  git switch -c feature-a >/dev/null
  printf 'feature-a\n' > feature-a.txt
  git add feature-a.txt
  git commit -m feature-a >/dev/null
  git config git-town-branch.feature-a.parent main
  git switch -c feature-b >/dev/null
  printf 'feature-b\n' > feature-b.txt
  git add feature-b.txt
  git commit -m feature-b >/dev/null
  git config git-town-branch.feature-b.parent feature-a
)

before_clean_remote="$(remote_digest "$clean_origin")"
branch_output="$(cd "$clean_repo" && "$binary" branch --non-interactive 2>&1)"
branch_output_sha="$(printf '%s' "$branch_output" | hash_text)"
for required in main feature-a feature-b; do
  grep -Fq "$required" <<<"$branch_output" || {
    echo "FAIL branch hierarchy output omits $required" >&2
    exit 1
  }
done

dry_run_output="$(cd "$clean_repo" && timeout 60 "$binary" sync --stack --dry-run --non-interactive --no-auto-resolve --no-push 2>&1)"
dry_run_output_sha="$(printf '%s' "$dry_run_output" | hash_text)"
after_dry_remote="$(remote_digest "$clean_origin")"
[[ "$before_clean_remote" == "$after_dry_remote" ]] || {
  echo "FAIL dry-run changed remote refs" >&2
  exit 1
}

# Advance main locally and prove an actual no-push stack sync rebases both children.
git -C "$clean_repo" switch main >/dev/null
commit_file "$clean_repo" base.txt "main advanced" "advance main"
advanced_main="$(git -C "$clean_repo" rev-parse main)"
git -C "$clean_repo" switch feature-b >/dev/null
sync_output="$(cd "$clean_repo" && timeout 60 "$binary" sync --stack --non-interactive --no-auto-resolve --no-push 2>&1)"
sync_output_sha="$(printf '%s' "$sync_output" | hash_text)"
after_sync_remote="$(remote_digest "$clean_origin")"
[[ "$before_clean_remote" == "$after_sync_remote" ]] || {
  echo "FAIL no-push sync changed remote refs" >&2
  exit 1
}
git -C "$clean_repo" merge-base --is-ancestor "$advanced_main" feature-a
git -C "$clean_repo" merge-base --is-ancestor feature-a feature-b
[[ -z "$(git -C "$clean_repo" status --porcelain)" ]] || {
  echo "FAIL clean sync left residue" >&2
  exit 1
}

# Conflict canary in a separate disposable repository.
conflict_repo="$root/conflict"
conflict_origin="$root/conflict-origin.git"
init_repo "$conflict_repo" "$conflict_origin"
commit_file "$conflict_repo" conflict.txt "base" "base"
git -C "$conflict_repo" push -u origin main >/dev/null
(
  cd "$conflict_repo"
  write_config
  git switch -c feature-conflict >/dev/null
  printf 'feature version\n' > conflict.txt
  git add conflict.txt
  git commit -m feature-conflict >/dev/null
  git config git-town-branch.feature-conflict.parent main
  git switch main >/dev/null
  printf 'main version\n' > conflict.txt
  git add conflict.txt
  git commit -m main-conflict >/dev/null
  git switch feature-conflict >/dev/null
)

before_conflict_remote="$(remote_digest "$conflict_origin")"
set +e
conflict_output="$(cd "$conflict_repo" && timeout 60 "$binary" sync --stack --non-interactive --no-auto-resolve --no-push 2>&1)"
conflict_exit=$?
set -e
conflict_output_sha="$(printf '%s' "$conflict_output" | hash_text)"
[[ $conflict_exit -ne 0 ]] || {
  echo "FAIL planted semantic conflict unexpectedly succeeded" >&2
  exit 1
}
after_conflict_remote="$(remote_digest "$conflict_origin")"
[[ "$before_conflict_remote" == "$after_conflict_remote" ]] || {
  echo "FAIL conflict path changed remote refs" >&2
  exit 1
}

conflict_status="$(git -C "$conflict_repo" status --porcelain=v1)"
[[ -n "$conflict_status" ]] || {
  echo "FAIL conflict path left no inspectable blocked state" >&2
  exit 1
}
conflict_status_sha="$(printf '%s' "$conflict_status" | hash_text)"

# The forbidden recovery/ship commands are never invoked. Abort only the disposable
# native Git operation after receipt subjects are captured, then remove the temp root.
if git -C "$conflict_repo" rev-parse --git-path rebase-merge | grep -q . && \
   [[ -d "$(git -C "$conflict_repo" rev-parse --git-path rebase-merge)" ]]; then
  git -C "$conflict_repo" rebase --abort >/dev/null 2>&1 || true
elif [[ -f "$(git -C "$conflict_repo" rev-parse --git-path MERGE_HEAD)" ]]; then
  git -C "$conflict_repo" merge --abort >/dev/null 2>&1 || true
fi

cat > "$receipt_dir/disposable-canary.json" <<EOF
{
  "schema": "gym-come-true/git-town-disposable-canary-receipt/v1",
  "candidate": "v24.0.0-linux-x86_64",
  "versionOutputSha256": "$version_sha",
  "branchHierarchy": {
    "state": "PASS",
    "outputSha256": "$branch_output_sha"
  },
  "dryRunNoPushSync": {
    "state": "PASS",
    "outputSha256": "$dry_run_output_sha",
    "remoteBeforeSha256": "$before_clean_remote",
    "remoteAfterSha256": "$after_dry_remote"
  },
  "actualNoPushSync": {
    "state": "PASS",
    "outputSha256": "$sync_output_sha",
    "advancedMain": "$advanced_main",
    "remoteAfterSha256": "$after_sync_remote"
  },
  "semanticConflictFailClosed": {
    "state": "PASS",
    "exitCode": $conflict_exit,
    "outputSha256": "$conflict_output_sha",
    "blockedStatusSha256": "$conflict_status_sha",
    "remoteBeforeSha256": "$before_conflict_remote",
    "remoteAfterSha256": "$after_conflict_remote"
  },
  "promptSuppression": "PASS",
  "forbiddenCommandsInvoked": [],
  "publication": "NOT_EXERCISED",
  "consumerRepositorySync": "NOT_EXERCISED",
  "worktreeAndLease": "NOT_EXERCISED",
  "runtimeAdmitted": false,
  "productionUse": "DENY",
  "cleanup": "PASS_ON_PROCESS_EXIT"
}
EOF

python3 -m json.tool "$receipt_dir/disposable-canary.json" >/dev/null
printf '%s\n' "PASS disposable Git Town candidate canary"
