# Main limitations and risks

## 1. This is a rewrite across several major Forge API changes

Minecraft 1.7.10 used the old `cpw.mods.fml` lifecycle, numeric-era registration
patterns, old block states, old rendering hooks, and different world/dimension
APIs. Minecraft 1.12.2 uses `net.minecraftforge.fml`, registry events,
`ResourceLocation` registry names, JSON block/item models, capabilities, and
newer networking conventions. Most classes need deliberate conversion rather
than import replacement.

## 2. Darkcore is deeply embedded

The old mod does not merely load Darkcore; it extends and calls its abstract
blocks/items/commands, config system, network packet layer, coordinate stores,
teleport helpers, chunk loading, and utility collections. Darkcore only has an
official 1.7.10 release. We must either:

- port the required Darkcore subset as a separate 1.12.2 library, or
- replace those calls with Forge/vanilla code inside Renewed TARDIS.

The second option is cleaner for long-term maintenance, but it means more early
work.

## 3. World-save compatibility cannot be assumed

The old mod predates modern namespaced registry handling and stores significant
state in dimensions, tile entities, NBT, ownership registries, and custom save
data. Loading a 1.7.10 world directly in 1.12.2 is unsafe without a dedicated
migration design. Initial releases should use new worlds. A converter can be a
later milestone.

## 4. Dimension and teleportation code is high risk

The core fantasy depends on many interiors, exterior locations, door pairing,
chunk loading, safe teleportation, dimension unloading, and recovery after
server restarts. These systems can cause duplicate dimension IDs, lost players,
or corrupted links if ported carelessly. They need server-first tests and
recovery commands.

## 5. Rendering must be rebuilt in layers

The jar contains many custom renderers and old model formats (`.obj`, `.tcn`).
Some OBJ assets can be reused, but old `IItemRenderer`, TESR, OpenGL-state, and
texture-registration code will not drop into 1.12.2. Simple JSON models should
come first; complex console/exterior renderers come after mechanics are stable.

## 6. Networking must be rewritten and hardened

The old packet layer is based on Darkcore and older FML networking. Every packet
needs a 1.12.2 `SimpleNetworkWrapper` equivalent, explicit side handling, main
thread scheduling, bounds checks, and permission validation. Client requests
must never be trusted to change ownership, flight, dimensions, or inventories.

## 7. Optional integrations changed versions and APIs

AE2, Waila/HWYLA, Thaumcraft, CoFH/RF, IC2, ComputerCraft, and other integrations
need their matching 1.12.2 APIs. They should be optional modules, never copied
as bundled API classes from the old jar. Some old integration behavior may be
removed or redesigned when no clean 1.12.2 equivalent exists.

## 8. The original source baseline must be chosen

The attached binary is 0.994-124, while the public repository contains a later
0.995 code line. Before deep porting, choose whether Renewed TARDIS follows the
attached release exactly or starts from the latest original source. Mixing both
without a baseline will create hard-to-trace behavior differences.

## 9. Scope growth can overwhelm the port

Feature extensions are possible, but adding them while foundational systems are
still moving will multiply bugs. New mechanics should be queued behind stable
milestones unless a redesign must happen before save formats are frozen.

## 10. Testing will be a major part of development

At minimum each milestone needs:

- development client startup
- dedicated server startup
- new world creation and restart
- player death/logout while inside a TARDIS
- dimension unload/reload
- two-player ownership and permissions
- malformed/missing NBT recovery
- no-client-class loading on a server
