# Known limitations

These are known project limitations, not hidden promises of completed release polish.

- The police-box exterior and most console/interior blocks still use prototype models or textures.
- Gravity lifts currently use a reliable server-authoritative teleport between levels rather than smooth phasing movement.
- Optional HWYLA and ComputerCraft integrations still require dedicated testing with the matching 1.12.2 mods installed.
- Computer integration is read-only.
- Room construction and secondary-corridor management are command-driven; no visual architect interface exists yet.
- Only horizontally adjacent existing rooms can receive secondary corridors.
- Interior schemas are intentionally simple and do not yet provide the full legacy room catalogue.
- The shared interior dimension ID must not be changed after interiors have been generated.
- Legacy 1.7.10 world import is outside scope.
- `/tr repair ... force` and forced landing are destructive operator recovery tools.
- Deep diagnostics may load the saved exterior dimension and chunk. Normal `/tr doctor` does not perform that world check.
- If the saved exterior lower block is completely missing, repair can restore the saved position and shell but must use north as the facing because landed facing is not stored separately from the block state.
