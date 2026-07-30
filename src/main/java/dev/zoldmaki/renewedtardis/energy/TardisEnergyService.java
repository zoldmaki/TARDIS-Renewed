package dev.zoldmaki.renewedtardis.energy;

import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.damage.TardisDamageService;
import dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeService;
import net.minecraft.util.math.BlockPos;

/** Shared calculations and formatting for persistent Artron energy. */
public final class TardisEnergyService {
    private TardisEnergyService() {
    }

    public static long refresh(TardisWorldData data, TardisRecord record) {
        return data.refreshArtronEnergy(record.getTardisId(), System.currentTimeMillis());
    }

    public static boolean consume(TardisWorldData data, TardisRecord record, long amount) {
        return data.consumeArtronEnergy(record.getTardisId(), amount, System.currentTimeMillis());
    }

    public static long refund(TardisWorldData data, TardisRecord record, long amount) {
        return data.addArtronEnergy(record.getTardisId(), amount, System.currentTimeMillis());
    }

    public static long calculateFlightCost(DimensionPosition departure, DimensionPosition destination) {
        long cost = TardisConfig.getBaseFlightEnergyCost();
        if (departure.getDimension() == destination.getDimension()) {
            BlockPos from = departure.getPosition();
            BlockPos to = destination.getPosition();
            double dx = (double) to.getX() - from.getX();
            double dz = (double) to.getZ() - from.getZ();
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            cost += (long) Math.floor(horizontalDistance / TardisConfig.getBlocksPerExtraFlightEnergy());
        } else {
            cost += TardisConfig.getCrossDimensionFlightEnergyCost();
        }
        return Math.max(0L, cost);
    }

    public static long calculateFlightCost(
        TardisRecord record,
        DimensionPosition departure,
        DimensionPosition destination
    ) {
        return TardisDamageService.applyEnergyPenalty(
            record,
            calculateFlightCost(departure, destination)
        );
    }

    public static long getRegenerationPerSecond(TardisRecord record) {
        return TardisUpgradeService.getRegenerationPerSecond(record);
    }

    public static String describe(TardisRecord record) {
        long capacity = Math.max(1L, record.getArtronCapacity());
        long energy = Math.max(0L, Math.min(capacity, record.getArtronEnergy()));
        long percent = Math.min(100L, (long) Math.floor(((double) energy * 100.0D) / (double) capacity));
        return format(energy) + "/" + format(capacity) + " AU (" + percent + "%)";
    }

    public static String format(long amount) {
        return String.format(java.util.Locale.ROOT, "%,d", Math.max(0L, amount));
    }
}
