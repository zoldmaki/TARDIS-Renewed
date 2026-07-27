# TARDIS Renewed Phase 10 Hotfix 3

This hotfix replaces the generic two-block materialization result with verified placement and exact failure diagnostics.

## Changes

- Landing now verifies the actual lower and upper block states after each world update instead of trusting only the boolean returned by `setBlockState`.
- The target chunk is explicitly loaded before materialization begins.
- The lower exterior tile entity is explicitly repaired if Forge does not create it during programmatic placement.
- Nested structure changes now use a depth counter, preventing an inner two-block cleanup from ending an outer protected structure operation too early.
- Operator `force` landing performs destructive clearing inside the same verified placement operation.
- Failure messages identify the exact stage: clearing, lower block, upper block, or tile entity.
- A full diagnostic is shown in chat and written to the server/client log.
- The duplicate sound-category argument in the landing sound call is corrected.

## Recovery test

Use the TARDIS that is currently stuck in flight:

```text
/tr land departure force
```

If it succeeds, also test a normal flight and normal landing.

If it still fails, copy the complete line beginning with:

```text
Debug: dimension=...
```

The same information will be written to:

```text
run/logs/latest.log
```

under a line containing `materialization failed`.

## Build

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
```

This hotfix does not change registry names, save formats, or the `0.11.0-alpha` version number.
