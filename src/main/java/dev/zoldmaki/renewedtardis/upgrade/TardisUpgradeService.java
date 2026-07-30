package dev.zoldmaki.renewedtardis.upgrade;

import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;

/** Shared validation and gameplay effects for persistent upgrade components. */
public final class TardisUpgradeService {
    private TardisUpgradeService() {
    }

    public static InstallResult install(TardisWorldData data, TardisRecord record, TardisUpgradeType type) {
        if (data == null || record == null || type == null) {
            return InstallResult.failure("The upgrade could not be identified.");
        }
        if (record.isInFlight()) {
            return InstallResult.failure("Upgrades cannot be changed while the TARDIS is in flight.");
        }
        if (record.getInstalledUpgradeCount() >= TardisConfig.getUpgradeSlotCount()) {
            return InstallResult.failure("All TARDIS upgrade slots are occupied.");
        }
        int current = record.getUpgradeCount(type);
        int maximum = getMaximumPerType(type);
        if (current >= maximum) {
            return InstallResult.failure(type.getDisplayName() + " is already at its limit of " + maximum + ".");
        }
        if (!data.setUpgradeCount(record.getTardisId(), type, current + 1)) {
            return InstallResult.failure("The upgrade could not be saved.");
        }
        return InstallResult.success(
            "Installed " + type.getDisplayName() + " " + (current + 1) + "/" + maximum + "."
        );
    }

    public static int getMaximumPerType(TardisUpgradeType type) {
        switch (type) {
            case SPEED:
                return TardisConfig.getMaximumSpeedUpgrades();
            case CAPACITY:
                return TardisConfig.getMaximumCapacityUpgrades();
            case REGENERATION:
                return TardisConfig.getMaximumRegenerationUpgrades();
            case ROOM:
                return TardisConfig.getMaximumRoomUpgrades();
            case PROTECTION:
                return TardisConfig.getMaximumProtectionUpgrades();
            default:
                return 0;
        }
    }

    public static long getCapacityBonus(TardisRecord record) {
        return safeMultiply(
            record.getUpgradeCount(TardisUpgradeType.CAPACITY),
            TardisConfig.getCapacityBonusPerUpgrade()
        );
    }

    public static long getRegenerationPerSecond(TardisRecord record) {
        long bonus = safeMultiply(
            record.getUpgradeCount(TardisUpgradeType.REGENERATION),
            TardisConfig.getRegenerationBonusPerUpgrade()
        );
        return safeAdd(TardisConfig.getArtronRegenerationPerSecond(), bonus);
    }

    public static long applyFlightDuration(TardisRecord record, long durationMillis) {
        int reduction = record.getUpgradeCount(TardisUpgradeType.SPEED)
            * TardisConfig.getSpeedDurationReductionPercentPerUpgrade();
        int remainingPercent = Math.max(10, 100 - Math.max(0, reduction));
        return multiplyPercentCeil(durationMillis, remainingPercent);
    }

    public static long applyRoomCost(TardisRecord record, long baseCost) {
        int reduction = record.getUpgradeCount(TardisUpgradeType.ROOM)
            * TardisConfig.getRoomCostReductionPercentPerUpgrade();
        int remainingPercent = Math.max(10, 100 - Math.max(0, reduction));
        return multiplyPercentCeil(baseCost, remainingPercent);
    }

    public static int getRoomLimit(TardisRecord record) {
        long limit = (long) TardisConfig.getBaseExpandableRoomLimit()
            + (long) record.getUpgradeCount(TardisUpgradeType.ROOM)
                * TardisConfig.getRoomLimitBonusPerUpgrade();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, limit));
    }

    public static int applyWearReduction(TardisRecord record, int wear) {
        int safeWear = Math.max(0, wear);
        int reduction = record.getUpgradeCount(TardisUpgradeType.PROTECTION)
            * TardisConfig.getProtectionWearReductionPercentPerUpgrade();
        int remainingPercent = Math.max(0, 100 - Math.max(0, reduction));
        long reduced = multiplyPercentCeil(safeWear, remainingPercent);
        return (int) Math.min(Integer.MAX_VALUE, reduced);
    }

    public static String describe(TardisRecord record) {
        StringBuilder builder = new StringBuilder();
        builder.append(record.getInstalledUpgradeCount())
            .append('/')
            .append(TardisConfig.getUpgradeSlotCount())
            .append(" slots");
        for (TardisUpgradeType type : TardisUpgradeType.values()) {
            int count = record.getUpgradeCount(type);
            if (count > 0) {
                builder.append(", ").append(type.getName()).append('=').append(count);
            }
        }
        return builder.toString();
    }

    public static String describeEffect(TardisRecord record, TardisUpgradeType type) {
        int count = record.getUpgradeCount(type);
        switch (type) {
            case SPEED:
                return (count * TardisConfig.getSpeedDurationReductionPercentPerUpgrade())
                    + "% shorter flight time";
            case CAPACITY:
                return "+" + (count * (long) TardisConfig.getCapacityBonusPerUpgrade()) + " AU capacity";
            case REGENERATION:
                return "+" + (count * (long) TardisConfig.getRegenerationBonusPerUpgrade()) + " AU/s regeneration";
            case ROOM:
                return "+" + (count * TardisConfig.getRoomLimitBonusPerUpgrade()) + " room capacity, "
                    + (count * TardisConfig.getRoomCostReductionPercentPerUpgrade()) + "% lower room cost";
            case PROTECTION:
                return (count * TardisConfig.getProtectionWearReductionPercentPerUpgrade())
                    + "% less flight wear";
            default:
                return "no effect";
        }
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

    private static long safeMultiply(long left, long right) {
        long a = Math.max(0L, left);
        long b = Math.max(0L, right);
        if (a == 0L || b == 0L) {
            return 0L;
        }
        return a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
    }

    private static long safeAdd(long left, long right) {
        long a = Math.max(0L, left);
        long b = Math.max(0L, right);
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }

    public static final class InstallResult {
        private final boolean success;
        private final String message;

        private InstallResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static InstallResult success(String message) {
            return new InstallResult(true, message);
        }

        public static InstallResult failure(String message) {
            return new InstallResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
