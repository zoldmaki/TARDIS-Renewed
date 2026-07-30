# Phase 12 testing — Artron energy and engine core

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar: `build/libs/TARDIS-Renewed-0.13.0-alpha.jar`

## Existing-world migration

1. Open a Phase 11 world and run `/tr info`.
2. Ownership, exterior, shell, rooms, destination, and flight state must remain unchanged.
3. The record should receive the configured default Artron capacity and a full charge because it predates the energy system.
4. Enter the interior. A glowing prototype engine-core block should appear near the rear wall without rebuilding the room or existing expansions.
5. Restart and confirm the same energy value and capacity persist.

## Engine core and status

- Right-click the engine core: current energy, capacity, percentage, and regeneration state should be shown.
- Sneak-right-click it: the current route cost and all three room costs should also be shown.
- `/tr energy`, `/tr info`, `/tr flight`, the status control, and the legacy flight console should report energy.

## Regeneration

With the default config:

```text
/tr energy set 100
/tr energy
```

Wait about five seconds and run `/tr energy` again. It should have gained roughly 50 AU at the default 10 AU/s rate. Sub-second time is retained between checks.

Set a low value, stop the server for several seconds, restart, and check again. Regeneration uses real elapsed time and should continue across restarts, capped at capacity.

## Flight costs

1. Place the exterior and set a destination.
2. Run `/tr energy` to see the calculated route cost.
3. As an operator, set energy below that cost:

```text
/tr energy set 0
```

4. Attempt takeoff. It must be refused, and the exterior must remain present.
5. Refill and take off:

```text
/tr energy fill
/tr takeoff
```

6. The takeoff message should state the charged amount and remaining energy.
7. Land normally. Landing should not charge a second time.
8. Restart during flight and confirm both the flight and already-paid energy state persist.

## Room costs

1. Run `/tr room schemas`; each schema should display its configured AU cost.
2. Set energy below a schema cost and attempt to add it. The room must not be created.
3. Fill energy and add the room. The exact cost should be deducted.
4. Restart and confirm both the room and energy persist.
5. Removing a room intentionally does not refund energy, preventing free regeneration exploits.

## Operator recovery

```text
/tr energy fill
/tr energy set 250
/tr energy add 100
/tr energy capacity 20000
/tr as <owner> energy
/tr as <owner> energy fill
/tr as <owner> energy set 0
```

Capacity changes clamp current energy when the new capacity is lower.

## Non-operator command-root regression

On a dedicated server, remove operator status and verify:

```text
/tr info
/tr room list
/tr room schemas
/tr energy
```

These read/owner commands should now be available. Administrative mutations such as `/tr energy fill`, `/tr transfer`, `/tr as`, forced landing, and forced room removal must still be denied.

## Config

The generated `renewedtardis.cfg` now contains an `artron_energy` category for capacity, starting charge, regeneration, flight costs, and room costs. Existing records keep their saved capacity; changing the default capacity mainly affects new records and saves that predate Phase 12.
