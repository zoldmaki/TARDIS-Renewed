# Phase 18 testing — room graph, transmat, gravity lift, and manual

Build with the normal Java 8 / Gradle 4.9 command:

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.19.0-alpha.jar
```

## 1. Existing-world migration

1. Back up the Phase 17 test world.
2. Load it with Phase 18.
3. Run `/tr info`, `/tr room list`, `/tr systems`, `/tr upgrades`, and `/tr crew list`.
4. Confirm the TARDIS UUID, owner, exterior, shell, destination, energy, damage, upgrades, crew, rooms, and flight state are unchanged.
5. Enter the interior. A transmat pad should appear near the rear-left corner of the console room without rebuilding the room.
6. Restart and confirm the pad and all old data remain stable.

## 2. Secondary room-corridor graph

Create a square-shaped arrangement whose adjacent rooms were created through different parents. Room IDs may differ, so check `/tr room list` after each command.

```text
/tr room add basic north
/tr room add basic east
/tr room add basic east 1
/tr room list
```

With the usual ID order, rooms `2` and `3` are adjacent but are not parent/child:

```text
/tr room connect 2 3
/tr room connections
```

Verify:

- A corridor opens between both existing rooms.
- Neither room is regenerated or moved.
- Their room IDs remain unchanged.
- Repeating the command is refused as a duplicate.
- Connecting non-adjacent rooms is refused.
- Connecting an original parent/child pair is refused because it already has its primary corridor.
- The secondary corridor persists after a world/server restart.

Then test:

```text
/tr room disconnect 2 3
```

The corridor should be removed and both doorways sealed without deleting either room. A room with a secondary connection must refuse `/tr room remove <id>` until the extra corridor is disconnected.

Occupant safety:

- Stand in a secondary corridor and run disconnect. It must refuse.
- As an operator, `/tr room disconnect <firstId> <secondId> force` should move occupants to the console room and remove the corridor.

## 3. Transmat pad

The console room receives a bound default pad automatically.

1. Land the exterior and remain near it.
2. Enter the TARDIS and right-click the active pad with an empty hand.
3. You should appear safely beside the exterior, spend exactly `50 AU`, and hear the transmat sound.
4. Use `/tr transmat` while outside and within 250 blocks of the exterior. You should return to the pad.
5. `/tr transmat status` should report the active pad, cost, and range.
6. Test with insufficient energy. It must refuse without teleporting.
7. Test while the TARDIS is in flight. It must refuse.
8. Test outside the exterior dimension and beyond 250 blocks. Both must refuse.
9. Restart and verify the active pad remains saved.

Alternate pad binding:

1. Place another `renewedtardis:tardis_transmat_pad` inside the same TARDIS cell.
2. Right-click it with a bound key for that TARDIS.
3. `/tr transmat status` should show the new position.
4. The old pad should report that it is inactive.
5. Breaking the active pad clears the saved point; re-entering the TARDIS should restore the safe default pad.

## 4. Sonic Transmat mode

1. Use Link mode on a linked TARDIS block or the active transmat pad.
2. Cycle to **Transmat** mode.
3. Standing outside within range, right-click air to enter the interior.
4. Standing near the active interior pad, right-click air to leave.
5. Revoke the tester's console/sonic permission and confirm the linked sonic stops working immediately.

## 5. Gravity lift

Give two pads:

```text
/give @p renewedtardis:gravity_lift 2
```

1. Place them on the same X/Z coordinate at least two blocks apart vertically.
2. Keep the entire shaft between them as air.
3. Right-click the lower pad until it says `up`.
4. Right-click the upper pad until it says `down`.
5. Stand centered on the lower pad and verify smooth upward transport.
6. Stand centered on the upper pad and verify downward transport.
7. Confirm fall damage is prevented during transport.
8. Obstruct the shaft; the lift should stop rather than moving the player through blocks.
9. Remove the paired pad; the remaining pad should stop safely.
10. Restart and verify each pad's direction persists through block state metadata.

## 6. TARDIS manual

Craft a book plus one Chronosteel Ingot, or use:

```text
/give @p renewedtardis:tardis_manual
```

- Right-click to read the current page and advance.
- Sneak-right-click to read the current page and move backward.
- Confirm the selected page persists on the item through logout/restart.

## 7. Dedicated server / non-operator regression

As a non-operator owner or appropriately permitted crew member, test:

- `/tr room list`, `/tr room connections`, `/tr room connect`, and `/tr room disconnect`
- `/tr transmat` and `/tr transmat status`
- Physical use of the transmat pad
- Sonic Transmat mode
- Gravity-lift movement

Also recheck exterior locking/entry, flight, landing, room installation/removal, engine repairs, upgrades, crew permissions, and sonic modes for regressions.
