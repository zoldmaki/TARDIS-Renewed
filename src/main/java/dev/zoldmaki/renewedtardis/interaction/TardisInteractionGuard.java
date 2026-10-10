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
    private static final int MAX_TRACKED_INTERACTIONS = 4096;
    private static final Map<InteractionKey, Long> LAST_INTERACTIONS = new HashMap<>();

    private TardisInteractionGuard() {
    }

    public static boolean tryBegin(EntityPlayer player, World world, BlockPos pos) {
        return tryBegin(player, world, pos, 0);
    }

    /**
     * Variant for multi-control blocks. The discriminator keeps the fallback
     * packet and vanilla activation for one control deduplicated without
     * suppressing a different control on the same block in the same tick.
     * Separate keys also keep an interleaved click on another control from
     * hiding the still-pending duplicate delivery of the first control.
     */
    public static boolean tryBegin(
        EntityPlayer player,
        World world,
        BlockPos pos,
        int interactionDiscriminator
    ) {
        long tick = world.getTotalWorldTime();
        InteractionKey key = new InteractionKey(
            player.getUniqueID(),
            world.provider.getDimension(),
            pos.toImmutable(),
            interactionDiscriminator
        );
        Long previousTick = LAST_INTERACTIONS.get(key);

        if (
            previousTick != null
                && tick >= previousTick
                && tick - previousTick <= DUPLICATE_WINDOW_TICKS
        ) {
            return false;
        }

        LAST_INTERACTIONS.put(key, tick);
        if (LAST_INTERACTIONS.size() > MAX_TRACKED_INTERACTIONS) {
            LAST_INTERACTIONS.clear();
            LAST_INTERACTIONS.put(key, tick);
        }
        return true;
    }

    private static final class InteractionKey {
        private final UUID playerId;
        private final int dimension;
        private final BlockPos pos;
        private final int interactionDiscriminator;

        private InteractionKey(
            UUID playerId,
            int dimension,
            BlockPos pos,
            int interactionDiscriminator
        ) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.pos = pos;
            this.interactionDiscriminator = interactionDiscriminator;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof InteractionKey)) {
                return false;
            }
            InteractionKey key = (InteractionKey) other;
            return dimension == key.dimension
                && interactionDiscriminator == key.interactionDiscriminator
                && playerId.equals(key.playerId)
                && pos.equals(key.pos);
        }

        @Override
        public int hashCode() {
            int result = playerId.hashCode();
            result = 31 * result + dimension;
            result = 31 * result + pos.hashCode();
            result = 31 * result + interactionDiscriminator;
            return result;
        }
    }
}
