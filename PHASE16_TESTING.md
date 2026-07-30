# Phase 16 testing — crew permissions and exterior access

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.17.0-alpha.jar
```

## Migration check

Load an existing Phase 15 world and run:

```text
/tr info
/tr access
/tr crew list
```

Expected:

- Existing ownership, exterior, shell, destination, rooms, flight, energy, damage, and upgrades remain unchanged.
- Existing TARDISes migrate to `public` exterior access so an unlocked exterior behaves as it did before Phase 16.
- The crew list initially contains no members.

## Access modes

```text
/tr access mode owner
/tr access mode crew
/tr access mode public
```

- `owner`: only the owner or an operator may enter an unlocked exterior.
- `crew`: the owner/operator and crew with `door` permission may enter.
- `public`: any player may enter while the exterior is unlocked.
- The exterior lock remains a separate restriction in every mode.
- The interior exit door always permits leaving so guests cannot be trapped.

## Crew roles

A target player must be online when first added so the real UUID is stored.

```text
/tr crew add <player> guest
/tr crew add <player> keyholder
/tr crew add <player> pilot
/tr crew add <player> engineer
/tr crew add <player> full
/tr crew list
```

Role presets:

- `guest`: door
- `keyholder`: door, key
- `pilot`: door, key, console
- `engineer`: door, maintenance
- `full`: door, key, console, maintenance

Changing a role replaces that member's permission set:

```text
/tr crew role <player> pilot
```

Individual changes:

```text
/tr crew grant <player> console
/tr crew revoke <player> key
```

Valid permissions are `door`, `key`, `console`, and `maintenance`.

## Bound crew keys

A crew member must be online and already have `key` permission:

```text
/tr crew key <player>
```

The issued key should:

- Lock and unlock the matching exterior.
- Set a destination by sneak-right-clicking a block.
- Stop working immediately if `key` permission is revoked, even though the item remains bound.
- Never bind a new unbound exterior unless used by the owner or an operator.

## Console permission

A member with `console` permission should be able to use takeoff, landing, destination, and facing controls.

A member without `console` permission should be denied control actions. Read-only status controls and sneak-status on the legacy console remain available.

## Maintenance permission

A member with `maintenance` permission should be able to:

- Repair subsystems with Chronosteel at the engine core.
- Install upgrade components at the engine core.

A member without that permission should still be able to read engine-core status but not consume repair or upgrade items.

## Solo/random-name dedicated-server test

This is useful when each restart gives you a different development username.

1. Join with the new username and give that player operator status temporarily.
2. Find the old saved owner with `/tr count`, tab completion, or the known old name.
3. Run:

```text
/tr as <oldOwner> crew add <currentPlayer> pilot
/tr as <oldOwner> access mode crew
/tr as <oldOwner> crew key <currentPlayer>
```

4. Remove operator status from the current player.
5. Test the old TARDIS's door, key, and console as a normal crew member.
6. Revoke one permission through the console/operator account and confirm the physical action is denied immediately.

Operator recovery syntax also supports:

```text
/tr as <owner> crew list
/tr as <owner> crew grant <player> maintenance
/tr as <owner> crew remove <player>
/tr as <owner> access public
```

## Persistence and transfer

Restart the client/server after adding crew and changing access mode. UUIDs, last-known names, permissions, and access mode must remain unchanged.

Ownership transfer preserves the existing crew list, but the new owner is removed from that list because owners already have every permission. The old owner is not automatically retained as crew.

## Regression checks

- Owner key locking and destination selection still work.
- Public unlocked entry still works for migrated worlds.
- Locked exteriors reject entry.
- Takeoff, landing, shell changes, force landing, room generation, Artron costs, damage, and upgrades remain functional.
- Dedicated-server interactions trigger only once per click.
