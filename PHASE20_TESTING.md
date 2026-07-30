# Phase 20 testing — diagnostics, recovery, and stabilization

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.21.0-alpha.jar
```

## Regression smoke test

Use an existing Phase 19 world and verify:

1. The TARDIS owner, UUID, shell, lock, exterior, interior, rooms, crew, energy, damage, upgrades, destination, and flight state are unchanged.
2. Enter and exit the exterior.
3. Take off, wait, and land.
4. Use the transmat and one gravity-lift pair.
5. Restart the world/server and repeat entry and `/tr info`.

Phase 20 does not change the persistent record format; save-data version remains `12`.

## Doctor command

As the owner:

```text
/tr doctor
```

This is a record-only inspection and must not intentionally load the saved exterior dimension or chunk. A healthy record should report zero errors and zero warnings, apart from informational lines.

As an operator:

```text
/tr doctor deep
/tr as <owner> doctor
/tr as <owner> doctor deep
```

Deep mode should verify both exterior halves, shell state, tile entity, TARDIS UUID, owner metadata, and lock state. It may load the saved exterior dimension and chunk.

## Safe repair tests

Back up the test world first.

### Upper-half repair

1. Place and land the TARDIS normally.
2. Remove only the exterior upper block with a world-editing/debug method that does not clear the persistent record.
3. Run `/tr doctor deep`; it should report the missing upper block.
4. Run:

```text
/tr repair exterior
```

5. Both halves should be present again, the tile should remain bound, and lock/owner/shell data should be unchanged.

### Interior core repair

1. Break one disposable prototype console control or the engine core inside the test interior.
2. Run:

```text
/tr repair interior
```

3. The core-room components should be recreated and rebound to the same TARDIS.
4. Existing expandable room records must remain intact.

### Obstructed exterior repair

1. Remove the exterior without clearing its saved position, then put solid blocks in both saved exterior spaces.
2. `/tr repair exterior` must refuse without deleting the obstruction.
3. As an operator, run:

```text
/tr repair exterior force
```

4. The two obstructing blocks should be deleted and the TARDIS should materialize at the saved position.

`force` is deliberately destructive and should only be used after checking the saved coordinates with `/tr info` or `/tr doctor deep`.

## Config GUI

Open:

```text
Mods -> TARDIS Renewed -> Config -> Diagnostics and Recovery
```

Verify:

- `Maximum Doctor Messages` changes how many findings are printed.
- `Log Successful Repairs` controls successful repair entries in `latest.log`/the dedicated-server log.

## Dedicated server

1. Run `/tr doctor` as a non-operator owner.
2. Confirm `/tr doctor deep` and every `/tr repair` form are denied while de-opped.
3. Re-op and test `/tr as <offlineSavedOwner> doctor deep`.
4. Repair an exterior near spawn protection and confirm the operation is server-authoritative.
5. Restart and verify repaired structures and bindings persist.

## Failure reports

For a failed repair, include:

- the full chat message and `Details:` line,
- the matching section of `logs/latest.log`,
- `/tr info`,
- `/tr doctor deep`,
- the dimension and coordinates involved.
