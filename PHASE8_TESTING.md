# TARDIS Renewed Phase 8 testing

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.9.0-alpha.jar
```

## 1. Existing world

Load a backup of the Phase 7 world and run:

```text
/tr info
```

Confirm the TARDIS UUID, owner, interior, exterior, lock state, and destination remain intact.

## 2. Timed flight

Set a destination and take off:

```text
/tr destination here north
/tr takeoff
/tr flight
```

Move the destination first so it does not overlap the player.

Expected:

- The exterior dematerializes.
- `/tr flight` reports a percentage and remaining time.
- Immediate `/tr land` refuses because travel is not complete.
- After the timer reaches 100%, `/tr land` materializes the exterior.

Default same-dimension minimum time is 8 seconds. Longer same-dimension distances add time, capped at 60 seconds. Cross-dimension travel adds 10 seconds.

## 3. Console behavior

- Normal click while landed starts flight.
- Normal click during the timer reports remaining travel rather than landing.
- Sneak-click reports status without changing state.
- Normal click after the timer is complete lands at the exact destination.

## 4. Restart-safe timing

1. Take off.
2. Exit the world or stop the dedicated server before the timer completes.
3. Wait longer than the reported remaining time.
4. Restart and run `/tr flight`.

Expected: the TARDIS is still in flight but reports `READY TO LAND (100%)`. The exterior must not spontaneously duplicate or reappear.

## 5. Obstructed exact landing

1. Take off toward a valid destination.
2. Before landing, place blocks in the destination and the block above it.
3. Wait until ready and run `/tr land`.

Expected: exact landing refuses and the TARDIS remains in flight.

Then run:

```text
/tr land nearest
```

Expected: it searches within the configured radius and lands on the nearest two-block clear position with a full solid block underneath.

## 6. Departure recovery

During another flight, run:

```text
/tr land departure
```

After the timer completes, the TARDIS should return to the original takeoff area and use its saved departure facing. If the exact departure block is occupied, it searches nearby rather than overwriting the obstruction.

## 7. Operator recovery

Operator-only examples:

```text
/tr land force
/tr land nearest force
/tr as <owner> flight
/tr as <owner> land departure force
```

Phase 10 Hotfix 2 supersedes this original rule: `force` now bypasses the timer and clears the two exterior destination blocks as an operator-only destructive recovery action. Normal landing modes still refuse occupied blocks.

## 8. Configuration

After launching, `run/config/renewedtardis.cfg` gains a `[flight]` section:

- `minimumFlightSeconds`
- `maximumFlightSeconds`
- `blocksPerExtraSecond`
- `crossDimensionPenaltySeconds`
- `safeLandingSearchRadius`

For quick testing, the minimum can temporarily be reduced. Restart the client/server after editing.

## Known limitations

- Flights do not auto-land; a player or operator activates landing when ready.
- Flight time currently does not consume energy or account for upgrades.
- The safe search is intentionally limited and will not clear terrain.
- The prototype console still combines several future controls into one block.
