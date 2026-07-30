# Phase 13 testing — advanced flight and materialization handling

Phase 13 updates the save-data version to `8` and the mod version to `0.14.0-alpha`.
Existing Phase 12 worlds should migrate automatically. Existing TARDISes default to
`normal` speed and `precise` accuracy so their previous exact-flight behavior is preserved.

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.14.0-alpha.jar
```

Run both the client and dedicated-server tests as usual.

## 1. Existing-world migration

Load the current Phase 12 test world and run:

```text
/tr info
/tr flight
```

Confirm that ownership, exterior, shell, rooms, energy, lock state, and destination
are unchanged. The new profile should initially report:

```text
speed=normal, accuracy=precise
```

Restart the world and confirm the profile persists.

## 2. Flight speed settings

The owner may use:

```text
/tr flight speed slow
/tr flight speed normal
/tr flight speed fast
```

The settings use these prototype travel-time multipliers:

- `slow`: 150% of normal duration
- `normal`: 100%
- `fast`: 65%

Set the same route and compare the estimated takeoff time for each setting. Flight
energy cost is intentionally unchanged in this phase. Speed and accuracy cannot be
changed while already in flight.

Operator control works through:

```text
/tr as <owner> flight speed fast
```

## 3. Accuracy and resolved landing target

Accuracy settings:

```text
/tr flight accuracy precise
/tr flight accuracy normal
/tr flight accuracy risky
```

- `precise`: no intentional drift
- `normal`: up to 2 blocks of horizontal drift
- `risky`: up to 6 blocks of horizontal drift

For `normal` and `risky`, choose a large clear area, set a destination, and take off.
Then run:

```text
/tr flight target
/tr flight
```

The resolved landing target should be saved separately from the selected destination.
Restart while in flight and confirm the same resolved target is retained. Landing must
use that saved target rather than rolling a new random offset.

If no other clear candidate exists inside the accuracy radius, the intended destination
is kept instead of making the flight impossible.

## 4. Player materialization capture

This can be tested with one player because the command does not require the pilot to
stand inside the console room:

1. Make sure the exterior is landed somewhere away from you.
2. Set accuracy to precise:

```text
/tr flight accuracy precise
```

3. Stand in a clear two-block-high location and use:

```text
/tr destination here
/tr takeoff
```

4. Stay at that destination, wait until the flight is ready, then use:

```text
/tr land
```

The exterior should materialize at your position and immediately move you to the linked
TARDIS console room. You should receive a message explaining that the materializing
TARDIS pulled you inside. The landing message should report one captured player.

Also test standing partly across the edge of the landing block. The slightly expanded
collision volume should still detect the player.

On a real multiplayer test, every non-spectator player intersecting the two-block exterior
volume should be moved into the same linked interior.

## 5. Destination restrictions

After launching once, inspect the `flight` section in:

```text
run/config/renewedtardis.cfg
```

New settings:

```text
B:allowCrossDimensionFlight=true
I:blockedDestinationDimensions <
>
```

### Blocked-dimension test

Add a registered test dimension ID to the list, restart, and verify that both
`/tr destination` and sneak-right-clicking with a bound key reject that dimension.
Normal takeoff must also refuse a previously saved destination that is now blocked.

### Cross-dimension test

Set:

```text
B:allowCrossDimensionFlight=false
```

Restart and confirm that same-dimension routes still work while cross-dimensional
routes are refused. Restore the setting afterward if cross-dimensional travel is wanted.

Operator `force` landing deliberately bypasses these normal route restrictions for
recovery, but it still cannot materialize inside the configured TARDIS interior dimension.

## 6. Flight-event foundation

Phase 13 posts Forge events for cancellable takeoff, cancellable landing, and completed
landing. There is no visible gameplay test without a small addon or test listener, but
normal flight must continue to work when no listener is installed.

## Regression checklist

- Exact, nearest-safe, departure, and forced landings still work.
- Forced landing still removes obstructing blocks.
- The two exterior halves materialize and dematerialize together.
- Lock state, shell, owner, and exterior tile binding persist.
- Energy is charged exactly once at takeoff.
- Mid-flight restart keeps timing and the resolved target.
- Interior door refuses exit while flying and works after landing.
- Non-operator owners can use `/tr flight`, speed, and accuracy settings.
- Dedicated-server interaction still performs only one action per click.
