# Phase 20 changelog

Version: `0.21.0-alpha`

- Added `/tr doctor` record diagnostics for owners.
- Added operator-only `/tr doctor deep` exterior block/tile verification.
- Added `/tr as <owner> doctor ...` support.
- Added operator-only `/tr repair interior` to rebuild and rebind registered interior systems and registered rooms.
- Added `/tr repair exterior [force]` to restore both exterior halves, shell state, lock state, owner metadata, and TARDIS UUID binding at the saved position.
- Added `/tr repair all [force]` convenience recovery.
- Added tab completion for diagnostics and repair commands.
- Added a Mods-menu `Diagnostics and Recovery` config category.
- Added configurable doctor output limits and successful-repair logging.
- Added a diagnostics/recovery page to the in-game manual.
- Added release checklist and known-limitations documents.
- Kept save-data version `12`; no persistent field migration is introduced.
