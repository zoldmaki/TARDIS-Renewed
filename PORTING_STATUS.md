# TARDIS Renewed porting status

## Baseline

- Legacy reference: TARDIS Mod `1.7.10-0.995-350`
- Darkcore reference: `0.41-179`
- Target: Forge `1.12.2-14.23.5.2847`, Java 8, ForgeGradle `2.3-SNAPSHOT`, mappings `stable_39`
- Public name: **TARDIS Renewed**
- Registry namespace: `renewedtardis`
- Current version: `0.22.6-alpha`
- Save-data version: `18`
- Legacy-world import: out of scope

## Completed through Phase 21G

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

## Phase 21A/B additions

- Stable logical identities for all four physical console panels and the sketched controls
- Corrected orientation: Navigation west/left, Utility east/right, Main Flight south, Rooms north
- Persistent per-TARDIS control states and no-timeout takeoff-sequence progress
- Save-data version increased to `13`; Phase 20 records receive safe default console state
- Four generated and repairable placeholder panel blocks around the compatibility console
- Orientation-aware 4x4 hit-region routing with server-authoritative permission checks
- Right-click increment, sneak-right-click decrement, pull-switch toggling, guarded actions, and Y-sign disable behavior
- Temporal Primer → Helmic Regulator → Spatial Link → Quantum Handbrake sequence
- Existing takeoff/landing service routing through the Quantum Handbrake
- Quick-time eligibility metadata without active quick-time events

## Phase 21C additions

- Built-in exact placeholder-control labels under the crosshair, independent of HWYLA
- Independent X/Y/Z and local/global navigation lever banks with per-axis signs
- Save-data version increased to `14`; Phase 21A/B shared lever state migrates into the selected bank
- Functional Navigation Reset and first authoritative destination updates from physical coordinate controls
- Absolute/relative target preservation and destination-facing synchronization
- Dynamic dimension cycling remained deferred at the end of Phase 21C

## Phase 21D additions

- Dynamic Y-lever dimension cycling across registered, targetable dimensions
- Relative mode keeps the dimension selector locked to the exterior dimension
- Persistent previous-materialized position and facing recorded when a flight completes
- Functional Previous button that restores the last materialized location rather than the last selected target
- Scale-aware randomizer: local generates within 9,999 blocks of the exterior; global uses the legal world range
- Randomization preserves Y and facing while updating the authoritative destination and physical lever banks
- Relative horizontal encoding now supports the full eight-digit lever range when the final absolute target remains legal
- Save-data version increased to `15`; older records begin with no previous materialized position until they complete a flight

## Phase 21E additions

- Functional physical 5×5 waypoint grid with 25 persistent slots
- Write/Read mode and Current/Destination write-source controls
- Absolute waypoint packages storing position, dimension, facing, landing mode, and landing-pad preference
- Saved waypoints remain absolute and load correctly while the console is in either absolute or relative mode
- Relative mode can retain a target dimension separate from the exterior dimension while its X/Y/Z levers remain exterior-relative offsets
- Named paper renames waypoint slots; dyes recolor their built-in crosshair labels
- Client-visible waypoint metadata is synchronized through the Navigation panel tile entity without making client data authoritative
- Navigation placeholder hit regions now expose all 25 waypoint buttons in a sketch-inspired 5×5 grid
- Save-data version increased to `16`; older records begin with empty, default-named waypoint slots

## Phase 21F additions

- Save-data version increased to `17`; authoritative flight speed, destination direction, and handbrake state synchronize with the physical Main Flight controls on load
- The previously non-functional drive-mode placeholder is normalized to Coordinated until the coordinated/uncoordinated state machine is implemented
- Sketch-inspired south-panel hit regions expose the three gauges, seven progress lamps, radial speed lever, handbrake, landing controls, direction dial, and event placeholders
- Physical slow/normal/fast selection is connected to the existing persistent timed-flight profiles and cannot be changed after dematerialization
- Quantum Handbrake landing now respects Exact/Midair, Surface, and Nearest Solid modes
- Surface ignores entered Y and finds the highest valid support at target X/Z
- Nearest Solid prioritizes valid support closest to entered Y before expanding horizontally within the configured safe-landing radius
- Landing-pad preference persists and reports an explicit fallback until dedicated pad blocks exist
- Artron and timed-flight progress controls provide live server-authoritative readouts; Shield and TARDIS XP gauges honestly remain reserved
- `/tr land surface` and `/tr land solid` are available alongside the existing recovery landing modes

## Phase 21G additions

- Save-data version increased to `18` with persistent requested/resolved route targets, segment start, duration, elapsed progress, active timestamp, and coordinated-travel history
- Functional Coordinated/Uncoordinated drive pull control before takeoff and while airborne
- Destination-free uncoordinated takeoff that remains at departure until a coordinated route is engaged
- Restart-safe pause/resume preserving travelled percentage and reconstructed position
- Same-dimension X/Y/Z interpolation for interrupted landings
- Active route remains frozen while selected navigation values are edited; leaving and re-engaging Coordinated commits a changed destination as a new segment from the travelled point
- Normal handbrake landing while Uncoordinated and sneak-handbrake early landing before coordinated readiness
- Random horizontal facing after any coordinated travel; departure facing when the flight remained purely uncoordinated
- Interrupted landing replaces Navigation's target with the actual materialized position while Previous still refers to departure
- Base/incremental/new-segment Artron charging prevents uncoordinated takeoff from bypassing route costs
- Relative navigation and current-position waypoint actions use the reconstructed in-flight position
- Cross-dimensional interrupted travel remains at the current segment start until that dimension transition completes


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
- Each active coordinated route segment freezes its resolved landing target. Later destination edits become the selected target for a replacement segment only after the drive is switched to Uncoordinated and re-engaged.
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

The original Phase 0–20 foundation roadmap is complete; Phase 21 now begins the physical console productization work, and the mod is not yet presented as release-ready. Remaining work includes final assets and GUIs, optional-integration tests, true simultaneous multiplayer tests, survival balance, recipe review, performance profiling, long dedicated-server soak tests, packaging/legal review, and only then a beta/release version decision.

See `ROADMAP.md`, `PHASE21_CONSOLE_SPEC.md`, `PHASE21_TESTING.md`, `PHASE21_VALIDATION.md`, `RELEASE_CHECKLIST.md`, and `KNOWN_LIMITATIONS.md`.

## Phase 21G.1 interrupted-flight landing hotfix

- Save-data version `19` persists an immutable paused travelled-position snapshot.
- Coordinated-to-Uncoordinated transitions freeze the exact reconstructed position before stopping route time.
- Uncoordinated handbrake landing uses the frozen position directly and survives restart without advancing to the route target.
- Failed pause transitions no longer switch the visible drive mode while leaving travel active.
