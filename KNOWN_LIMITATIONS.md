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
- The Phase 21 console uses four low placeholder blocks; Navigation now has sketch-inspired hit regions and a 5×5 waypoint grid, while the final sloped model, exact control placement, animations, and sounds are not implemented.
- Phase 21G implements coordinate navigation, waypoints, Main Flight controls, coordinated/uncoordinated pause/resume, same-dimension travelled-position interpolation, and early handbrake landing. Scanner pages, shell selection, room actions, physical landing pads, shields, TARDIS XP progression, and quick-time events remain deferred.
- Minecraft dimensions do not provide a continuous shared coordinate space. An interrupted cross-dimensional route therefore reconstructs at that segment's starting dimension/position until the transition completes; same-dimension routes interpolate continuously.
- Waypoint dye colors currently affect the built-in crosshair label only; final button textures and model tinting await the final console model.

