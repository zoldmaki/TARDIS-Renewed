# TARDIS Renewed porting status

## Baseline

- Legacy reference: TARDIS Mod `1.7.10-0.995-350`
- Darkcore reference: `0.41-179`
- Target: Forge `1.12.2-14.23.5.2847`, Java 8, ForgeGradle `2.3-SNAPSHOT`, mappings `stable_39`
- Public name: **TARDIS Renewed**
- Registry namespace: `renewedtardis`
- Current version: `0.21.0-alpha`
- Save-data version: `12`
- Legacy-world import: out of scope

## Completed through Phase 20

The project now has persistent TARDIS identity and ownership, keys, a linked two-block exterior with shell selection, a configurable shared interior dimension, isolated interior cells, multiplayer-safe interaction, restart-safe timed flight, administrative recovery, dedicated prototype console controls, expandable rooms and secondary corridors, Artron energy, persistent subsystem damage, installable upgrades, UUID-based crew permissions, an extensible sonic screwdriver, transmat and gravity-lift systems, an in-game manual, isolated optional information/computer integrations, and dedicated diagnostics/recovery tooling.

## Phase 18 additions

- Save-data version increased to `12`; existing records migrate with no secondary corridors and receive a default active console-room transmat pad when their interior is next prepared
- Persistent secondary graph edges between already-existing adjacent rooms
- Duplicate, non-adjacent, and original-parent corridor rejection
- Safe secondary corridor generation/removal without changing room IDs or rebuilding rooms
- Player-presence checks and operator-only forced corridor removal
- Persistent active transmat point per TARDIS
- Bidirectional interior/exterior transmat with Artron cost, range, permission, flight-state, obstruction, and endpoint validation
- Transmat sonic mode and link support for transmat-pad tile entities
- Craftable paired gravity lifts with up/down block-state direction
- Lightweight persistent-page TARDIS manual

## Phase 19 additions

- Gravity lifts now behave as deliberate elevator levels rather than automatic launch pads
- Up levels activate when a standing player presses jump; down levels activate while the player holds sneak
- Travellers are moved with a reliable server-authoritative teleport to the safe standing space above the paired level
- Destination clearance and cooldown checks prevent trapping and immediate bounce-back
- Existing gravity-lift registry names, metadata, blocks, and worlds remain compatible
- Optional `integrations` configuration category in the Mods-menu GUI
- `/tr integrations` diagnostics for installed, disabled, queued, registered, and failed states
- Optional server-authoritative HWYLA/Waila tooltips for exterior, door, console, controls, engine core, transmat, and gravity lift
- Optional reflection-only ComputerCraft/CC:Tweaked peripheral exposed from the engine core
- Read-only computer methods for identity, owner, state, flight, energy, destination, exterior, systems, upgrades, rooms, and access
- Computer reads are scheduled through the computer API's main-thread task mechanism
- No ComputerCraft API classes are compiled or bundled
- Minimal Waila/HWYLA compile surfaces are excluded from the output jar; the real optional mod supplies them at runtime
- No persistent record-format change; save-data version remains `12`


## Phase 20 additions

- Version increased to `0.21.0-alpha`; persistent save-data version remains `12`
- Owner-accessible `/tr doctor` record consistency inspection without intentional dimension/chunk loading
- Operator-only `/tr doctor deep` verification of the saved exterior dimension, lower/upper blocks, shell, tile entity, UUID, owner metadata, and lock state
- Operator targeting through `/tr as <owner> doctor ...`
- Safe exterior structure synchronization and non-destructive missing-exterior restoration at the saved position
- Explicit destructive `/tr repair exterior force` recovery with detailed failure messages
- Interior recovery that verifies/recreates the door, console controls, engine core, transmat, and registered expandable rooms
- `/tr repair all [force]`, tab completion, configurable output limits, and repair audit logging
- In-game manual diagnostics page, release checklist, known-limitations document, and save-format migration policy
- Deep diagnostics load only the specifically requested saved exterior dimension/chunk; the normal doctor command remains record-only

## Current architectural rules

- One shared configurable dimension contains isolated TARDIS interior cells.
- Darkcore is a source and behavior reference only.
- Persistent records are authoritative; block and tile data link back to the TARDIS UUID.
- Energy, flight, room generation, shell changes, ownership changes, upgrades, crew permissions, and integration data reads are server-authoritative.
- Existing worlds default to public unlocked-door behavior during Phase 16 migration.
- Exterior locked/unlocked state and exterior access mode are separate checks.
- Owners and operators always bypass crew permission checks.
- Crew permission checks use UUIDs; names are display and command-completion metadata only.
- A revoked bound key remains an item but cannot operate the TARDIS.
- A flight freezes its resolved landing target at takeoff; later destination edits do not reroll that active flight.
- Player entities are not treated as landing obstructions; occupants are captured into the interior after successful materialization.
- Flight events are hooks, not replacements for validation: the flight service remains authoritative.
- Room `0` is protected; expandable rooms retain one primary parent connection and may store additional explicit secondary connections.
- Upgrade components are stored as stable type/count data rather than raw item stacks.
- Existing registry IDs are preserved even when visual models change.
- Operator-only forced landing remains deliberately destructive.
- Optional integrations must remain isolated: TARDIS Renewed must start with none of them installed.
- Phase 19 computer access is read-only; future write/control methods require explicit TARDIS permission and security design.

## Remaining room-system limitations

Secondary corridors are currently managed through commands and only connect horizontally adjacent rooms. A future visual room/schema interface can expose the same persistent graph without changing room IDs or save structure.

## Remaining integration limitations

Phase 19 deliberately starts with low-risk information hooks. AE2, external energy networks, richer computer control, and other mod-specific behavior are deferred until core save, permission, and recovery systems are closer to release stability.

## Release-candidate work remaining

The numbered feature roadmap is complete, but the mod is not yet presented as release-ready. Remaining work includes final assets and GUIs, optional-integration tests, true simultaneous multiplayer tests, survival balance, recipe review, performance profiling, long dedicated-server soak tests, packaging/legal review, and only then a beta/release version decision.

See `ROADMAP.md`, `PHASE20_TESTING.md`, `RELEASE_CHECKLIST.md`, and `KNOWN_LIMITATIONS.md`.
