# TARDIS Renewed Phase 7 testing

Phase 7 expands destination parsing and adds operator ownership/recovery tools. It does not change the working flight loop.

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.8.0-alpha.jar
```

## 1. Existing-world migration

Use a backup of the Phase 6 world.

1. Start the world and run `/tr info`.
2. Confirm the TARDIS UUID, interior cell, exterior position, destination, lock state, and flight state are unchanged.
3. Complete one normal takeoff and landing to confirm the Phase 6 loop still works.

The persistent file format is unchanged; ownership uses the existing owner UUID and name fields.

## 2. Relative destination coordinates

The command accepts vanilla-style `~` coordinates. A bare `~` copies that coordinate from the command player's current block position. Numeric offsets are also supported.

Examples:

```text
/tr destination ~ ~ ~
/tr destination ~ 70 ~
/tr destination ~10 ~ ~-5
/tr destination 100 80 -40
```

Checks:

1. Stand at a known position and run `/tr destination ~ ~ ~`.
2. Run `/tr info` and confirm all three coordinates match your block position.
3. Run `/tr destination ~5 ~2 ~-3` and confirm the offsets are applied.
4. Confirm unsafe heights are still rejected. For example, `/tr destination ~ 0 ~` parses correctly but is rejected because Y=0 is outside the safe landing range.

## 3. `here` destination mode

```text
/tr destination here
/tr destination here north
```

- `here` uses the player's current block coordinates and current dimension.
- With no explicit facing, the exterior faces back toward the player using the existing default behavior.
- The optional facing is one of `north`, `south`, `east`, or `west`.

Move away before landing so the result is easy to inspect.

## 4. Explicit dimension and facing

Supported forms:

```text
/tr destination <x> <y> <z>
/tr destination <x> <y> <z> <facing>
/tr destination <x> <y> <z> <dimension>
/tr destination <x> <y> <z> <dimension> <facing>
```

Examples:

```text
/tr destination ~ ~ ~ east
/tr destination 100 70 100 -1
/tr destination 100 70 100 -1 west
```

Confirm `/tr info` reports both the chosen dimension and facing, then land and inspect the exterior orientation.

## 5. Ownership transfer

This command requires operator permission. The source owner may be offline, but the new owner must be online so the correct UUID is used.

```text
/tr transfer <currentOwner> <newOwner>
```

Suggested solo dev-environment test:

1. Note the old generated player name from `/tr info`.
2. Restart until the development client receives a new name.
3. Join the server and give the new name operator permission.
4. Run `/tr transfer <oldGeneratedName> <newGeneratedName>`.
5. Run `/tr info` as the new player.
6. Confirm the original TARDIS UUID, interior, exterior, destination, and flight state were retained.
7. Enter the interior and operate the console as the new owner.
8. Run `/tr key` to obtain a key whose tooltip contains the updated owner name.

Transfer is refused when the target already owns a different TARDIS record. This prevents accidental silent record loss.

Existing physical keys remain bound to the TARDIS UUID. Their cached owner tooltip updates after the key is used again, or a fresh key can be issued.

## 6. Operator commands for another TARDIS

Use:

```text
/tr as <owner> info
/tr as <owner> key
/tr as <owner> destination here
/tr as <owner> destination ~ ~ ~ 0 north
/tr as <owner> exterior
/tr as <owner> interior
/tr as <owner> exit
/tr as <owner> takeoff
/tr as <owner> land
```

The `<owner>` argument accepts:

- the record's saved owner name
- an online owner's current name
- a TARDIS UUID
- an owner UUID

Checks:

1. Use `/tr as <owner> info` to inspect an offline player's record.
2. Use `/tr as <owner> interior` to enter it.
3. Set its destination relative to the operator's position.
4. Use `takeoff` and `land` while not being its owner.
5. Use `exterior` to recover or relocate a landed, missing exterior in front of the operator.

`/tr as` and `/tr transfer` must reject non-operators.

## 7. Tab completion

Press Tab in these positions:

```text
/tr <TAB>
/tr transfer <TAB>
/tr transfer OldOwner <TAB>
/tr as <TAB>
/tr as OldOwner <TAB>
/tr destination <TAB>
/tr destination here <TAB>
```

Expected:

- top-level subcommands complete
- saved owner names appear even when those players are offline
- current online names appear for transfer targets
- destination modes, `~`, and horizontal facings complete

## Known limitations

- The new owner must be online during transfer.
- A transfer does not hunt through every inventory or unloaded container to rewrite old key tooltip data.
- `/tr as ... exterior` remains a debug/recovery placement two blocks in front of the operator, based on horizontal facing rather than camera ray tracing.
- Flight still has no duration, energy, instability, or automatic safe-site search.
- Guest access lists are not implemented yet.
