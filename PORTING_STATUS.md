# TARDIS Renewed Porting Status

## Fixed project baseline

- Display name: **TARDIS Renewed**
- Target: Minecraft 1.12.2
- Forge: 14.23.5.2847
- Mappings: stable_39
- Java: Temurin 8u492
- Gradle: 4.9
- Legacy TARDIS reference: 1.7.10-0.995-350
- Legacy Darkcore reference: 0.41-179
- Mod ID / namespace: `renewedtardis`
- Java package: `dev.zoldmaki.renewedtardis`
- Save policy: native 1.12.2 worlds only; no 1.7.10 world importer

## Completed

### Phase 0

- ForgeGradle project scaffold
- Mod metadata and logo
- Initial legacy resource archive
- Successful clean build and client launch

### Phase 1

- Forge 1.12.2 registry-event structure
- Client-only model registration isolated from common/server code
- TARDIS Renewed creative tab
- Chronosteel Ingot and Chronosteel Block
- Chronosteel crafting and ore-dictionary entries
- Legacy sounds copied into the new namespace and registered

### Phase 2

- Public display name changed to **TARDIS Renewed**
- First internal Darkcore replacement: dimension-aware block-position value object
- Versioned native `WorldSavedData` TARDIS registry
- UUID-based TARDIS identity and UUID-based player ownership
- Reserved persistent fields for interior dimension and exterior position
- One-TARDIS-per-owner lookup and creation
- Functional TARDIS Key item with legacy texture
- Unbound keys bind to the player's persistent TARDIS record on right-click
- Bound-key owner and TARDIS identity stored in item NBT
- Non-destructive `/tardisrenewed` command surface with `/tr` alias
- Legacy four-iron TARDIS Key recipe ported

## Intentional design decisions

- The public name is **TARDIS Renewed**, while the stable registry namespace remains `renewedtardis`.
- TARDIS identity uses a UUID and is not tied directly to a dimension number.
- Player ownership uses UUIDs; names are retained only for display and are refreshed when seen.
- World data lives in the overworld's `MapStorage`, so it is independent of TARDIS dimension loading.
- Old metadata-heavy blocks/items will normally become separate registry entries.
- Darkcore will not be a required runtime mod. Needed features will be replaced internally.
- Old assets may be used as placeholders and replaced with Blockbench models/textures later.
- Registry names must remain stable once worlds containing the content are created.
- Legacy-world migration is outside project scope.

## Not ported yet

- TARDIS exterior block and tile entity
- interior dimension registration and allocation
- teleportation and door linking
- console and flight mechanics
- screwdriver
- GUI/networking
- schemas and room generation
- damage, upgrades, energy, and integrations

## Next proposed milestone

Phase 3 should build the first safe dimension prototype:

1. native TARDIS dimension type and world provider
2. deterministic interior-dimension allocation linked to `TardisRecord`
3. a small temporary interior platform for testing
4. server-authoritative teleport helper with safe placement
5. `/tr enter` and `/tr exit` debug commands
6. restart, death, logout, and dedicated-server smoke tests

The exterior block should come only after entering and leaving an assigned interior works reliably.
