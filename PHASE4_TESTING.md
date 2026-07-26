# Phase 4 testing — shared interior dimension

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

`build/libs/TARDIS-Renewed-0.5.0-alpha.jar`

## Client test

1. Start a world containing your existing Phase 3 TARDIS record and exterior.
2. Run `/tr info`. Existing ownership and exterior coordinates must remain intact.
3. Unlock the exterior with its matching key.
4. Right-click the exterior with an empty hand.
5. The first entry should create a simple quartz-and-chronosteel room in dimension `1212`.
6. Right-click the blue interior door to return outside.
7. Enter and exit several more times. The same room and cell number must be reused.
8. Restart the world and repeat the entry/exit test.

Debug commands:

```text
/tr interior
/tr exit
/tr info
```

`/tr interior` creates and enters your room without requiring the exterior to be unlocked. `/tr exit` returns to the saved exterior.

## Dedicated-server test

1. Start `runServer` and join it.
2. Give yourself operator permissions as before.
3. Create/place/unlock a TARDIS and enter it.
4. Confirm another non-op player can enter an unlocked exterior.
5. Confirm a locked exterior still refuses entry.
6. Stop and restart the server, then verify the same interior is retained.

## Expected prototype limitations

- The interior is a small generated test room, not a port of a legacy console schema yet.
- All interiors share one dimension but are separated by 2048 blocks.
- Dimension ID `1212` is fixed for this milestone; a config and conflict handling are planned.
- Death/respawn behavior inside the dimension has not been finalized. Avoid intentional death tests for now.
- The interior door is a single unbreakable prototype block.

## Report with a log if

- loading stops with a dimension-ID conflict;
- entry leaves the player in the void or inside a wall;
- returning places the player inside the exterior;
- a restart creates a second room or changes the cell number;
- a dedicated server references `net.minecraft.client` classes.
