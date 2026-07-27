# TARDIS Renewed development roadmap

This roadmap is intentionally modular. A phase may be split when testing reveals that a subsystem needs more isolation.

## Completed foundation

### Phase 0 — development scaffold
ForgeGradle/Java 8 project, metadata, logo, legacy resource inventory, and first loading test.

### Phase 1 — registry foundation
Forge registry events, creative tab, Chronosteel content, recipes, models, localization, and legacy sounds.

### Phase 2 — persistent identity
Native `WorldSavedData`, TARDIS/owner UUIDs, bound keys, and the first development commands.

### Phase 3 — exterior foundation
Persistent exterior link, locking, key validation, and safe debug placement/relocation.

### Phase 4 — interior dimension
Shared interior dimension, isolated cells, prototype room generation, and two-way teleportation.

### Phase 5 — multiplayer/config hardening
Configurable dimension ID, non-operator interactions, packet validation, dedicated-server fixes, and lighting updates.

### Phase 6 — flight foundation
Persistent landed/in-flight state, destinations, console prototype, dematerialization/materialization, and restart recovery.

### Phase 7 — navigation/admin tools
Relative coordinates, `here`, facing selection, ownership transfer, tab completion, and operator control of other TARDISes.

### Phase 8 — timed flight and landing recovery
Real elapsed flight duration, persistent progress through restarts, exact/nearby/departure landing modes, and operator timer bypass.

## Completed presentation foundations

### Phase 9 — proper prototype console controls
Separate destination, facing, takeoff, landing, and status controls with reusable control/tile-entity architecture.

### Phase 10 — exterior model and chameleon foundation
Two-block exterior structure, collision and front-door behavior, door sounds, persistent shell IDs, prototype/police-box shells, and operator-aware shell commands.

## Planned gameplay systems

### Phase 11 — interior schema and room management
Move beyond the fixed test room: schema definitions, room placement/removal, protected core area, door connectors, and safe interior expansion.

### Phase 12 — Artron energy and engine core
Persistent energy capacity/usage/regeneration, flight costs, engine/core blocks, charging rules, and operator recovery commands.

### Phase 13 — advanced flight
Coordinate controls, flight accuracy, speed modifiers, randomization, dimension restrictions, flight events, and improved console feedback.

### Phase 14 — damage, instability, and repair
Subsystem health, flight faults, exterior/interior damage, repair mechanics, emergency states, and safeguards against unrecoverable records.

### Phase 15 — upgrades and components
Upgrade inventory, component framework, speed/energy/room/protection upgrades, crafting progression, and balancing configuration.

### Phase 16 — permissions and multiplayer crew
Owner/guest permission lists, lock modes, console permissions, key permissions, ownership recovery, and crew-friendly command handling.

### Phase 17 — sonic screwdriver
Sonic item state, exterior/door interaction, block/entity actions, permissions, sounds, models, and extensible action registration.

### Phase 18 — additional legacy mechanics
Schemas/manual, transmat, gravity lift, decorative systems, special blocks, and other legacy features selected by priority.

### Phase 19 — optional mod integrations
Separate optional integrations for supported 1.12.2 APIs such as Waila/HWYLA, ComputerCraft, AE2, energy systems, and other chosen mods. No bundled third-party API classes.

### Phase 20 — polish, compatibility, and release stabilization
Final assets and GUIs, configuration cleanup, performance/chunk-loading review, dedicated-server soak tests, recovery tools, documentation, recipes, balancing, and save-format freeze for beta/release.

## Deliberately outside scope

- Importing or converting 1.7.10 worlds
- Runtime dependency on Darkcore
- Preserving every old implementation detail where a clean 1.12.2 design is safer
