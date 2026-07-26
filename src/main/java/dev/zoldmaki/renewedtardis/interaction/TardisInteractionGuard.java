package dev.zoldmaki.renewedtardis.interaction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Suppresses the second half of a client custom-packet plus vanilla activation
 * pair. This keeps protected-spawn support without toggling a TARDIS control
 * twice from one physical click.
 */
public final class TardisInteractionGuard {
    private static final long DUPLICATE_WINDOW_TICKS = 2L;
    private static final Map<UUID, Stamp> LAST_INTERACTIONS = new HashMap<>();

    private TardisInteractionGuard() {
    }

    public static boolean tryBegin(EntityPlayer player, World world, BlockPos pos) {
        UUID playerId = player.getUniqueID();
        long tick = world.getTotalWorldTime();
        int dimension = world.provider.getDimension();
        Stamp previous = LAST_INTERACTIONS.get(playerId);

        if (
            previous != null
                && previous.dimension == dimension
                && previous.pos.equals(pos)
                && tick >= previous.tick
                && tick - previous.tick <= DUPLICATE_WINDOW_TICKS
        ) {
            return false;
        }

        LAST_INTERACTIONS.put(playerId, new Stamp(dimension, pos.toImmutable(), tick));
        if (LAST_INTERACTIONS.size() > 1024) {
            LAST_INTERACTIONS.clear();
            LAST_INTERACTIONS.put(playerId, new Stamp(dimension, pos.toImmutable(), tick));
        }
        return true;
    }

    private static final class Stamp {
        private final int dimension;
        private final BlockPos pos;
        private final long tick;

        private Stamp(int dimension, BlockPos pos, long tick) {
            this.dimension = dimension;
            this.pos = pos;
            this.tick = tick;
        }
    }
}
