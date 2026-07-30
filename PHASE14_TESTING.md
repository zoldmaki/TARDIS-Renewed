# Phase 14 testing — config GUI, damage, instability, and repair

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.15.0-alpha.jar
```

## 1. Forge Mods-menu configuration

From the Minecraft main menu:

1. Open **Mods**.
2. Select **TARDIS Renewed**.
3. Click **Config**.
4. Confirm the four categories appear: Dimensions, Flight and Navigation, Artron Energy, and Damage and Repair.
5. Change a harmless dynamic value such as `minimumFlightSeconds`, press Done, reopen the menu, and confirm it was saved.

The interior dimension ID is marked as requiring a complete Minecraft/server restart. Do not change it for an existing test world.

### Dimension blacklist editor

Open **Flight and Navigation → Blocked Destination Dimensions**. Add one integer ID per row. Examples:

- `-1` blocks the Nether.
- `1` blocks the End.
- A modded dimension is blocked by adding that mod's numeric dimension ID.

In the raw `renewedtardis.cfg`, the equivalent syntax is:

```text
I:blockedDestinationDimensions <
    -1
    1
    42
>
```

Do not use commas or brackets inside the list.

On a dedicated server, edit the server's own `config/renewedtardis.cfg`; a client's Mods-menu config does not replace the server configuration.

## 2. Existing-save migration

Load the Phase 13 world and run:

```text
/tr systems
/tr info
```

All four systems should initially read `100%`, and ownership, rooms, shell, destination, energy, and exterior data must remain unchanged.

Restart the world/server and confirm the values persist.

## 3. Normal wear

With the default config, perform one normal precise same-dimension flight.

After takeoff, run:

```text
/tr systems
```

Expected default wear:

- Engines: -1
- Navigation: -1
- Stabilizers: -1
- Dematerialization: -1

Landing must still work normally.

## 4. Fast and risky wear

```text
/tr systems repairall
/tr flight speed fast
/tr flight accuracy risky
```

Perform a flight. With default values, fast and risky profiles should apply additional targeted wear. `/tr systems` should show engines/navigation/stabilizers lower than the dematerialization matrix.

## 5. Damage penalties

As an operator:

```text
/tr systems set engines 50
/tr systems set stabilizers 50
/tr systems set navigation 40
```

Set a destination and inspect the predicted flight cost/status. Engine damage should increase Artron cost, stabilizer damage should increase travel time, and navigation damage can add extra horizontal drift even with a normally precise profile.

The resolved target remains frozen at takeoff and survives a restart.

## 6. Critical takeoff safeguard

```text
/tr systems set dematerialization 9
/tr takeoff
```

Takeoff should be refused with a clear critical-damage message. The exterior and Artron energy must remain unchanged.

Repeat with engines at `9`.

Recover with:

```text
/tr systems repairall
```

## 7. Chronosteel repair

1. Damage one or more systems with `/tr systems damage ...`.
2. Hold a Chronosteel ingot.
3. Right-click the prototype engine core.

One ingot should be consumed outside Creative mode, and the most damaged subsystem should gain the configured repair amount. At full health, no ingot should be consumed.

Right-click the engine core empty-handed for energy and system status. Sneak-right-click for the route-cost and repair summary.

## 8. Operator recovery and completion

```text
/tr systems damage navigation 25
/tr systems repair navigation 10
/tr systems set engines 75
/tr systems repairall
/tr as <owner> systems
/tr as <owner> systems set dematerialization 50
```

Tab completion should suggest actions and subsystem names.

## 9. Dedicated server regression

Confirm a non-operator can still:

- Lock/unlock and enter/exit the TARDIS.
- Read `/tr systems` and `/tr info`.
- Use normal console controls.
- Repair at the engine core with Chronosteel.

Administrative `damage`, `set`, `repair`, and `repairall` command actions must remain operator-only.
