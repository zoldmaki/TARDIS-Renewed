# TARDIS Renewed porting status

## Fixed project baseline

- Legacy behavior reference: TARDIS Mod `1.7.10-0.995-350`
- Darkcore behavior reference: `0.41-179`
- Target: Minecraft Forge `1.12.2-14.23.5.2847`
- Java: Eclipse Temurin 8
- ForgeGradle: `2.3-SNAPSHOT`
- Mappings: `stable_39`
- Public name: **TARDIS Renewed**
- Registry namespace: `renewedtardis`
- Current version: `0.7.0-alpha`
- Legacy-world import: intentionally out of scope

## Completed

### Phase 0 — loading scaffold

- ForgeGradle project and Java 8 setup
- Mod metadata, logo, legacy resource inventory

### Phase 1 — registry foundation

- Registry-event structure
- Creative tab
- Chronosteel ingot and block
- Legacy sound-event registration

### Phase 2 — persistent identity

- Overworld `WorldSavedData` registry
- UUID-based TARDIS and owner records
- Bound TARDIS key
- Development command surface

### Phase 3 — exterior foundation

- Persistent prototype exterior tile entity
- Key binding and lock state
- Safe debug placement and relocation

### Phase 4 — interior dimension

- Shared empty interior dimension
- Persistent, widely separated interior cells
- Prototype room generation
- Two-way exterior/interior teleportation
- Existing Phase 2–3 data preserved

### Phase 5 — multiplayer and configuration hardening

- Configurable shared interior dimension ID
- Existing-record/config mismatch detection
- Exterior placement forbidden from inside the TARDIS dimension
- Narrowly scoped client-to-server interaction packet
- Non-operator lock, unlock, entry, and exit support near protected spawn
- Stronger first-generation lighting updates

### Phase 6 — flight-state foundation

- Server-side duplicate interaction suppression for custom-packet plus vanilla activation pairs
- Persistent `LANDED` and `IN FLIGHT` state
- Persistent departure, destination, landing facing, and exterior lock state
- Bound-key destination selection by sneak-right-clicking a block
- Crosshair and explicit-coordinate destination commands
- Temporary owner-controlled flight console added to new and existing interiors
- Server-authoritative exterior dematerialization and materialization
- Exit-door lockout while in flight
- Restart-safe in-flight recovery and obstructed-landing refusal
- Takeoff and landing debug/recovery commands

## Architectural decisions

- One shared dimension contains isolated TARDIS interior cells.
- Interior cells are separated by 2,048 blocks.
- The default dimension ID is 1212 but can be changed before world creation.
- A changed dimension ID does not migrate old interiors; mismatches fail clearly.
- Darkcore is not a runtime dependency. Required behavior is replaced internally.
- TARDIS block interactions are server-authoritative and narrowly validated.
- One physical click may arrive through both vanilla and the custom protected-block path; server-side deduplication prevents double activation.
- During flight, the current exterior position is cleared while the departure and destination remain recoverable in persistent data.
- Visual prototype blocks may later be replaced without changing persistent identity or flight records.

## Next planned phase

Phase 7 should strengthen flight and exterior recovery before detailed rendering:

- controlled flight duration rather than instant second-click landing
- basic coordinate/dimension validation rules and safe landing search
- flight progress and state synchronization for clients
- handling players joining or logging out while inside an in-flight TARDIS
- owner/guest permission groundwork
- exterior recovery command for missing or corrupted placements

Energy, damage, instability, animated console controls, chameleon forms, and advanced navigation should remain later milestones until the flight state machine passes multiplayer and restart testing.
