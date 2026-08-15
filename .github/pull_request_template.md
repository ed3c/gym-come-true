## Work packet

- Packet ID (matches `docs/git/stacked-delivery-manifest.json`):
- Closes issue:
- Parent PR / packet:
- Reviewed parent head SHA:

## State transition

```text
<FROM_STATE> -> <TO_STATE>
```

## Path lease

Every changed path is covered by this packet's lease in the manifest.

- [ ] `git diff --name-only <parent>...HEAD` stays inside the declared lease
- [ ] no sibling packet's lease is touched

## Evidence lanes

Record what actually executed against **this exact head**. Do not reuse a green result from an older SHA.

| Lane | Command | Head SHA | Result |
|---|---|---|---|
| policy | `python3 scripts/validate_repository.py` | | |
| delivery graph | `python3 scripts/validate_stacked_delivery.py --self-test` | | |
| hosted | GitHub Actions run URL | | |

`PRE_RUN_BLOCKED` is a valid recorded outcome. It is not a pass.

## Negative controls

- [ ]

## Rollback

Immutable rollback subject:

## Human Admit

- [ ] merge
- [ ] release / store / legal / clinical / credential operations remain owner-controlled
