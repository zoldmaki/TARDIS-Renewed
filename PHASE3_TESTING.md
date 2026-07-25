# TARDIS Renewed — Phase 3 testing

Phase 3 adds the persistent exterior-link foundation. The visible exterior is
intentionally a temporary one-block prototype; its tile entity and saved link
will remain useful when the proper model and multi-block shell replace it.

## Build and launch

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" clean build --no-daemon
& "C:\Gradle\gradle-4.9\bin\gradle.bat" runClient --no-daemon
```

Also run the dedicated server once after the client checks:

```powershell
& "C:\Gradle\gradle-4.9\bin\gradle.bat" runServer --no-daemon
```

## Key recipe regression test

Craft the key with four iron ingots. This exact arrangement should work, and
its mirrored form should work as well:

```text
. I .
. I .
. I I
```

The Phase 2 JSON had rows of unequal width, so Forge rejected the recipe while
loading it. Phase 3 makes all three rows exactly three characters wide.

## Exterior tests

1. Use `/tr create` and `/tr key`.
2. Stand in an open area with two clear blocks of height in front of you.
3. Run `/tr exterior`.
4. Run `/tr info`; the exterior position should now be listed.
5. Right-click the exterior with an empty hand. It should say it is locked.
6. Right-click it with the matching bound key. It should unlock.
7. Right-click with an empty hand again. It should report that the interior is
   not connected yet.
8. Right-click with the key again. It should lock.
9. Run `/tr exterior` somewhere else. The old prototype should be removed and
   the saved position should change.
10. Save and restart the world. The exterior should remain linked and locked.

## Manual placement test

Give yourself the prototype block:

```text
/give @p renewedtardis:tardis_exterior
```

Place it, then use an unbound or matching TARDIS Key on it. It should bind only
when that TARDIS does not already have another saved exterior.

## Expected limitations

- The prototype occupies one block and is not the final police-box model.
- It does not teleport into an interior yet.
- Lock state currently controls interaction only; later phases will use it to
  allow or deny entry.
- Breaking the linked exterior clears its saved exterior position.
