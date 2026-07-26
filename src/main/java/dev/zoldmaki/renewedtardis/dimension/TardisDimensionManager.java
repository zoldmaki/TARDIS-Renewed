package dev.zoldmaki.renewedtardis.dimension;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

/**
 * Registers and loads the single shared dimension containing separated TARDIS
 * interior cells.
 */
public final class TardisDimensionManager {
    private static DimensionType interiorType;
    private static int dimensionId = Integer.MIN_VALUE;

    private TardisDimensionManager() {
    }

    public static void register(int configuredDimensionId) {
        if (interiorType != null) {
            if (dimensionId != configuredDimensionId) {
                throw new IllegalStateException(
                    "TARDIS interior dimension was already registered as " + dimensionId
                );
            }
            return;
        }

        if (configuredDimensionId == -1 || configuredDimensionId == 0 || configuredDimensionId == 1) {
            throw new IllegalArgumentException(
                "TARDIS interior dimension cannot replace vanilla dimension " + configuredDimensionId
            );
        }

        if (DimensionManager.isDimensionRegistered(configuredDimensionId)) {
            DimensionType existing = DimensionManager.getProviderType(configuredDimensionId);
            throw new IllegalStateException(
                "Dimension ID " + configuredDimensionId + " is already used by " + existing.getName()
                    + ". Change tardisInteriorDimensionId in renewedtardis.cfg before loading the world."
            );
        }

        try {
            interiorType = DimensionType.register(
                "TARDIS Interiors",
                "_tardis_interiors",
                configuredDimensionId,
                WorldProviderTardisInterior.class,
                false
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                "Dimension type ID " + configuredDimensionId
                    + " is already occupied. Change tardisInteriorDimensionId in renewedtardis.cfg.",
                exception
            );
        }

        dimensionId = configuredDimensionId;
        DimensionManager.registerDimension(dimensionId, interiorType);
    }

    public static int getDimensionId() {
        if (dimensionId == Integer.MIN_VALUE) {
            throw new IllegalStateException("TARDIS interior dimension has not been registered");
        }
        return dimensionId;
    }

    public static DimensionType getInteriorType() {
        if (interiorType == null) {
            throw new IllegalStateException("TARDIS interior dimension type has not been registered");
        }
        return interiorType;
    }

    public static WorldServer getOrLoadWorld(MinecraftServer server) {
        int id = getDimensionId();
        WorldServer world = DimensionManager.getWorld(id);
        if (world == null) {
            DimensionManager.initDimension(id);
            world = DimensionManager.getWorld(id);
        }
        if (world == null) {
            throw new IllegalStateException("Forge could not load the TARDIS interior dimension " + id);
        }
        return world;
    }
}
