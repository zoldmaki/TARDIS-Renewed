# Phase 17 testing — Sonic screwdriver foundation

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.18.0-alpha.jar
```

## Obtain the sonic

Use the recipe (top to bottom):

```text
Chronosteel Ingot
Redstone
Iron Ingot
```

Or use:

```text
/give @p renewedtardis:sonic_screwdriver
```

## Controls

- Sneak-right-click **air** to cycle modes.
- Sneak-right-click a **block** to use the current mode without activating the block normally.
- In Schematic mode, left-click an entity to scan it; the scan cancels attack damage.

The initial modes are:

1. Link
2. Locate
3. Reconfigure
4. Schematic Scan
5. Recall

## Core tests

### 1. Link and persistence

1. Leave the sonic in Link mode.
2. Sneak-right-click the exterior, interior door, engine core, console, or a console control.
3. Confirm the tooltip shows the linked owner and TARDIS UUID.
4. Relog and restart the server.
5. Confirm the same link and selected mode remain on the item.
6. Link it to a different TARDIS block while permitted and confirm the binding changes.

### 2. Locate

1. Cycle to Locate.
2. Right-click air while the exterior is landed in the same dimension.
3. Confirm the coordinates and approximate distance are reported.
4. Move to another dimension and confirm the dimension/coordinates are still reported.
5. Take off and confirm Locate reports flight progress and remaining time.

### 3. Door reconfiguration

1. Cycle to Reconfigure.
2. Sneak-right-click the linked exterior.
3. Confirm it toggles exactly once between locked and unlocked.
4. Enter the interior and use Reconfigure on the interior exit door.
5. Confirm the exterior lock state changes and persists after restart.
6. Try a sonic linked to another TARDIS; it must refuse.

Reconfigure requires key permission. Owners and operators bypass that check.

### 4. Schematic block and entity scans

1. Cycle to Schematic Scan.
2. Sneak-right-click several blocks.
3. Confirm registry ID, state, hardness, harvest information, and tile-entity type are reported.
4. Left-click a passive mob or another disposable test entity.
5. Confirm its name, class, health, and UUID are reported without dealing attack damage.

### 5. Recall

1. Land the linked TARDIS somewhere away from the player.
2. Confirm the current location contains two clear blocks.
3. Cycle to Recall and right-click air.
4. Confirm the destination becomes the player's current coordinates and facing.
5. Confirm normal takeoff checks still apply: Artron cost, subsystem health, route restrictions, and travel timer.
6. Stay at the recall position and land when ready.
7. Confirm materialization capture moves the player into the interior.

If recall takeoff fails, the previous saved destination should be restored.

Recall requires console permission and cannot target the shared TARDIS interior dimension.

## Crew and revocation tests

Suggested solo-server simulation:

1. As operator, add the current temporary username as `pilot` or `keyholder` crew on an older TARDIS.
2. De-op the player.
3. Confirm the sonic can link.
4. A pilot should be able to Locate, Reconfigure, and Recall.
5. A keyholder should be able to Locate and Reconfigure but Recall should refuse because it requires console permission.
6. Revoke the relevant permissions and confirm the already-bound sonic immediately stops performing protected actions.

## Dedicated-server regression

Verify that a non-operator player can:

- craft and hold the sonic;
- cycle modes;
- scan ordinary blocks/entities;
- use only those linked-TARDIS actions allowed by their current crew permissions.

Also retest normal key locking, exterior entry, console controls, engine-core repairs, room commands, takeoff, landing, and shell changes.
