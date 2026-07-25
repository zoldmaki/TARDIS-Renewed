# Renewed TARDIS Mod — Phase 0 overlay

This overlay is meant to be extracted over the root of a fresh Minecraft Forge
1.12.2 MDK project, or over another known-working 1.12.2 ForgeGradle 2.3 project.
It intentionally does not include Gradle wrapper binaries.

## What this first overlay does

- Targets Forge `1.12.2-14.23.5.2847`.
- Uses ForgeGradle `2.3-SNAPSHOT` and MCP mappings `stable_39`.
- Pins Gradle to the supplied Temurin Java 8 installation.
- Adds a minimal mod entry point that should load without Darkcore.
- Packages the original runtime assets under the original `tardismod` namespace.
- Uses the supplied Renewed TARDIS logo in the Mods screen.
- Adds planning and inventory notes for the real port.

## Setup

1. Download/extract the Forge 1.12.2-14.23.5.2847 MDK into an empty project folder.
2. Extract this overlay into that folder and allow replacement of matching files.
3. Open a command prompt in the project root.
4. Verify the wrapper uses Java 8:
   `gradlew -version`
5. Prepare the workspace:
   `gradlew setupDecompWorkspace`
6. Generate IDE files if needed:
   - Eclipse: `gradlew eclipse`
   - IntelliJ IDEA: `gradlew idea`
7. Test the development client:
   `gradlew runClient`
8. Build the jar:
   `gradlew clean build`

The built jar should appear in `build/libs`.

## Expected Phase-0 result

Minecraft reaches the title screen, Forge lists **Renewed TARDIS Mod**, and the
log contains `Renewed TARDIS Mod phase-0 scaffold loaded.` No blocks, items,
dimensions, GUIs, or TARDIS mechanics are expected yet.

## Important

Do not place the old 1.7.10 TARDIS jar or Darkcore jar in the 1.12.2 development
`mods` folder. They cannot run on 1.12.2 and can make the first test misleading.
