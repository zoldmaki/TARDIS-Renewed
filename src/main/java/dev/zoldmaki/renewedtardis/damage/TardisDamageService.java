package dev.zoldmaki.renewedtardis.damage;

import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.flight.FlightAccuracy;
import dev.zoldmaki.renewedtardis.flight.FlightSpeed;
import dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeService;

/** Shared calculations for subsystem wear, penalties, diagnostics, and repair. */
public final class TardisDamageService {
    public static final int MAX_HEALTH = 100;

    private TardisDamageService() {
    }

    public static String getTakeoffFailure(TardisRecord record) {
        if (!TardisConfig.isDamageSystemEnabled()) {
            return "";
        }
        int threshold = TardisConfig.getCriticalSubsystemHealth();
        if (record.getSubsystemHealth(TardisSubsystem.DEMATERIALIZATION) < threshold) {
            return "The dematerialization matrix is critically damaged and cannot initiate takeoff.";
        }
        if (record.getSubsystemHealth(TardisSubsystem.ENGINES) < threshold) {
            return "The engines are critically damaged and cannot sustain takeoff.";
        }
        return "";
    }

    public static long applyEnergyPenalty(TardisRecord record, long baseCost) {
        if (!TardisConfig.isDamageSystemEnabled()) {
            return Math.max(0L, baseCost);
        }
        int missing = MAX_HEALTH - record.getSubsystemHealth(TardisSubsystem.ENGINES);
        int penaltyPercent = (missing * TardisConfig.getEngineEnergyPenaltyAtZeroHealthPercent()) / MAX_HEALTH;
        return multiplyPercentCeil(baseCost, 100 + penaltyPercent);
    }

    public static long applyDurationPenalty(TardisRecord record, long baseDurationMillis) {
        if (!TardisConfig.isDamageSystemEnabled()) {
            return Math.max(0L, baseDurationMillis);
        }
        int missing = MAX_HEALTH - record.getSubsystemHealth(TardisSubsystem.STABILIZERS);
        int penaltyPercent = (missing * TardisConfig.getStabilizerDurationPenaltyAtZeroHealthPercent()) / MAX_HEALTH;
        return multiplyPercentCeil(baseDurationMillis, 100 + penaltyPercent);
    }

    public static int getNavigationDriftBonus(TardisRecord record) {
        if (!TardisConfig.isDamageSystemEnabled()) {
            return 0;
        }
        int missing = MAX_HEALTH - record.getSubsystemHealth(TardisSubsystem.NAVIGATION);
        return Math.min(TardisConfig.getMaximumDamageDriftRadius(), missing / 20);
    }

    /** Applies deterministic wear once takeoff has successfully dematerialized. */
    public static void applyTakeoffWear(
        TardisWorldData data,
        TardisRecord record,
        DimensionPosition departure,
        DimensionPosition destination
    ) {
        if (!TardisConfig.isDamageSystemEnabled()) {
            return;
        }

        int base = TardisConfig.getBaseSubsystemWearPerFlight();
        int fast = record.getFlightSpeed() == FlightSpeed.FAST
            ? TardisConfig.getFastFlightExtraWear()
            : 0;
        int risky = record.getFlightAccuracy() == FlightAccuracy.RISKY
            ? TardisConfig.getRiskyFlightExtraWear()
            : 0;
        int crossDimension = departure.getDimension() != destination.getDimension()
            ? TardisConfig.getCrossDimensionExtraWear()
            : 0;

        data.damageSubsystem(
            record.getTardisId(),
            TardisSubsystem.ENGINES,
            TardisUpgradeService.applyWearReduction(record, base + fast + crossDimension)
        );
        data.damageSubsystem(
            record.getTardisId(),
            TardisSubsystem.NAVIGATION,
            TardisUpgradeService.applyWearReduction(record, base + risky + crossDimension)
        );
        data.damageSubsystem(
            record.getTardisId(),
            TardisSubsystem.STABILIZERS,
            TardisUpgradeService.applyWearReduction(record, base + fast + risky)
        );
        data.damageSubsystem(
            record.getTardisId(),
            TardisSubsystem.DEMATERIALIZATION,
            TardisUpgradeService.applyWearReduction(record, base)
        );
    }

    public static TardisSubsystem findMostDamaged(TardisRecord record) {
        TardisSubsystem result = TardisSubsystem.ENGINES;
        int lowest = record.getSubsystemHealth(result);
        for (TardisSubsystem subsystem : TardisSubsystem.values()) {
            int health = record.getSubsystemHealth(subsystem);
            if (health < lowest) {
                lowest = health;
                result = subsystem;
            }
        }
        return result;
    }

    public static String describe(TardisRecord record) {
        StringBuilder builder = new StringBuilder();
        for (TardisSubsystem subsystem : TardisSubsystem.values()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(subsystem.getName())
                .append('=')
                .append(record.getSubsystemHealth(subsystem))
                .append('%');
        }
        return builder.toString();
    }

    public static String describeCondition(int health) {
        int safeHealth = Math.max(0, Math.min(MAX_HEALTH, health));
        if (safeHealth >= 90) {
            return "optimal";
        }
        if (safeHealth >= 65) {
            return "operational";
        }
        if (safeHealth >= 35) {
            return "damaged";
        }
        if (safeHealth >= TardisConfig.getCriticalSubsystemHealth()) {
            return "severely damaged";
        }
        return "critical";
    }

    public static String formatSubsystem(TardisRecord record, TardisSubsystem subsystem) {
        int health = record.getSubsystemHealth(subsystem);
        return subsystem.getDisplayName() + ": " + health + "% (" + describeCondition(health) + ")";
    }

    private static long multiplyPercentCeil(long value, int percent) {
        long safeValue = Math.max(0L, value);
        long safePercent = Math.max(0L, percent);
        if (safeValue == 0L || safePercent == 0L) {
            return 0L;
        }
        if (safeValue > Long.MAX_VALUE / safePercent) {
            return Long.MAX_VALUE;
        }
        return (safeValue * safePercent + 99L) / 100L;
    }
}
