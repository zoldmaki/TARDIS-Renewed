package dev.zoldmaki.renewedtardis.dimension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;

/**
 * Produces empty chunks. Interior rooms are placed explicitly and only when a
 * TARDIS is first entered.
 */
public final class ChunkGeneratorTardisInterior implements IChunkGenerator {
    private final World world;

    public ChunkGeneratorTardisInterior(World world) {
        this.world = world;
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        Chunk chunk = new Chunk(world, new ChunkPrimer(), x, z);
        Arrays.fill(chunk.getBiomeArray(), (byte) Biome.getIdForBiome(Biomes.VOID));
        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public void populate(int x, int z) {
    }

    @Override
    public boolean generateStructures(Chunk chunkIn, int x, int z) {
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public BlockPos getNearestStructurePos(
        World worldIn,
        String structureName,
        BlockPos position,
        boolean findUnexplored
    ) {
        return null;
    }

    @Override
    public void recreateStructures(Chunk chunkIn, int x, int z) {
    }

    @Override
    public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
        return false;
    }
}
