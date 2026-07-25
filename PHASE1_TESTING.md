# Phase 1 test checklist

Extract this overlay into the project root and allow it to overwrite matching files.

## Build

PowerShell:

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

Expected jar:

```text
build/libs/RenewedTardis-0.2.0-alpha.jar
```

## Client

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" runClient --no-daemon
```

Check all of the following:

- Renewed TARDIS loads without a crash.
- A `Renewed TARDIS Mod` creative tab appears.
- The tab contains Chronosteel Ingot and Chronosteel Block.
- Both have proper names and textures.
- The block places, breaks, and drops itself.
- Nine Chronosteel Ingots in a full crafting grid produce one Chronosteel Block.
- `latest.log` no longer reports missing `minecraft:sounds/*.ogg` files for TARDIS sounds.

Useful commands in a creative test world:

```text
/give @p renewedtardis:chronosteel_ingot 64
/give @p renewedtardis:chronosteel_block 1
```

## Dedicated server smoke test

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" runServer --no-daemon
```

The first run may stop until `run/eula.txt` is accepted. The important result is that no client-only class-loading crash occurs.

When reporting a problem, include the complete terminal output and `run/logs/latest.log`.
