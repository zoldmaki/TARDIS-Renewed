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

## Completed interior expansion foundation

### Phase 11 — interior schema and room management
Persistent room records, protected console room, reusable schema definitions, cardinal room grid, parent/child connectors, generated corridors, safe leaf-room removal, occupant protection, room teleportation, operator management, and first basic/workshop/storage schemas.

## Completed energy foundation

### Phase 12 — Artron energy and engine core
Persistent per-TARDIS capacity and charge, restart-safe regeneration, transactional flight and room costs, prototype engine core, status integration, configuration, and operator recovery commands.

## Completed advanced-flight foundation

### Phase 13 — advanced flight and materialization handling
Materialization occupant capture, persistent speed and accuracy profiles, restart-safe resolved landing targets, horizontal drift, dimension restrictions, Forge flight events, and improved status feedback.

## Completed damage foundation

### Phase 14 — damage, instability, repair, and config GUI
Forge Mods-menu configuration, persistent engine/navigation/stabilizer/dematerialization health, deterministic flight wear, energy/time/drift penalties, critical takeoff safeguards, Chronosteel repair, diagnostics, and operator recovery.

## Completed upgrade foundation

### Phase 15 — upgrades and components
Persistent component slots, five craftable upgrade types, engine-core installation,
flight/energy/room/protection effects, balancing configuration, status integration,
and operator recovery commands.

## Completed multiplayer permissions foundation

### Phase 16 — permissions and multiplayer crew
Persistent UUID-based crew lists, owner/crew/public exterior access modes, separate door/key/console/maintenance privileges, bound crew-key issuing, operator recovery, tab completion, and permission-aware physical interactions.

## Completed sonic foundation

### Phase 17 — sonic screwdriver
Persistent item linking and modes, ownership/crew-aware TARDIS actions, exterior/interior-door lock reconfiguration, location reporting, recall, block/entity schematic scans, sonic sound/model/recipe, and public block/entity action registration points.

## Completed legacy-mechanics foundation

### Phase 18 — additional legacy mechanics and room-graph expansion
Persistent secondary corridor links between already-adjacent rooms, safe connection removal, an interior/exterior Artron transmat with sonic mode, paired gravity lifts, and a lightweight in-game TARDIS manual. Decorative blocks, richer schema tooling, and additional legacy systems remain candidates for later polish.

## Completed optional-integration foundation

### Phase 19 — optional mod integrations
Restart-configurable HWYLA/Waila component tooltips, a reflection-only read-only ComputerCraft/CC:Tweaked engine-core peripheral, integration diagnostics, compile-only API isolation, and reliable server-authoritative gravity-lift level activation/teleportation. Broader AE2, energy, and other mod-specific hooks remain candidates after core stabilization.

## Completed stabilization foundation

### Phase 20 — diagnostics, recovery, and release stabilization foundation
Record-only and deep world diagnostics, safe/destructive exterior recovery, registered interior-component recovery, maintenance configuration, release documentation, known-limitations tracking, and a candidate save-format freeze. Final assets, survival balance, optional-integration verification, real two-player testing, and dedicated-server soak testing remain release-candidate work rather than a new numbered feature phase.

## Current console productization

### Phase 21A/B — physical console architecture foundation
Stable south/west/north/east panel identities, logical control registry, persistent control state, data-version 13 migration, four generated placeholder panels, orientation-aware hit regions, server-authoritative routing, interaction direction conventions, guarded controls, and the no-timeout four-control takeoff sequence.

### Phase 21C — console testability and navigation banks
Built-in exact control labels, data-version 14 navigation-bank migration, independent X/Y/Z and local/global lever values, functional destination reset, first authoritative coordinate updates, relative/absolute target preservation, and destination-facing synchronization. Dynamic dimension selection and the remaining panel gameplay stay in later Phase 21 steps.

### Phase 21D — navigation completion
Dynamic targetable-dimension selection, data-version 15 previous-materialized history, functional Previous and local/global Random controls, and full eight-digit relative horizontal encoding.

### Phase 21E — physical waypoint grid
Data-version 16 persistent 25-slot waypoint packages, physical 5×5 hit regions, read/write and current/destination controls, absolute storage with relative-mode loading, named-paper renaming, dye recoloring, and client-synchronized slot labels.

### Phase 21F — Main Flight panel controls
Data-version 17 synchronization of authoritative flight controls, sketch-inspired south-panel hit regions, physical slow/normal/fast selection, Exact/Midair, Surface, and Nearest Solid handbrake landings, persistent landing-pad preference, direction synchronization, and interactive Artron/progress readouts.

### Phase 21G — coordinated/uncoordinated travel state
Data-version 18 persistent route segments, destination-free uncoordinated takeoff, pause/resume across restarts, same-dimension travelled-position interpolation, route replacement from the already-travelled point, Artron-safe segment activation, normal uncoordinated landing, and sneak-handbrake early landing with random facing after any coordinated travel.

### Phase 21G.1 — interrupted-flight landing hotfix

Data-version 19 frozen travelled-position snapshots, restart-safe paused landing, and guarded coordinated-to-uncoordinated transitions.

## Deliberately outside scope

- Importing or converting 1.7.10 worlds
- Runtime dependency on Darkcore
- Preserving every old implementation detail where a clean 1.12.2 design is safer
