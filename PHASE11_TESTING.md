# Phase 11 testing — interior schemas and expandable rooms

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.12.0-alpha.jar
```

Test both `runClient` and a dedicated server. Existing Phase 10 worlds should load without changing their TARDIS UUID, owner, exterior, shell, lock state, destination, flight state, or console room.

## Command overview

```text
/tr room
/tr room list
/tr room schemas
/tr room add <schema> <north|south|east|west> [parentRoomId]
/tr room remove <roomId> [force]
/tr room tp <roomId>
```

Room `0` is always the protected console room. Expandable rooms begin at ID `1`.

Available first-pass schemas:

```text
basic
workshop
storage
```

Operator control works through the existing administration wrapper:

```text
/tr as <owner> room list
/tr as <owner> room add storage north
/tr as <owner> room tp 1
/tr as <owner> room remove 1 force
```

## Existing-world migration

1. Open an existing Phase 10 world.
2. Run `/tr info`.
3. Confirm the record still has the same owner, TARDIS UUID, interior cell, exterior position, shell, lock state, destination, and flight state.
4. Confirm the new line reports `1 protected console room + 0 expandable room(s)`.
5. Enter and exit normally.

Old records receive an empty room list and the first new room ID will be `1`.

## Schema and first-room test

1. Run `/tr room schemas`.
2. Run `/tr room add basic north`.
3. Confirm a corridor opens through the north wall of the console room.
4. Walk through it and confirm a finished quartz room exists.
5. Run `/tr room list` and confirm room `1` is at grid `0,-1`, connected north of room `0`.
6. Run `/tr room tp 1` and confirm you appear safely inside the room.
7. Run `/tr room tp 0` and confirm you return to the console room.

## Branching and occupancy tests

Create several connected rooms:

```text
/tr room add workshop east
/tr room add storage north 1
/tr room add basic west 1
```

Expected behavior:

- Every room receives a unique persistent ID.
- A child room may use another expandable room as its parent.
- Corridors connect only the selected parent and child.
- Attempting to add a room into an occupied grid cell is refused.
- Attempting to connect back into grid `0,0` is refused because the console room is protected.
- Tab completion offers schema names, directions, and known room IDs.

## Persistence test

1. Create at least three rooms.
2. Note their IDs and positions from `/tr room list`.
3. Stop the client/server cleanly.
4. Restart the same world.
5. Run `/tr room list` again.
6. Confirm every room record and generated structure remains.
7. Confirm entering the TARDIS does not regenerate or overwrite decorated room interiors.
8. Add another room and confirm its ID continues upward rather than reusing an existing ID.

## Safe removal tests

### Empty leaf room

1. Stand in the console room.
2. Remove a room that has no children:

```text
/tr room remove <id>
```

3. Confirm its room and corridor are cleared.
4. Confirm the parent doorway is sealed.
5. Confirm the record disappears from `/tr room list`.

### Child protection

1. Try to remove a room that still has child rooms.
2. Confirm removal is refused and reports the number of children.
3. Remove the child rooms first, then remove the parent.

### Player safety

1. Stand inside the target room or its corridor.
2. Run `/tr room remove <id>`.
3. Confirm removal is refused because a player is inside the removal area.
4. As an operator, run:

```text
/tr room remove <id> force
```

5. Confirm the player is moved to the console room before the structure is cleared.

`force` does not bypass the child-room check. Children must still be removed first so no persistent room graph becomes disconnected.

## Dedicated-server checks

- A normal non-operator owner can list, add, teleport to, and safely remove their own rooms.
- A normal player cannot use `/tr as` or forced removal.
- Operators can manage another saved TARDIS with `/tr as <owner> room ...`.
- Restarting the dedicated server preserves room IDs, schemas, parent links, grid positions, and generated states.
- Existing exterior locking, entry, flight, landing, shell changing, and ownership-transfer tests still pass.

## Known Phase 11 limitations

- Room management is command-driven; there is no schema item, GUI, or console control yet.
- The three room designs are intentionally simple generated test schemas.
- Players may manually break or decorate room blocks; only command-based deletion protects the core and checks occupants.
- Generated rooms are not automatically rebuilt after manual damage, which prevents the system from overwriting player decoration.
- Room costs and Artron energy are not implemented until Phase 12.
