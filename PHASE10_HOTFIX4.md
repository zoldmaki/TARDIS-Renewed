# Phase 10 Hotfix 4

Fixes the Phase 10 Hotfix 3 compilation failure under Forge 1.12.2 stable_39.

`World#getChunkFromBlockCoords(BlockPos)` is not exposed under this mapping set. The exterior placement diagnostic now loads the destination chunk through:

```java
world.getChunkProvider().provideChunk(pos.getX() >> 4, pos.getZ() >> 4);
```

The upper and lower exterior blocks use the same X/Z chunk, so one load is sufficient.

This hotfix changes no registry names, saved data, resources, or gameplay behavior.
