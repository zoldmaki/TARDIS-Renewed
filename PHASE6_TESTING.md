# TARDIS Renewed Phase 6 testing

Phase 6 fixes duplicate multiplayer interactions and introduces the first persistent flight-state loop.

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.7.0-alpha.jar
```

## 1. Existing-world migration

Use a backup of the Phase 5 test world.

1. Start the world with the same interior dimension ID.
2. Run `/tr info`.
3. Confirm the TARDIS UUID, interior cell, exterior position, and owner are unchanged.
4. Enter the existing interior.
5. Confirm a blue prototype flight-console block appears in the center without rebuilding the room.

Existing saves receive these new defaults:

- flight state: `LANDED`
- exterior lock record: `locked` until synchronized by the existing tile or next key use/takeoff
- destination: not set

## 2. Dedicated-server double-action regression

Repeat this both as an operator and as a non-operator near protected spawn.

1. Hold the matching key.
2. Right-click the exterior once while standing.
3. Confirm it changes lock state exactly once.
4. Right-click once again and confirm it changes back exactly once.
5. Repeat while crouching.
6. Enter and exit with single clicks.

Expected result: no click may lock and immediately unlock, enter twice, or trigger two status messages.

## 3. Set a destination

Preferred survival-style method:

1. Stand in a non-TARDIS dimension with the bound key.
2. Sneak-right-click the top or side of a block.
3. The adjacent two-block space must be clear.
4. Run `/tr info` and confirm the destination coordinates and facing are saved.

Command alternatives:

```text
/tr destination
/tr destination <x> <y> <z>
/tr destination <x> <y> <z> <dimension>
/tr destination clear
```

With no coordinates, the command uses the block under the camera crosshair within 64 blocks and targets the adjacent face.

## 4. Basic flight loop

1. Set a destination away from the current exterior.
2. Enter the TARDIS.
3. Right-click the prototype console once.
4. Confirm the takeoff sound plays and `/tr info` reports `IN FLIGHT`.
5. Confirm the exterior was removed from the departure position.
6. Try the interior exit door. It must refuse while in flight.
7. Right-click the console once again.
8. Confirm the exterior appears at the destination.
9. Confirm `/tr info` reports `LANDED`, the destination is cleared, and the new exterior position is stored.
10. Exit through the interior door and confirm arrival beside the new exterior.

Sneak-right-clicking the console reports status without changing flight state.

## 5. Restart while in flight

1. Set a destination and take off.
2. Stop the server while the TARDIS is in flight.
3. Restart it.
4. Run `/tr info` and confirm `IN FLIGHT`, departure, and destination survived.
5. Enter with `/tr interior` if needed.
6. Land with the console or `/tr land`.

Expected result: no old exterior is recreated automatically, and landing uses the saved destination.

## 6. Obstructed landing recovery

1. Set a destination.
2. Take off.
3. Place blocks in the destination space using another player or server console.
4. Attempt to land.

Expected result:

- landing is refused
- the TARDIS remains `IN FLIGHT`
- no exterior is placed or duplicated
- a different destination can be supplied with explicit coordinates, then landing can be retried

Useful recovery commands:

```text
/tr destination <x> <y> <z> <dimension>
/tr takeoff
/tr land
/tr interior
/tr info
```

## 7. Lock-state preservation

Test once starting locked and once starting unlocked.

1. Set the desired lock state with the key.
2. Take off and land.
3. Confirm the materialized exterior retains that state.
4. Restart the server and check again.

## Known limitations

- Flight has no duration, energy cost, instability, damage, coordinate GUI, or advanced controls yet.
- The console is a temporary full-cube prototype and is owner-only, with operators allowed for recovery.
- Destination selection stores an exact block position and horizontal facing.
- No automatic safe-site search is performed if the chosen target becomes obstructed.
- The exterior and interior remain visual prototypes.
