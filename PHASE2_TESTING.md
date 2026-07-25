# Phase 2 test checklist

Extract this overlay into the project root and overwrite matching files.

## Build

PowerShell:

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/TARDIS-Renewed-0.3.0-alpha.jar
```

## Client checks

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" runClient --no-daemon
```

Check that:

- The Mods screen and creative tab say **TARDIS Renewed**.
- The creative tab contains the TARDIS Key.
- The key has its legacy texture and the name `TARDIS Key`.
- An untouched creative key says `Unbound` in its tooltip.
- Right-clicking an unbound key binds it and shows a warm-key message.
- The tooltip then shows the owner and a TARDIS UUID.
- Right-clicking the bound key reports that its interior dimension is not assigned yet.
- Four iron ingots in the legacy key shape craft an unbound key.

Legacy key recipe shape:

```text
_ I
_ I
_ I I
```

Useful commands:

```text
/give @p renewedtardis:tardis_key
/tr info
/tr create
/tr key
/tr count
```

`/tr key` should create a record when necessary and give a key already bound to it.

## Persistence test

1. Run `/tr info` and copy the TARDIS UUID.
2. Save and quit to the title screen.
3. Reopen the same world.
4. Run `/tr info` again.
5. Confirm the UUID is unchanged.

The world should contain a native save file similar to:

```text
run/saves/<world>/data/renewedtardis_tardis_registry.dat
```

## Dedicated server smoke test

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" runServer --no-daemon
```

Confirm that:

- The server reaches the normal startup state without a client-class loading crash.
- `/tr count` works from the server console.
- A player can use `/tr create`, `/tr info`, and `/tr key`.
- The same TARDIS UUID remains after restarting the server.

When reporting a problem, include the complete terminal output and `run/logs/latest.log`.
