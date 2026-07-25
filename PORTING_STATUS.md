# TARDIS Renewed porting status

## Canonical legacy references

- TARDIS Mod 1.7.10: `0.995-350`
- Darkcore 1.7.10: `0.41-179`
- Target: Minecraft 1.12.2 / Forge `14.23.5.2847`
- Mappings: `stable_39`
- Java: Eclipse Temurin 8
- Legacy-world importing: intentionally out of scope

## Stable project identity

- Public name: **TARDIS Renewed**
- Mod ID / registry namespace: `renewedtardis`
- Java package root: `dev.zoldmaki.renewedtardis`

The mod ID should not be changed after public worlds begin using the mod.

## Completed

### Phase 0

- ForgeGradle 2.3 loading scaffold
- Java 8 build configuration
- Mod metadata and logo
- Legacy resource inventory

### Phase 1

- Registry-event structure
- Client-only item/block model registration
- TARDIS Renewed creative tab
- Chronosteel ingot and block
- Sound-event placeholders using the legacy audio assets

### Phase 2

- Native global `WorldSavedData`
- UUID-based TARDIS records and ownership
- Persistent key binding
- `/tardisrenewed` and `/tr` test commands
- Reserved interior-dimension and exterior-position data

### Phase 3

- Fixed TARDIS Key recipe JSON
- Prototype TARDIS exterior block
- Persistent exterior tile entity
- Saved TARDIS-to-exterior links
- Matching-key lock and unlock behavior
- Safe `/tr exterior` placement and relocation command
- Exterior position cleanup when the linked block is broken

## Current version

`0.4.0-alpha`

## Current architectural boundary

The Phase 3 exterior is a one-block functional prototype, not a commitment to
the final exterior shape. The important stable pieces are the TARDIS UUID,
owner UUID, exterior location, and lock state.

The next major architectural decision is the interior layout strategy:

1. one dynamically registered dimension per TARDIS, close to the original mod;
2. one shared TARDIS dimension with widely separated interior regions.

The choice should be made before implementing real entry teleportation because
it affects save structure, chunk loading, recovery tools, and future features.

## Next recommended phase

Phase 4 should establish the interior dimension skeleton, safe teleportation,
a return location, and recovery commands. It should use a deliberately simple
test room before importing schemas or console mechanics.
