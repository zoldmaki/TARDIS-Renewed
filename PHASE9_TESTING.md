# TARDIS Renewed Phase 9 testing

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.10.0-alpha.jar
```

## 1. Existing world migration

Load the Phase 8 world and enter the existing TARDIS interior. Five new prototype controls should be installed automatically around the old combined flight console. The room, TARDIS UUID, exterior, destination, lock state, and flight state must remain unchanged.

The old combined flight console remains intentionally present for save compatibility.

## 2. Destination command facing

Stand outside the TARDIS dimension and face a known direction.

```text
/tr destination here
/tr info
```

Expected: the destination uses the player's current position and copies the player's horizontal facing.

Now set an explicit facing:

```text
/tr destination here west
```

Expected: west overrides the player's facing.

Finally test a coordinate-only update:

```text
/tr destination ~5 ~ ~
```

Expected: the previous destination facing is preserved because no facing was supplied.

## 3. Dedicated controls

The controls are arranged around the legacy center console:

- Takeoff control: normal click starts flight; sneak-click shows status.
- Landing control: normal click requests exact landing; sneak-click requests nearest-safe landing.
- Destination control: click a face to move the destination one block in that direction; sneak-click moves it ten blocks.
- Facing control: normal click rotates clockwise; sneak-click rotates counter-clockwise.
- Status control: displays flight, exterior, lock, destination, and facing information.

## 4. Flight test

1. Set a valid destination outside the interior dimension.
2. Enter the TARDIS.
3. Use the takeoff control.
4. Check progress with the status control.
5. Change destination facing with the facing control.
6. Nudge the destination using different faces of the destination control.
7. After the timer completes, use the landing control.
8. Exit and confirm coordinates and facing match the adjusted destination.

## 5. Multiplayer/server interaction

On a dedicated server, repeat the control tests as a non-op owner. Each physical click must trigger exactly one action. A non-owner should be rejected, while an operator should be allowed for recovery/testing.

## 6. Restart persistence

Take off, alter destination/facing with the new controls, stop the server, restart, and check the status control. The flight timer, destination coordinates, and facing must persist.

## Known limitations

- These are deliberately simple cube controls, not the final console model or GUI.
- Destination nudging only edits an already-set destination.
- Normal landing uses exact mode; sneak-landing uses nearest-safe mode. Departure recovery remains command-only.
- Landing on an entity does not yet move that entity into the TARDIS. That behavior is planned for Phase 13 advanced flight.
