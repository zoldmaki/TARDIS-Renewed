# TARDIS Renewed Phase 10 Hotfix 2

This hotfix addresses the Phase 10 two-block exterior regressions.

## Fixes

- Takeoff now explicitly removes both exterior halves instead of relying on a lower-block break callback.
- Landing accepts an already-correct exterior half as success rather than treating `setBlockState` returning `false` as a placement failure.
- Orphaned upper exterior halves are cleaned before normal landing checks.
- Replaceable blocks such as grass and snow no longer falsely obstruct normal landings.
- Shell changes preserve and restore the lower exterior tile entity NBT.
- Shell changes rebind the placed tile entity from the authoritative TARDIS record after the block-state update.
- Internal shell/structure updates can no longer clear the saved exterior link.
- Breaking either half manually still clears the saved exterior position.
- Failed landing cleanup removes both exterior halves.

## Force landing behavior

`force` remains operator-only, but now means a true emergency placement:

```text
/tr land force
/tr land departure force
/tr as <owner> land force
```

It bypasses the flight timer and clears the two destination blocks before materializing. This can destroy blocks, including unbreakable blocks, so it should only be used for recovery.

It still cannot materialize inside the configured TARDIS interior dimension, in an unregistered/unloadable dimension, or with the lower half outside Y=0..254.

## Focused test

1. Change shells repeatedly and verify `/tr info` continues to report the same owner, TARDIS UUID, and exterior position.
2. Lock/unlock after every shell change without rebinding the exterior.
3. Take off and confirm both halves disappear.
4. Land normally at the destination.
5. Repeat with `/tr land departure`.
6. Obstruct both destination blocks and use `/tr land force`; the blocks should be cleared and the exterior should materialize.
7. Restart and confirm the exterior remains bound and the chosen shell persists.
