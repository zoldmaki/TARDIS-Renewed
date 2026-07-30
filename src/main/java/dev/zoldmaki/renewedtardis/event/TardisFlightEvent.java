package dev.zoldmaki.renewedtardis.event;

import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.flight.TardisFlightService;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Internal Forge event foundation for integrations that need to observe or
 * veto TARDIS travel without replacing the flight service.
 */
public abstract class TardisFlightEvent extends Event {
    private final EntityPlayerMP pilot;
    private final TardisRecord record;
    private final DimensionPosition departure;
    private final DimensionPosition target;

    protected TardisFlightEvent(
        EntityPlayerMP pilot,
        TardisRecord record,
        DimensionPosition departure,
        DimensionPosition target
    ) {
        this.pilot = pilot;
        this.record = record;
        this.departure = departure;
        this.target = target;
    }

    public EntityPlayerMP getPilot() {
        return pilot;
    }

    public TardisRecord getRecord() {
        return record;
    }

    public DimensionPosition getDeparture() {
        return departure;
    }

    public DimensionPosition getTarget() {
        return target;
    }

    @Cancelable
    public static final class Takeoff extends TardisFlightEvent {
        private final long durationMillis;

        public Takeoff(
            EntityPlayerMP pilot,
            TardisRecord record,
            DimensionPosition departure,
            DimensionPosition target,
            long durationMillis
        ) {
            super(pilot, record, departure, target);
            this.durationMillis = durationMillis;
        }

        public long getDurationMillis() {
            return durationMillis;
        }
    }

    @Cancelable
    public static final class Landing extends TardisFlightEvent {
        private final BlockPos actualTarget;
        private final TardisFlightService.LandingMode mode;
        private final boolean forced;

        public Landing(
            EntityPlayerMP pilot,
            TardisRecord record,
            DimensionPosition departure,
            DimensionPosition requestedTarget,
            BlockPos actualTarget,
            TardisFlightService.LandingMode mode,
            boolean forced
        ) {
            super(pilot, record, departure, requestedTarget);
            this.actualTarget = actualTarget;
            this.mode = mode;
            this.forced = forced;
        }

        public BlockPos getActualTarget() {
            return actualTarget;
        }

        public TardisFlightService.LandingMode getMode() {
            return mode;
        }

        public boolean isForced() {
            return forced;
        }
    }

    public static final class Landed extends TardisFlightEvent {
        private final int capturedPlayers;

        public Landed(
            EntityPlayerMP pilot,
            TardisRecord record,
            DimensionPosition departure,
            DimensionPosition landedPosition,
            int capturedPlayers
        ) {
            super(pilot, record, departure, landedPosition);
            this.capturedPlayers = capturedPlayers;
        }

        public int getCapturedPlayers() {
            return capturedPlayers;
        }
    }
}
