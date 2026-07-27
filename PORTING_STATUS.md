# TARDIS Renewed porting status

## Baseline

- Legacy reference: TARDIS Mod `1.7.10-0.995-350`
- Darkcore reference: `0.41-179`
- Target: Forge `1.12.2-14.23.5.2847`, Java 8, ForgeGradle `2.3-SNAPSHOT`, mappings `stable_39`
- Public name: **TARDIS Renewed**
- Registry namespace: `renewedtardis`
- Current version: `0.11.0-alpha`
- Legacy-world import: out of scope

## Completed through Phase 9

The project has native persistent TARDIS identities, bound keys, a linked/lockable exterior, a shared interior dimension with isolated rooms, multiplayer-safe interactions, configurable interior dimension ID, timed/restart-safe flight, destination/facing commands, ownership transfer, operator recovery, and separate prototype console controls.

## Phase 10 additions

- Exterior converted from one block to a linked two-block structure
- Existing Phase 3-9 exteriors automatically receive their missing upper half when loaded
- Proper inset collision/selection bounds for both halves
- Upper-half interaction delegates safely to the lower linked tile entity
- Breaking either half removes the complete exterior structure
- New `police_box` JSON shell with temporary pixel-art textures
- Original blue prototype retained as the `prototype` shell
- Persistent exterior shell/chameleon selection in each TARDIS record
- `/tr shell`, `/tr shell list`, and `/tr as <owner> shell ...`
- Shell selection survives restart, relocation, takeoff, and landing
- New TARDIS records default to `police_box`; older records migrate to `prototype`
- Empty-hand entry only works from the exterior's front-door side
- Upper and lower front-door interactions both work
- Door sounds added when entering or exiting
- Exterior placement and landing now create/validate the complete two-block structure
- Phase 10 Hotfix 2 explicitly removes both halves at takeoff and preserves exterior tile identity through shell state changes
- Operator `force` landing now bypasses the timer and clears obstructing lower/upper destination blocks
- Phase 10 Hotfix 3 verifies both placed states, repairs a missing exterior tile entity, and reports the precise materialization failure stage and world-state diagnostics

## Current architectural rules

- One shared configurable dimension contains isolated TARDIS interior cells.
- Darkcore is a source/behavior reference only.
- Persistent records are authoritative; block/tile data links back to the TARDIS UUID.
- Interactions, flight, shell changes, and ownership changes are server-authoritative.
- Exterior shell IDs are persistent data; models are replaceable presentation assets.
- The lower exterior block owns identity/lock data; the upper half reserves interaction and collision space.
- Existing registry IDs are preserved even when visual models change.
- Normal recovery modes never overwrite occupied blocks; operator-only `force` landing deliberately clears the two exterior destination blocks for emergency recovery.

## Next planned phase

Phase 11 will introduce the interior schema and room-management foundation: reusable schema definitions, connector positions, protected core space, safe room placement/removal, and the first expandable interior room.

See `ROADMAP.md` for the wider phase plan.
