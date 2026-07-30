# Release stabilization checklist

## Required before a public beta

- Complete clean client and dedicated-server builds with Java 8 and Gradle 4.9.
- Run the full Phase 20 regression test on a copy of an older project world.
- Test two real simultaneous players for crew, keys, entry, console permissions, materialization capture, transmat, and gravity lifts.
- Test without optional integrations installed.
- Test with the intended 1.12.2 HWYLA/Waila build installed.
- Test with the intended 1.12.2 ComputerCraft or CC:Tweaked build installed.
- Run a multi-hour dedicated-server soak test including repeated flights, room generation, restarts, and dimension unload/reload.
- Review recipes and energy/damage defaults in a survival playthrough.
- Replace or explicitly label remaining prototype assets.
- Verify license, permission, original-author credit, and third-party asset/API notices.
- Back up and test recovery commands on deliberately damaged copies of a world.

## Save compatibility policy

Save-data version remains `12` in Phase 20. No field is removed or renamed. New persistent changes after this point should require an explicit migration review and a documented data-version increase.

## Packaging checks

- The output jar must not contain Waila/HWYLA API classes.
- The output jar must not contain ComputerCraft API classes.
- Darkcore must not be required or bundled.
- Generated, IDE, run, and local configuration files must not be included in release source archives.
- `mcmod.info`, version number, changelog, known limitations, and credits must agree.
