# TARDIS Renewed Phase 10 testing

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.11.0-alpha.jar
```

## 1. Existing world migration

Load the Phase 9 test world and visit the existing exterior.

Expected:

- TARDIS UUID, owner, lock, destination, interior, and flight data remain unchanged.
- The existing lower exterior automatically gains a linked upper half when its chunk loads.
- An older record remains on the `prototype` shell until deliberately changed.

Check:

```text
/tr info
/tr shell
```

## 2. Chameleon/shell selection

Run:

```text
/tr shell list
/tr shell police_box
/tr shell
/tr info
```

Expected: the placed exterior immediately becomes the two-block police-box model, and `/tr info` reports `shell=police_box`.

Switch back and forth:

```text
/tr shell prototype
/tr shell police_box
```

Restart the world/server and confirm the selected shell remains. The exterior must remain bound throughout every shell change; it must not require a key rebind and `/tr info` must keep the same owner, UUID, and exterior position.

Operator test:

```text
/tr as <owner> shell prototype
/tr as <owner> shell police_box
```

## 3. Facing and front-door behavior

Unlock the exterior with its matching key.

- Empty-hand click on the back or side: must not enter; it should report which side contains the doors.
- Empty-hand click on the lower front: should enter.
- Empty-hand click on the upper front: should also enter.
- Key use from any side should still lock or unlock exactly once.

The visible doors should rotate with the saved exterior facing.

## 4. Structure integrity

Test both halves separately in a disposable test exterior:

- Break the lower half: the upper half must disappear.
- Break the upper half: the lower half must disappear.
- Only one exterior item should drop in survival.
- `/tr info` should no longer report a placed exterior after the linked structure is broken.

Then restore it with:

```text
/tr exterior
```

Both halves should appear, facing the player, without leaving an orphan block.

## 5. Flight and relocation

1. Select `police_box`.
2. Set a destination and take off.
3. Confirm both exterior halves dematerialize.
4. Land and confirm both halves materialize with the selected shell and facing.
5. Repeat using `/tr land departure` and confirm the original two-block position is reusable.
6. Use `/tr exterior` to relocate it and confirm the old two-block structure is completely removed.

## 6. Change shell while in flight

Take off and run:

```text
/tr shell prototype
```

Expected: the command says the shell will be used on the next landing. Land and confirm the prototype shell appears. Repeat with `police_box`.

## 7. Dedicated-server/non-op check

As a non-op owner, test:

- upper and lower front-door entry
- key locking/unlocking from both halves
- `/tr shell police_box`
- takeoff and landing with the selected shell

Each click must trigger one action only.

## Known limitations

- The police-box shell is a temporary JSON/pixel-art model intended to prove the chameleon architecture. It can later be replaced by a Blockbench model without changing saved TARDIS records.
- Doors teleport the player and play a sound; visual door-leaf animation is not implemented yet.
- Shell selection is command-driven for now. A console/chameleon control or GUI can be added later.
- Existing records intentionally migrate to `prototype`; new records default to `police_box`.
- Materializing on a player does not yet pull that player into the interior. That remains planned for advanced flight.

## 8. Emergency force landing

Obstruct the selected destination at both the lower and upper exterior positions, then run as an operator:

```text
/tr land force
```

Expected: the timer and block obstruction are bypassed, both occupied destination blocks are cleared, and the two-block exterior materializes. This is destructive recovery behavior. The command must still refuse the configured TARDIS interior dimension and impossible world heights.
