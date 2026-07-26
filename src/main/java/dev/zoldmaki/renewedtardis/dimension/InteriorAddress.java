package dev.zoldmaki.renewedtardis.dimension;

import net.minecraft.util.math.BlockPos;

/**
 * Converts a persistent sequential interior index into a widely separated cell
 * in the shared dimension. The square spiral keeps early interiors near the
 * origin while reserving 2048 blocks of space in every direction per TARDIS.
 */
public final class InteriorAddress {
    public static final int CELL_SPACING = 2048;
    public static final int FLOOR_Y = 64;

    private InteriorAddress() {
    }

    public static BlockPos getCenterForIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Interior index cannot be negative");
        }
        if (index == 0) {
            return new BlockPos(0, FLOOR_Y, 0);
        }

        int ring = (int) Math.ceil((Math.sqrt(index + 1.0D) - 1.0D) / 2.0D);
        int legLength = ring * 2;
        int maxIndex = (ring * 2 + 1) * (ring * 2 + 1) - 1;
        int offset = maxIndex - index;

        int gridX;
        int gridZ;
        if (offset < legLength) {
            gridX = ring - offset;
            gridZ = -ring;
        } else if (offset < legLength * 2) {
            gridX = -ring;
            gridZ = -ring + (offset - legLength);
        } else if (offset < legLength * 3) {
            gridX = -ring + (offset - legLength * 2);
            gridZ = ring;
        } else {
            gridX = ring;
            gridZ = ring - (offset - legLength * 3);
        }

        long x = (long) gridX * CELL_SPACING;
        long z = (long) gridZ * CELL_SPACING;
        if (x < Integer.MIN_VALUE || x > Integer.MAX_VALUE || z < Integer.MIN_VALUE || z > Integer.MAX_VALUE) {
            throw new IllegalStateException("TARDIS interior address exceeded Minecraft coordinate limits");
        }
        return new BlockPos((int) x, FLOOR_Y, (int) z);
    }

    public static BlockPos getEntryPosition(BlockPos center) {
        return center.add(0, 1, 3);
    }

    public static BlockPos getDoorPosition(BlockPos center) {
        return center.add(0, 1, 5);
    }

    public static BlockPos getFlightConsolePosition(BlockPos center) {
        return center.add(0, 1, 0);
    }
}
