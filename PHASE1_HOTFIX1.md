# Phase 1 Hotfix 1

Fixes compile errors caused by using older MCP names with the project's `stable_39` mappings.

Changes:
- `setUnlocalizedName(...)` -> `setTranslationKey(...)`
- `CreativeTabs#getTabIconItem()` -> `CreativeTabs#createIcon()`

Apply this overlay after the Phase 1 overlay, replacing matching files.
