# Phase 10 Hotfix 1

Fixes the two `compileJava` errors in the exterior block classes by replacing the rejected `getBlockLayer()` overrides with Forge 1.12.2 `canRenderInLayer(...)` overrides.

No save, registry, resource, or version changes are included.
