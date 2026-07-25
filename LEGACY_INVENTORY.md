# Legacy binary inventory

The uploaded 0.994-124 jar contains no `.java` files. The uploaded zip is the
same compiled content extracted into folders. Fortunately, the original project
also has a public source repository, so decompilation should only be a fallback
for matching this exact binary.

## Main legacy systems observed

- mod bootstrap and configuration
- block, item, tile entity, and command registration
- TARDIS dimension registry/provider/chunk provider
- ownership registry and per-TARDIS data store
- flight configuration, modifiers, takeoff, landing, and controls
- exterior and interior doors
- schema save/load/rotation system
- sonic screwdriver modes and extensions
- console, engine, core, battery, lab, landing pad, roundels
- damage system and upgrades
- custom packet handlers
- custom renderers, portal rendering, OBJ/Techne models
- AE2, Waila, Thaumcraft, CoFHCore, and IC2 integrations

## Asset summary from the uploaded jar

- 181 PNG textures
- 28 OBJ models
- 17 TCN model files
- 13 OGG sounds
- 21 schema files
- 2 language files
- supporting JSON, MTL, metadata, and schema index files

The original jar also bundles third-party API class files. Those must not be
copied into the new mod jar; proper 1.12.2 compile dependencies or optional
reflection/API modules should be used instead.
