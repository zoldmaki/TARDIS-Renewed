package dev.zoldmaki.renewedtardis.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * Small internal replacement for the Darkcore coordinate stores used by the
 * original mod. It keeps a block position together with its dimension ID.
 */
public final class DimensionPosition {
    private final int dimension;
    private final BlockPos position;

    public DimensionPosition(int dimension, BlockPos position) {
        this.dimension = dimension;
        this.position = position.toImmutable();
    }

    public int getDimension() {
        return dimension;
    }

    public BlockPos getPosition() {
        return position;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Dimension", dimension);
        tag.setInteger("X", position.getX());
        tag.setInteger("Y", position.getY());
        tag.setInteger("Z", position.getZ());
        return tag;
    }

    public static DimensionPosition readFromNBT(NBTTagCompound tag) {
        return new DimensionPosition(
            tag.getInteger("Dimension"),
            new BlockPos(tag.getInteger("X"), tag.getInteger("Y"), tag.getInteger("Z"))
        );
    }

    @Override
    public String toString() {
        return "dim " + dimension + " @ " + position.getX() + ", " + position.getY() + ", " + position.getZ();
    }
}
