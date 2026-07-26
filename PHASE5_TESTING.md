# TARDIS Renewed Phase 5 testing

Phase 5 hardens the existing travel foundation before flight controls are added.

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.6.0-alpha.jar
```

## 1. Existing-world compatibility

Use a backup of the Phase 4 test world and leave the dimension setting at its default value.

1. Start the world.
2. Run `/tr info`.
3. Confirm the TARDIS UUID, exterior position, interior cell, and generated state remain unchanged.
4. Enter and exit normally.

## 2. Config generation

After one launch, inspect:

```text
run/config/renewedtardis.cfg
```

A dedicated server creates the same file in its normal `config` directory.

Confirm that the file contains:

```text
I:tardisInteriorDimensionId=1212
```

The warning above the value must explain that changing it after interior generation is highly unrecommended.

Do not change the value on the main test world. To test a different ID, use a disposable new world and a free dimension ID.

## 3. Exterior placement guard

From inside the shared TARDIS dimension, run:

```text
/tr exterior
```

Expected result:

- The command refuses placement.
- The existing exterior remains in its original world and position.
- `/tr info` still reports the original exterior.

## 4. Dedicated-server non-operator interaction

This is the most important Phase 5 test.

1. Place and bind an exterior.
2. Put it near the server spawn so the normal spawn-protection area is exercised.
3. Remove operator status from the test player.
4. With the matching key, lock and unlock the exterior.
5. Enter without holding the key while it is unlocked.
6. Use the interior door to exit.
7. Repeat after a server restart.

Expected result: all four interactions work for a non-operator. The custom packet accepts only nearby TARDIS exterior and interior-door blocks; it does not grant general protected-block access.

## 5. Key and visitor behavior

- A wrong key must still be rejected.
- A locked exterior must still reject empty-hand entry.
- An unlocked exterior must allow a visitor to enter.
- The visitor must be able to leave through the linked interior door.

## 6. Lighting

Generate a brand-new TARDIS interior and watch the room on first arrival.

Expected result: the ceiling and corners should receive block light immediately. Report any remaining dark patches and include a screenshot plus `latest.log`.

## Known limitations

- `/tr exterior` still places two blocks ahead based on horizontal facing, not the exact camera crosshair.
- The exterior and interior room remain visual prototypes.
- The dimension setting is startup-only and has no in-game GUI.
- Changing the dimension ID does not migrate existing interiors.
