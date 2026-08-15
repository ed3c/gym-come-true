---
name: Work packet
about: One molecular delivery slice with an explicit parent, lease, evidence, and rollback
title: "<ID> — <outcome>"
labels: []
assignees: []
---

## Work packet

- Packet ID (matches `docs/git/stacked-delivery-manifest.json`):
- Parent packet ID(s):
- Delivery branch (`agent/<slug>`):
- Reviewed parent head SHA:

## State transition

One transition only.

```text
<FROM_STATE> -> <TO_STATE>
```

## Path lease

Paths this packet may write. Siblings of the same parent must not overlap.

- `path/**`

## Evidence lanes

Name the eval set(s) from the manifest and the exact commands that will run.

- Eval set(s):
- Commands:

```bash
python3 scripts/validate_repository.py
python3 scripts/validate_stacked_delivery.py --self-test
```

Only a command that actually ran against the stated commit may be `PASS` or `FAIL`.
A run blocked before runner allocation is `PRE_RUN_BLOCKED`, not a pass and not a code failure.

## Negative controls

What must be rejected for this packet to be admissible.

- [ ]

## External gate

Evidence repository code cannot manufacture (legal, clinical, store, credential, device, rights).

- [ ]

## Rollback

Immutable rollback subject (exact SHA or prior manifest version):

## Human Admit

Operations that stay owner-controlled.

- [ ] merge
- [ ]
