# Phase 15 testing — upgrades and components

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.16.0-alpha.jar
```

## 1. Existing-save migration

Load the Phase 14 test world and run:

```text
/tr info
/tr upgrades
```

The TARDIS should report `0/8 slots` with all component counts at zero. Ownership,
rooms, shell, destination, exterior, energy, subsystem health, and flight settings must
remain unchanged. Restart once and confirm the empty upgrade state persists.

## 2. Items, recipes, and models

The creative tab should contain:

- Temporal Accelerator
- Artron Capacitor
- Artron Regenerator
- Architectural Expansion
- Temporal Shielding

Craft each recipe or create test items as an operator:

```text
/tr upgrades give speed
/tr upgrades give capacity
/tr upgrades give regeneration
/tr upgrades give room
/tr upgrades give protection
```

All five items should have textures and tooltips rather than missing-model squares.

## 3. Physical installation

1. Hold an upgrade component.
2. Right-click the linked engine core while landed.
3. Confirm one item is consumed outside Creative mode.
4. Confirm `/tr upgrades` and `/tr info` show the installed component.
5. Restart and confirm it remains installed.

A component must not be consumed when installation is refused because the per-type or
total-slot limit has been reached. Installation during flight must also be refused.

## 4. Capacity and regeneration

```text
/tr energy set 0
/tr upgrades install capacity
/tr upgrades install regeneration
/tr energy
```

With default configuration, one capacitor adds `2500 AU` to capacity and one regenerator
adds `5 AU/s`. Wait several seconds, run `/tr energy` again, then restart the world/server
and confirm regeneration continues from elapsed real time.

Removing a capacitor with an operator command must clamp stored energy if it is above the
new effective capacity.

## 5. Flight speed component

Set a suitably distant destination and compare the predicted/actual duration before and
after:

```text
/tr upgrades install speed
/tr flight
```

One Temporal Accelerator should reduce the final flight duration by the configured
percentage while leaving the chosen slow/normal/fast profile intact.

## 6. Room component

```text
/tr room list
/tr room schemas
/tr upgrades install room
/tr room list
/tr room schemas
```

The expandable-room limit should increase by four with defaults, and displayed room costs
should fall by fifteen percent. Creating a room must charge the reduced amount exactly
once. Existing rooms are never removed when an upgrade is later removed.

## 7. Protection component

Repair all systems, install Temporal Shielding, then perform a fast/risky or
cross-dimensional flight so the default wear is larger than one point:

```text
/tr systems repairall
/tr upgrades install protection
/tr systems
```

Takeoff wear should be lower than the same flight without shielding. Integer wear is
rounded upward, so a one-point base-wear event may still remain one point.

## 8. Limits and recovery commands

```text
/tr upgrades set speed 3
/tr upgrades remove speed
/tr upgrades clear
/tr as <owner> upgrades
/tr as <owner> upgrades set capacity 2
```

Counts must respect both the per-type maximum and the total slot count. Tab completion
should suggest actions and the five stable type names.

## 9. Config GUI

Open **Mods → TARDIS Renewed → Config → Upgrades and Components**. Confirm all slot,
limit, and effect values are visible and save correctly. On a dedicated server, the
server's own `config/renewedtardis.cfg` remains authoritative.

## 10. Dedicated-server regression

A non-operator owner should be able to:

- Read `/tr upgrades` and `/tr info`.
- Install held components at their engine core.
- Use all existing door, console, room, repair, and flight behavior.

A non-owner non-operator must not install components. Upgrade mutation commands remain
operator-only; normal gameplay installation uses the engine core.
