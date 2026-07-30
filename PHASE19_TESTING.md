# Phase 19 testing — optional integrations and gravity-lift correction

## Build target

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.20.0-alpha.jar
```

The released jar must not contain `mcp/mobius/waila/api/` or any
`dan200/computercraft/api/` classes.

## 1. Existing-world regression

1. Load the Phase 18 test world.
2. Run `/tr info`, `/tr room list`, `/tr energy`, `/tr systems`, and `/tr upgrades`.
3. Enter and exit the TARDIS.
4. Take off and land once.
5. Confirm ownership, exterior, shell, lock, destination, rooms, energy, systems, upgrades, crew, sonic link, transmat, and secondary corridors are unchanged.

Save-data version remains `12`; Phase 19 adds no new persistent TARDIS fields.

## 2. Gravity-lift input and phasing

Create two gravity-lift level blocks on the same X/Z coordinate with a clear
vertical shaft. Keep at least two blocks between their Y levels.

1. Right-click the lower block until it reports `up`.
2. Stand on it and do nothing. It must not move you automatically.
3. Press jump. You should travel through the upper solid level block and arrive standing safely on it.
4. Right-click the upper block until it reports `down`.
5. Stand on it and hold sneak. You should travel through the source and lower blocks and arrive safely on the lower level.
6. Repeat both directions several times. You must not remain in `noClip`, fall through ordinary floors, or become embedded after arrival.
7. Step sideways out of the lift column during travel. The trip should stop and normal collision must return.
8. Break or reverse one level while travelling in a disposable test. Normal collision must return rather than leaving the player permanently phased.
9. Block the shaft with an ordinary block. The lift must refuse to start.
10. Verify Creative flight does not trigger the lift accidentally.

The direction remains stored in block state, so existing Phase 18 lift blocks
migrate automatically.

## 3. Configuration GUI

Open:

```text
Mods -> TARDIS Renewed -> Config -> Optional Integrations
```

Confirm these restart-required options exist:

- Enable HWYLA/Waila Tooltips
- Enable ComputerCraft Peripheral

On a dedicated server, edit the server's own `config/renewedtardis.cfg`; the
client Mods-menu screen only changes the client's local file.

## 4. Integration status command

Run:

```text
/tr integrations
```

Without either optional mod installed, it should report both as `not installed`
and the rest of TARDIS Renewed must work normally.

Disable either option in the config, restart, and confirm the command reports
`disabled in config`.

## 5. HWYLA/Waila test (optional)

Install a compatible Minecraft 1.12.2 HWYLA/Waila build on both sides as that
mod requires, enable the integration, and restart.

Look at:

- exterior
- interior exit door
- flight console and dedicated controls
- engine core
- transmat pad
- gravity lift

Expected information includes owner/state, flight progress, exterior lock and
shell, engine energy and lowest subsystem, transmat readiness, and gravity-lift
direction/input.

Also verify:

1. An unbound component says it is unbound instead of crashing.
2. Tooltip data updates after lock, takeoff, landing, damage, repair, and energy changes.
3. Removing HWYLA/Waila again does not prevent TARDIS Renewed from starting.

## 6. ComputerCraft or CC:Tweaked test (optional)

Install a compatible Minecraft 1.12.2 ComputerCraft-family build, enable the
integration, and restart. Place a wired or wireless modem next to the linked
TARDIS engine core and attach it from a computer.

The peripheral type is:

```text
tardis
```

Read-only methods:

```text
getId()
getOwner()
getState()
getFlightProgress()
getEnergy()
getDestination()
getExterior()
getSystems()
getUpgrades()
getRooms()
getAccess()
```

Example Lua smoke test:

```lua
local t = peripheral.find("tardis")
assert(t, "No TARDIS peripheral found")
print(t.getId())
print(t.getOwner())
print(t.getState())
print(t.getEnergy())
```

Verify values update after takeoff, landing, energy use/regeneration, damage,
repair, upgrade installation, room installation, and access changes.

The peripheral intentionally has no takeoff, landing, destination, lock, repair,
or upgrade-writing methods in Phase 19.

Also verify:

1. Breaking the engine core makes old peripheral calls fail safely rather than modifying stale data.
2. Removing ComputerCraft again does not prevent TARDIS Renewed from starting.
3. `/tr integrations` reports registration failure details if an incompatible API build is deliberately tested.

## 7. Dedicated-server regression

1. Start a clean dedicated server without optional integration mods.
2. Join as a non-operator and use normal exterior, interior, crew, sonic, transmat, and gravity-lift interactions.
3. Restart and repeat.
4. Confirm no client-only class crash and no missing optional-API class crash occurs.
