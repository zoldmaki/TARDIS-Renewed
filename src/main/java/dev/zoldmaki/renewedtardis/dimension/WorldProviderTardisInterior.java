package dev.zoldmaki.renewedtardis.dimension;

import net.minecraft.entity.Entity;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.gen.IChunkGenerator;

/**
 * Empty, dark world used as a container for individually generated TARDIS rooms.
 */
public final class WorldProviderTardisInterior extends WorldProvider {
    @Override
    protected void init() {
        hasSkyLight = false;
        doesWaterVaporize = false;
        biomeProvider = new BiomeProviderSingle(Biomes.VOID);
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new ChunkGeneratorTardisInterior(world);
    }

    @Override
    public DimensionType getDimensionType() {
        return TardisDimensionManager.getInteriorType();
    }

    @Override
    public boolean isSurfaceWorld() {
        return false;
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }

    @Override
    public boolean canCoordinateBeSpawn(int x, int z) {
        return false;
    }

    @Override
    public float calculateCelestialAngle(long worldTime, float partialTicks) {
        return 0.0F;
    }

    @Override
    public Vec3d getFogColor(float celestialAngle, float partialTicks) {
        return new Vec3d(0.0D, 0.0D, 0.0D);
    }

    @Override
    public Vec3d getSkyColor(Entity cameraEntity, float partialTicks) {
        return new Vec3d(0.0D, 0.0D, 0.0D);
    }

    @Override
    public float getCloudHeight() {
        return -1000.0F;
    }

    @Override
    public int getAverageGroundLevel() {
        return InteriorAddress.FLOOR_Y;
    }

    @Override
    public boolean doesXZShowFog(int x, int z) {
        return false;
    }
}
