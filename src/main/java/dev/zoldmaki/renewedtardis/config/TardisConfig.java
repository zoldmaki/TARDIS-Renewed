package dev.zoldmaki.renewedtardis.config;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import java.io.File;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/** Loads startup and runtime settings for dimensions, flight, energy, and damage. */
public final class TardisConfig {
    public static final String CATEGORY_DIMENSIONS = "dimensions";
    public static final String CATEGORY_FLIGHT = "flight";
    public static final String CATEGORY_ENERGY = "artron_energy";
    public static final String CATEGORY_DAMAGE = "damage";
    public static final String CATEGORY_UPGRADES = "upgrades";
    public static final String CATEGORY_INTEGRATIONS = "integrations";
    public static final String CATEGORY_MAINTENANCE = "maintenance";

    public static final int DEFAULT_INTERIOR_DIMENSION_ID = 1212;
    public static final int DEFAULT_MINIMUM_FLIGHT_SECONDS = 8;
    public static final int DEFAULT_MAXIMUM_FLIGHT_SECONDS = 60;
    public static final int DEFAULT_BLOCKS_PER_EXTRA_SECOND = 256;
    public static final int DEFAULT_CROSS_DIMENSION_PENALTY_SECONDS = 10;
    public static final int DEFAULT_SAFE_LANDING_SEARCH_RADIUS = 16;
    public static final boolean DEFAULT_ALLOW_CROSS_DIMENSION_FLIGHT = true;

    public static final int DEFAULT_ARTRON_CAPACITY = 10000;
    public static final int DEFAULT_STARTING_ARTRON_ENERGY = 10000;
    public static final int DEFAULT_ARTRON_REGEN_PER_SECOND = 10;
    public static final int DEFAULT_BASE_FLIGHT_ENERGY_COST = 250;
    public static final int DEFAULT_BLOCKS_PER_EXTRA_FLIGHT_ENERGY = 64;
    public static final int DEFAULT_CROSS_DIMENSION_FLIGHT_ENERGY_COST = 750;
    public static final int DEFAULT_BASIC_ROOM_ENERGY_COST = 500;
    public static final int DEFAULT_WORKSHOP_ROOM_ENERGY_COST = 750;
    public static final int DEFAULT_STORAGE_ROOM_ENERGY_COST = 650;

    public static final boolean DEFAULT_DAMAGE_SYSTEM_ENABLED = true;
    public static final int DEFAULT_BASE_SUBSYSTEM_WEAR_PER_FLIGHT = 1;
    public static final int DEFAULT_FAST_FLIGHT_EXTRA_WEAR = 2;
    public static final int DEFAULT_RISKY_FLIGHT_EXTRA_WEAR = 3;
    public static final int DEFAULT_CROSS_DIMENSION_EXTRA_WEAR = 2;
    public static final int DEFAULT_CRITICAL_SUBSYSTEM_HEALTH = 10;
    public static final int DEFAULT_REPAIR_PER_CHRONOSTEEL = 25;
    public static final int DEFAULT_ENGINE_ENERGY_PENALTY_AT_ZERO_HEALTH_PERCENT = 50;
    public static final int DEFAULT_STABILIZER_DURATION_PENALTY_AT_ZERO_HEALTH_PERCENT = 50;
    public static final int DEFAULT_MAXIMUM_DAMAGE_DRIFT_RADIUS = 5;

    public static final int DEFAULT_UPGRADE_SLOT_COUNT = 8;
    public static final int DEFAULT_MAX_SPEED_UPGRADES = 3;
    public static final int DEFAULT_MAX_CAPACITY_UPGRADES = 4;
    public static final int DEFAULT_MAX_REGENERATION_UPGRADES = 3;
    public static final int DEFAULT_MAX_ROOM_UPGRADES = 3;
    public static final int DEFAULT_MAX_PROTECTION_UPGRADES = 3;
    public static final int DEFAULT_SPEED_DURATION_REDUCTION_PERCENT = 10;
    public static final int DEFAULT_CAPACITY_BONUS_PER_UPGRADE = 2500;
    public static final int DEFAULT_REGENERATION_BONUS_PER_UPGRADE = 5;
    public static final int DEFAULT_BASE_EXPANDABLE_ROOM_LIMIT = 12;
    public static final int DEFAULT_ROOM_LIMIT_BONUS_PER_UPGRADE = 4;
    public static final int DEFAULT_ROOM_COST_REDUCTION_PERCENT = 15;
    public static final int DEFAULT_PROTECTION_WEAR_REDUCTION_PERCENT = 20;

    public static final boolean DEFAULT_HWYLA_INTEGRATION_ENABLED = true;
    public static final boolean DEFAULT_COMPUTERCRAFT_INTEGRATION_ENABLED = true;

    public static final int DEFAULT_MAXIMUM_DOCTOR_MESSAGES = 12;
    public static final boolean DEFAULT_SUCCESSFUL_REPAIR_LOGGING = true;

    private static File configFile;
    private static Configuration configuration;

    private static int interiorDimensionId = DEFAULT_INTERIOR_DIMENSION_ID;
    private static int minimumFlightSeconds = DEFAULT_MINIMUM_FLIGHT_SECONDS;
    private static int maximumFlightSeconds = DEFAULT_MAXIMUM_FLIGHT_SECONDS;
    private static int blocksPerExtraSecond = DEFAULT_BLOCKS_PER_EXTRA_SECOND;
    private static int crossDimensionPenaltySeconds = DEFAULT_CROSS_DIMENSION_PENALTY_SECONDS;
    private static int safeLandingSearchRadius = DEFAULT_SAFE_LANDING_SEARCH_RADIUS;
    private static boolean allowCrossDimensionFlight = DEFAULT_ALLOW_CROSS_DIMENSION_FLIGHT;
    private static int[] blockedDestinationDimensions = new int[0];

    private static int defaultArtronCapacity = DEFAULT_ARTRON_CAPACITY;
    private static int startingArtronEnergy = DEFAULT_STARTING_ARTRON_ENERGY;
    private static int artronRegenerationPerSecond = DEFAULT_ARTRON_REGEN_PER_SECOND;
    private static int baseFlightEnergyCost = DEFAULT_BASE_FLIGHT_ENERGY_COST;
    private static int blocksPerExtraFlightEnergy = DEFAULT_BLOCKS_PER_EXTRA_FLIGHT_ENERGY;
    private static int crossDimensionFlightEnergyCost = DEFAULT_CROSS_DIMENSION_FLIGHT_ENERGY_COST;
    private static int basicRoomEnergyCost = DEFAULT_BASIC_ROOM_ENERGY_COST;
    private static int workshopRoomEnergyCost = DEFAULT_WORKSHOP_ROOM_ENERGY_COST;
    private static int storageRoomEnergyCost = DEFAULT_STORAGE_ROOM_ENERGY_COST;

    private static boolean damageSystemEnabled = DEFAULT_DAMAGE_SYSTEM_ENABLED;
    private static int baseSubsystemWearPerFlight = DEFAULT_BASE_SUBSYSTEM_WEAR_PER_FLIGHT;
    private static int fastFlightExtraWear = DEFAULT_FAST_FLIGHT_EXTRA_WEAR;
    private static int riskyFlightExtraWear = DEFAULT_RISKY_FLIGHT_EXTRA_WEAR;
    private static int crossDimensionExtraWear = DEFAULT_CROSS_DIMENSION_EXTRA_WEAR;
    private static int criticalSubsystemHealth = DEFAULT_CRITICAL_SUBSYSTEM_HEALTH;
    private static int repairPerChronosteel = DEFAULT_REPAIR_PER_CHRONOSTEEL;
    private static int engineEnergyPenaltyAtZeroHealthPercent = DEFAULT_ENGINE_ENERGY_PENALTY_AT_ZERO_HEALTH_PERCENT;
    private static int stabilizerDurationPenaltyAtZeroHealthPercent = DEFAULT_STABILIZER_DURATION_PENALTY_AT_ZERO_HEALTH_PERCENT;
    private static int maximumDamageDriftRadius = DEFAULT_MAXIMUM_DAMAGE_DRIFT_RADIUS;

    private static int upgradeSlotCount = DEFAULT_UPGRADE_SLOT_COUNT;
    private static int maximumSpeedUpgrades = DEFAULT_MAX_SPEED_UPGRADES;
    private static int maximumCapacityUpgrades = DEFAULT_MAX_CAPACITY_UPGRADES;
    private static int maximumRegenerationUpgrades = DEFAULT_MAX_REGENERATION_UPGRADES;
    private static int maximumRoomUpgrades = DEFAULT_MAX_ROOM_UPGRADES;
    private static int maximumProtectionUpgrades = DEFAULT_MAX_PROTECTION_UPGRADES;
    private static int speedDurationReductionPercentPerUpgrade = DEFAULT_SPEED_DURATION_REDUCTION_PERCENT;
    private static int capacityBonusPerUpgrade = DEFAULT_CAPACITY_BONUS_PER_UPGRADE;
    private static int regenerationBonusPerUpgrade = DEFAULT_REGENERATION_BONUS_PER_UPGRADE;
    private static int baseExpandableRoomLimit = DEFAULT_BASE_EXPANDABLE_ROOM_LIMIT;
    private static int roomLimitBonusPerUpgrade = DEFAULT_ROOM_LIMIT_BONUS_PER_UPGRADE;
    private static int roomCostReductionPercentPerUpgrade = DEFAULT_ROOM_COST_REDUCTION_PERCENT;
    private static int protectionWearReductionPercentPerUpgrade = DEFAULT_PROTECTION_WEAR_REDUCTION_PERCENT;

    private static boolean hwylaIntegrationEnabled = DEFAULT_HWYLA_INTEGRATION_ENABLED;
    private static boolean computerCraftIntegrationEnabled = DEFAULT_COMPUTERCRAFT_INTEGRATION_ENABLED;

    private static int maximumDoctorMessages = DEFAULT_MAXIMUM_DOCTOR_MESSAGES;
    private static boolean successfulRepairLoggingEnabled = DEFAULT_SUCCESSFUL_REPAIR_LOGGING;

    private TardisConfig() {
    }

    /** Initial startup load. The configured interior dimension is applied here. */
    public static void load(File file) {
        configFile = file;
        loadInternal(true);
    }

    /** Reloads dynamic values after the client config GUI saves. */
    public static void reload() {
        if (configFile != null) {
            loadInternal(false);
        }
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static String getConfigPath() {
        return configFile == null ? "renewedtardis.cfg" : configFile.getAbsolutePath();
    }

    private static void loadInternal(boolean startup) {
        Configuration loaded = new Configuration(configFile);
        loaded.load();
        configuration = loaded;

        loaded.setCategoryComment(
            CATEGORY_DIMENSIONS,
            "Dimension registration settings. These values are read only during game startup."
        );
        loaded.setCategoryLanguageKey(CATEGORY_DIMENSIONS, "renewedtardis.config.category.dimensions");

        Property dimensionProperty = loaded.get(
            CATEGORY_DIMENSIONS,
            "tardisInteriorDimensionId",
            DEFAULT_INTERIOR_DIMENSION_ID,
            "Shared TARDIS interior dimension ID.\n"
                + "WARNING: Choose this value before creating a world. Changing it after any TARDIS interior "
                + "has been generated can make existing interiors inaccessible or cause unforeseen errors. "
                + "Changing it on an existing world is highly unrecommended.\n"
                + "Restart Minecraft or the dedicated server after editing this value.",
            -1000000,
            1000000
        );
        dimensionProperty
            .setLanguageKey("renewedtardis.config.tardisInteriorDimensionId")
            .setRequiresMcRestart(true);

        int configuredDimensionId = dimensionProperty.getInt(DEFAULT_INTERIOR_DIMENSION_ID);
        validateInteriorDimensionId(configuredDimensionId);
        if (startup) {
            interiorDimensionId = configuredDimensionId;
        } else if (configuredDimensionId != interiorDimensionId && RenewedTardisMod.logger != null) {
            RenewedTardisMod.logger.warn(
                "The TARDIS interior dimension was changed from {} to {} in the config GUI. "
                    + "The running game will keep using {} until Minecraft or the dedicated server restarts.",
                interiorDimensionId,
                configuredDimensionId,
                interiorDimensionId
            );
        }

        loaded.setCategoryComment(
            CATEGORY_FLIGHT,
            "Flight timing, recovery, and destination restriction settings. Times use real elapsed seconds and survive restarts."
        );
        loaded.setCategoryLanguageKey(CATEGORY_FLIGHT, "renewedtardis.config.category.flight");

        minimumFlightSeconds = intProperty(
            loaded,
            CATEGORY_FLIGHT,
            "minimumFlightSeconds",
            DEFAULT_MINIMUM_FLIGHT_SECONDS,
            0,
            3600,
            "Shortest normal flight duration in seconds."
        );
        maximumFlightSeconds = intProperty(
            loaded,
            CATEGORY_FLIGHT,
            "maximumFlightSeconds",
            DEFAULT_MAXIMUM_FLIGHT_SECONDS,
            0,
            3600,
            "Longest normal flight duration in seconds. Must not be below minimumFlightSeconds."
        );
        blocksPerExtraSecond = intProperty(
            loaded,
            CATEGORY_FLIGHT,
            "blocksPerExtraSecond",
            DEFAULT_BLOCKS_PER_EXTRA_SECOND,
            1,
            1000000,
            "Horizontal blocks of same-dimension travel that add one second to the minimum flight time."
        );
        crossDimensionPenaltySeconds = intProperty(
            loaded,
            CATEGORY_FLIGHT,
            "crossDimensionPenaltySeconds",
            DEFAULT_CROSS_DIMENSION_PENALTY_SECONDS,
            0,
            3600,
            "Additional seconds added when departure and destination are in different dimensions."
        );
        safeLandingSearchRadius = intProperty(
            loaded,
            CATEGORY_FLIGHT,
            "safeLandingSearchRadius",
            DEFAULT_SAFE_LANDING_SEARCH_RADIUS,
            1,
            64,
            "Maximum horizontal radius used by /tr land nearest and /tr land departure."
        );

        Property allowCrossDimensionProperty = loaded.get(
            CATEGORY_FLIGHT,
            "allowCrossDimensionFlight",
            DEFAULT_ALLOW_CROSS_DIMENSION_FLIGHT,
            "When false, a TARDIS may only land in the same dimension from which it departed."
        );
        allowCrossDimensionProperty.setLanguageKey("renewedtardis.config.allowCrossDimensionFlight");
        allowCrossDimensionFlight = allowCrossDimensionProperty.getBoolean(DEFAULT_ALLOW_CROSS_DIMENSION_FLIGHT);

        Property blockedDimensionsProperty = loaded.get(
            CATEGORY_FLIGHT,
            "blockedDestinationDimensions",
            new int[0],
            "Dimension IDs in which TARDIS exteriors may never materialize. Add one integer dimension ID per list entry. "
                + "The configured TARDIS interior dimension is always blocked separately."
        );
        blockedDimensionsProperty
            .setLanguageKey("renewedtardis.config.blockedDestinationDimensions")
            .setMaxListLength(256);
        blockedDestinationDimensions = blockedDimensionsProperty.getIntList();

        if (maximumFlightSeconds < minimumFlightSeconds) {
            maximumFlightSeconds = minimumFlightSeconds;
            loaded.get(CATEGORY_FLIGHT, "maximumFlightSeconds", DEFAULT_MAXIMUM_FLIGHT_SECONDS)
                .set(maximumFlightSeconds);
        }

        loaded.setCategoryComment(
            CATEGORY_ENERGY,
            "Persistent Artron energy settings. Existing TARDISes store their own capacity, while costs and regeneration use these values."
        );
        loaded.setCategoryLanguageKey(CATEGORY_ENERGY, "renewedtardis.config.category.artron_energy");

        defaultArtronCapacity = intProperty(
            loaded,
            CATEGORY_ENERGY,
            "defaultCapacity",
            DEFAULT_ARTRON_CAPACITY,
            1,
            100000000,
            "Capacity assigned to newly created TARDIS records and old saves that predate the energy system."
        );
        startingArtronEnergy = intProperty(
            loaded,
            CATEGORY_ENERGY,
            "startingEnergy",
            DEFAULT_STARTING_ARTRON_ENERGY,
            0,
            100000000,
            "Energy assigned to newly created TARDIS records. This is clamped to their capacity."
        );
        artronRegenerationPerSecond = intProperty(
            loaded,
            CATEGORY_ENERGY,
            "regenerationPerSecond",
            DEFAULT_ARTRON_REGEN_PER_SECOND,
            0,
            1000000,
            "Artron units regenerated per real second. Regeneration is calculated lazily and survives restarts."
        );
        baseFlightEnergyCost = intProperty(
            loaded,
            CATEGORY_ENERGY,
            "baseFlightCost",
            DEFAULT_BASE_FLIGHT_ENERGY_COST,
            0,
            100000000,
            "Base Artron cost paid when a normal flight begins."
        );
        blocksPerExtraFlightEnergy = intProperty(
            loaded,
            CATEGORY_ENERGY,
            "blocksPerExtraFlightEnergy",
            DEFAULT_BLOCKS_PER_EXTRA_FLIGHT_ENERGY,
            1,
            1000000,
            "Same-dimension horizontal blocks travelled per one additional Artron unit."
        );
        crossDimensionFlightEnergyCost = intProperty(
            loaded,
            CATEGORY_ENERGY,
            "crossDimensionFlightCost",
            DEFAULT_CROSS_DIMENSION_FLIGHT_ENERGY_COST,
            0,
            100000000,
            "Additional Artron cost for travelling to another dimension."
        );
        basicRoomEnergyCost = intProperty(
            loaded,
            CATEGORY_ENERGY,
            "basicRoomCost",
            DEFAULT_BASIC_ROOM_ENERGY_COST,
            0,
            100000000,
            "Artron cost for installing the basic room schema."
        );
        workshopRoomEnergyCost = intProperty(
            loaded,
            CATEGORY_ENERGY,
            "workshopRoomCost",
            DEFAULT_WORKSHOP_ROOM_ENERGY_COST,
            0,
            100000000,
            "Artron cost for installing the workshop room schema."
        );
        storageRoomEnergyCost = intProperty(
            loaded,
            CATEGORY_ENERGY,
            "storageRoomCost",
            DEFAULT_STORAGE_ROOM_ENERGY_COST,
            0,
            100000000,
            "Artron cost for installing the storage room schema."
        );

        if (startingArtronEnergy > defaultArtronCapacity) {
            startingArtronEnergy = defaultArtronCapacity;
            loaded.get(CATEGORY_ENERGY, "startingEnergy", DEFAULT_STARTING_ARTRON_ENERGY)
                .set(startingArtronEnergy);
        }

        loaded.setCategoryComment(
            CATEGORY_DAMAGE,
            "Subsystem wear, penalties, and Chronosteel repair settings. Existing TARDISes migrate at full health."
        );
        loaded.setCategoryLanguageKey(CATEGORY_DAMAGE, "renewedtardis.config.category.damage");

        Property damageEnabledProperty = loaded.get(
            CATEGORY_DAMAGE,
            "enabled",
            DEFAULT_DAMAGE_SYSTEM_ENABLED,
            "Enables persistent TARDIS subsystem wear and its flight penalties."
        );
        damageEnabledProperty.setLanguageKey("renewedtardis.config.damage.enabled");
        damageSystemEnabled = damageEnabledProperty.getBoolean(DEFAULT_DAMAGE_SYSTEM_ENABLED);

        baseSubsystemWearPerFlight = damageIntProperty(
            loaded,
            "baseWearPerFlight",
            DEFAULT_BASE_SUBSYSTEM_WEAR_PER_FLIGHT,
            0,
            100,
            "Base health lost by each affected subsystem after a successful takeoff."
        );
        fastFlightExtraWear = damageIntProperty(
            loaded,
            "fastFlightExtraWear",
            DEFAULT_FAST_FLIGHT_EXTRA_WEAR,
            0,
            100,
            "Extra engine and stabilizer wear when using fast flight."
        );
        riskyFlightExtraWear = damageIntProperty(
            loaded,
            "riskyFlightExtraWear",
            DEFAULT_RISKY_FLIGHT_EXTRA_WEAR,
            0,
            100,
            "Extra navigation and stabilizer wear when using risky accuracy."
        );
        crossDimensionExtraWear = damageIntProperty(
            loaded,
            "crossDimensionExtraWear",
            DEFAULT_CROSS_DIMENSION_EXTRA_WEAR,
            0,
            100,
            "Extra engine and navigation wear for cross-dimensional flights."
        );
        criticalSubsystemHealth = damageIntProperty(
            loaded,
            "criticalHealthThreshold",
            DEFAULT_CRITICAL_SUBSYSTEM_HEALTH,
            1,
            100,
            "Engines or dematerialization health below this value prevents normal takeoff."
        );
        repairPerChronosteel = damageIntProperty(
            loaded,
            "repairPerChronosteel",
            DEFAULT_REPAIR_PER_CHRONOSTEEL,
            1,
            100,
            "Health restored to one subsystem by one Chronosteel ingot at the engine core."
        );
        engineEnergyPenaltyAtZeroHealthPercent = damageIntProperty(
            loaded,
            "engineEnergyPenaltyAtZeroHealthPercent",
            DEFAULT_ENGINE_ENERGY_PENALTY_AT_ZERO_HEALTH_PERCENT,
            0,
            1000,
            "Maximum percentage added to flight energy cost as engine health approaches zero."
        );
        stabilizerDurationPenaltyAtZeroHealthPercent = damageIntProperty(
            loaded,
            "stabilizerDurationPenaltyAtZeroHealthPercent",
            DEFAULT_STABILIZER_DURATION_PENALTY_AT_ZERO_HEALTH_PERCENT,
            0,
            1000,
            "Maximum percentage added to flight duration as stabilizer health approaches zero."
        );
        maximumDamageDriftRadius = damageIntProperty(
            loaded,
            "maximumNavigationDamageDriftRadius",
            DEFAULT_MAXIMUM_DAMAGE_DRIFT_RADIUS,
            0,
            32,
            "Maximum extra horizontal landing drift radius caused by navigation damage."
        );

        loaded.setCategoryComment(
            CATEGORY_UPGRADES,
            "Upgrade slot limits and the effects of installed TARDIS components."
        );
        loaded.setCategoryLanguageKey(CATEGORY_UPGRADES, "renewedtardis.config.category.upgrades");

        upgradeSlotCount = upgradeIntProperty(
            loaded,
            "slotCount",
            DEFAULT_UPGRADE_SLOT_COUNT,
            0,
            64,
            "Total number of persistent upgrade components one TARDIS can hold."
        );
        maximumSpeedUpgrades = upgradeIntProperty(
            loaded,
            "maximumSpeedUpgrades",
            DEFAULT_MAX_SPEED_UPGRADES,
            0,
            64,
            "Maximum Temporal Accelerator components in one TARDIS."
        );
        maximumCapacityUpgrades = upgradeIntProperty(
            loaded,
            "maximumCapacityUpgrades",
            DEFAULT_MAX_CAPACITY_UPGRADES,
            0,
            64,
            "Maximum Artron Capacitor components in one TARDIS."
        );
        maximumRegenerationUpgrades = upgradeIntProperty(
            loaded,
            "maximumRegenerationUpgrades",
            DEFAULT_MAX_REGENERATION_UPGRADES,
            0,
            64,
            "Maximum Artron Regenerator components in one TARDIS."
        );
        maximumRoomUpgrades = upgradeIntProperty(
            loaded,
            "maximumRoomUpgrades",
            DEFAULT_MAX_ROOM_UPGRADES,
            0,
            64,
            "Maximum Architectural Expansion components in one TARDIS."
        );
        maximumProtectionUpgrades = upgradeIntProperty(
            loaded,
            "maximumProtectionUpgrades",
            DEFAULT_MAX_PROTECTION_UPGRADES,
            0,
            64,
            "Maximum Temporal Shielding components in one TARDIS."
        );
        speedDurationReductionPercentPerUpgrade = upgradeIntProperty(
            loaded,
            "speedDurationReductionPercentPerUpgrade",
            DEFAULT_SPEED_DURATION_REDUCTION_PERCENT,
            0,
            90,
            "Percentage removed from flight duration by each Temporal Accelerator."
        );
        capacityBonusPerUpgrade = upgradeIntProperty(
            loaded,
            "capacityBonusPerUpgrade",
            DEFAULT_CAPACITY_BONUS_PER_UPGRADE,
            0,
            100000000,
            "Additional Artron capacity supplied by each Artron Capacitor."
        );
        regenerationBonusPerUpgrade = upgradeIntProperty(
            loaded,
            "regenerationBonusPerUpgrade",
            DEFAULT_REGENERATION_BONUS_PER_UPGRADE,
            0,
            1000000,
            "Additional AU regenerated per second by each Artron Regenerator."
        );
        baseExpandableRoomLimit = upgradeIntProperty(
            loaded,
            "baseExpandableRoomLimit",
            DEFAULT_BASE_EXPANDABLE_ROOM_LIMIT,
            0,
            10000,
            "Expandable rooms allowed before Architectural Expansion components are counted."
        );
        roomLimitBonusPerUpgrade = upgradeIntProperty(
            loaded,
            "roomLimitBonusPerUpgrade",
            DEFAULT_ROOM_LIMIT_BONUS_PER_UPGRADE,
            0,
            10000,
            "Additional expandable-room slots supplied by each Architectural Expansion."
        );
        roomCostReductionPercentPerUpgrade = upgradeIntProperty(
            loaded,
            "roomCostReductionPercentPerUpgrade",
            DEFAULT_ROOM_COST_REDUCTION_PERCENT,
            0,
            90,
            "Percentage removed from room installation cost by each Architectural Expansion."
        );
        protectionWearReductionPercentPerUpgrade = upgradeIntProperty(
            loaded,
            "protectionWearReductionPercentPerUpgrade",
            DEFAULT_PROTECTION_WEAR_REDUCTION_PERCENT,
            0,
            100,
            "Percentage removed from deterministic flight wear by each Temporal Shielding component."
        );

        loaded.setCategoryComment(
            CATEGORY_INTEGRATIONS,
            "Optional compatibility hooks. These settings only have an effect when the matching mod is installed and require a restart."
        );
        loaded.setCategoryLanguageKey(
            CATEGORY_INTEGRATIONS,
            "renewedtardis.config.category.integrations"
        );

        Property hwylaProperty = loaded.get(
            CATEGORY_INTEGRATIONS,
            "enableHwylaIntegration",
            DEFAULT_HWYLA_INTEGRATION_ENABLED,
            "Adds server-authoritative TARDIS component information to Waila/HWYLA tooltips when that mod is installed."
        );
        hwylaProperty
            .setLanguageKey("renewedtardis.config.integrations.enableHwylaIntegration")
            .setRequiresMcRestart(true);
        hwylaIntegrationEnabled = hwylaProperty.getBoolean(DEFAULT_HWYLA_INTEGRATION_ENABLED);

        Property computerCraftProperty = loaded.get(
            CATEGORY_INTEGRATIONS,
            "enableComputerCraftIntegration",
            DEFAULT_COMPUTERCRAFT_INTEGRATION_ENABLED,
            "Exposes a read-only TARDIS peripheral through an adjacent ComputerCraft or CC:Tweaked modem when installed."
        );
        computerCraftProperty
            .setLanguageKey("renewedtardis.config.integrations.enableComputerCraftIntegration")
            .setRequiresMcRestart(true);
        computerCraftIntegrationEnabled = computerCraftProperty.getBoolean(
            DEFAULT_COMPUTERCRAFT_INTEGRATION_ENABLED
        );

        loaded.setCategoryComment(
            CATEGORY_MAINTENANCE,
            "Diagnostics and operator recovery settings. Deep diagnostics may load the saved exterior dimension and chunk only when explicitly requested."
        );
        loaded.setCategoryLanguageKey(
            CATEGORY_MAINTENANCE,
            "renewedtardis.config.category.maintenance"
        );

        Property doctorMessagesProperty = loaded.get(
            CATEGORY_MAINTENANCE,
            "maximumDoctorMessages",
            DEFAULT_MAXIMUM_DOCTOR_MESSAGES,
            "Maximum number of individual findings shown by /tr doctor before the remaining count is summarized.",
            1,
            100
        );
        doctorMessagesProperty.setLanguageKey("renewedtardis.config.maintenance.maximumDoctorMessages");
        maximumDoctorMessages = doctorMessagesProperty.getInt(DEFAULT_MAXIMUM_DOCTOR_MESSAGES);

        Property repairLoggingProperty = loaded.get(
            CATEGORY_MAINTENANCE,
            "logSuccessfulRepairs",
            DEFAULT_SUCCESSFUL_REPAIR_LOGGING,
            "Writes successful /tr repair operations to the server log for audit and support purposes."
        );
        repairLoggingProperty.setLanguageKey("renewedtardis.config.maintenance.logSuccessfulRepairs");
        successfulRepairLoggingEnabled = repairLoggingProperty.getBoolean(DEFAULT_SUCCESSFUL_REPAIR_LOGGING);

        if (loaded.hasChanged()) {
            loaded.save();
        }
    }

    private static int intProperty(
        Configuration config,
        String category,
        String name,
        int defaultValue,
        int minimum,
        int maximum,
        String comment
    ) {
        Property property = config.get(category, name, defaultValue, comment, minimum, maximum);
        property.setLanguageKey("renewedtardis.config." + name);
        return property.getInt(defaultValue);
    }

    private static int damageIntProperty(
        Configuration config,
        String name,
        int defaultValue,
        int minimum,
        int maximum,
        String comment
    ) {
        Property property = config.get(CATEGORY_DAMAGE, name, defaultValue, comment, minimum, maximum);
        property.setLanguageKey("renewedtardis.config.damage." + name);
        return property.getInt(defaultValue);
    }

    private static int upgradeIntProperty(
        Configuration config,
        String name,
        int defaultValue,
        int minimum,
        int maximum,
        String comment
    ) {
        Property property = config.get(CATEGORY_UPGRADES, name, defaultValue, comment, minimum, maximum);
        property.setLanguageKey("renewedtardis.config.upgrades." + name);
        return property.getInt(defaultValue);
    }

    private static void validateInteriorDimensionId(int dimensionId) {
        if (dimensionId == -1 || dimensionId == 0 || dimensionId == 1) {
            throw new IllegalArgumentException(
                "TARDIS interior dimension ID cannot use vanilla dimension ID " + dimensionId
            );
        }
    }

    public static int getInteriorDimensionId() {
        return interiorDimensionId;
    }

    public static int getMinimumFlightSeconds() {
        return minimumFlightSeconds;
    }

    public static int getMaximumFlightSeconds() {
        return maximumFlightSeconds;
    }

    public static int getBlocksPerExtraSecond() {
        return blocksPerExtraSecond;
    }

    public static int getCrossDimensionPenaltySeconds() {
        return crossDimensionPenaltySeconds;
    }

    public static int getSafeLandingSearchRadius() {
        return safeLandingSearchRadius;
    }

    public static boolean isCrossDimensionFlightAllowed() {
        return allowCrossDimensionFlight;
    }

    public static boolean isDestinationDimensionBlocked(int dimension) {
        for (int blocked : blockedDestinationDimensions) {
            if (blocked == dimension) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFlightRouteAllowed(int departureDimension, int destinationDimension) {
        if (isDestinationDimensionBlocked(destinationDimension)) {
            return false;
        }
        return allowCrossDimensionFlight || departureDimension == destinationDimension;
    }

    public static String describeFlightRestriction(int departureDimension, int destinationDimension) {
        if (isDestinationDimensionBlocked(destinationDimension)) {
            return "Dimension " + destinationDimension + " is blocked by the TARDIS flight config.";
        }
        if (!allowCrossDimensionFlight && departureDimension != destinationDimension) {
            return "Cross-dimensional TARDIS flight is disabled by the server config.";
        }
        return "";
    }

    public static int getDefaultArtronCapacity() {
        return defaultArtronCapacity;
    }

    public static int getStartingArtronEnergy() {
        return Math.min(startingArtronEnergy, defaultArtronCapacity);
    }

    public static int getArtronRegenerationPerSecond() {
        return artronRegenerationPerSecond;
    }

    public static int getBaseFlightEnergyCost() {
        return baseFlightEnergyCost;
    }

    public static int getBlocksPerExtraFlightEnergy() {
        return blocksPerExtraFlightEnergy;
    }

    public static int getCrossDimensionFlightEnergyCost() {
        return crossDimensionFlightEnergyCost;
    }

    public static int getBasicRoomEnergyCost() {
        return basicRoomEnergyCost;
    }

    public static int getWorkshopRoomEnergyCost() {
        return workshopRoomEnergyCost;
    }

    public static int getStorageRoomEnergyCost() {
        return storageRoomEnergyCost;
    }

    public static boolean isDamageSystemEnabled() {
        return damageSystemEnabled;
    }

    public static int getBaseSubsystemWearPerFlight() {
        return baseSubsystemWearPerFlight;
    }

    public static int getFastFlightExtraWear() {
        return fastFlightExtraWear;
    }

    public static int getRiskyFlightExtraWear() {
        return riskyFlightExtraWear;
    }

    public static int getCrossDimensionExtraWear() {
        return crossDimensionExtraWear;
    }

    public static int getCriticalSubsystemHealth() {
        return criticalSubsystemHealth;
    }

    public static int getRepairPerChronosteel() {
        return repairPerChronosteel;
    }

    public static int getEngineEnergyPenaltyAtZeroHealthPercent() {
        return engineEnergyPenaltyAtZeroHealthPercent;
    }

    public static int getStabilizerDurationPenaltyAtZeroHealthPercent() {
        return stabilizerDurationPenaltyAtZeroHealthPercent;
    }

    public static int getMaximumDamageDriftRadius() {
        return maximumDamageDriftRadius;
    }

    public static int getUpgradeSlotCount() {
        return upgradeSlotCount;
    }

    public static int getMaximumSpeedUpgrades() {
        return maximumSpeedUpgrades;
    }

    public static int getMaximumCapacityUpgrades() {
        return maximumCapacityUpgrades;
    }

    public static int getMaximumRegenerationUpgrades() {
        return maximumRegenerationUpgrades;
    }

    public static int getMaximumRoomUpgrades() {
        return maximumRoomUpgrades;
    }

    public static int getMaximumProtectionUpgrades() {
        return maximumProtectionUpgrades;
    }

    public static int getSpeedDurationReductionPercentPerUpgrade() {
        return speedDurationReductionPercentPerUpgrade;
    }

    public static int getCapacityBonusPerUpgrade() {
        return capacityBonusPerUpgrade;
    }

    public static int getRegenerationBonusPerUpgrade() {
        return regenerationBonusPerUpgrade;
    }

    public static int getBaseExpandableRoomLimit() {
        return baseExpandableRoomLimit;
    }

    public static int getRoomLimitBonusPerUpgrade() {
        return roomLimitBonusPerUpgrade;
    }

    public static int getRoomCostReductionPercentPerUpgrade() {
        return roomCostReductionPercentPerUpgrade;
    }

    public static int getProtectionWearReductionPercentPerUpgrade() {
        return protectionWearReductionPercentPerUpgrade;
    }

    public static boolean isHwylaIntegrationEnabled() {
        return hwylaIntegrationEnabled;
    }

    public static boolean isComputerCraftIntegrationEnabled() {
        return computerCraftIntegrationEnabled;
    }

    public static int getMaximumDoctorMessages() {
        return maximumDoctorMessages;
    }

    public static boolean isSuccessfulRepairLoggingEnabled() {
        return successfulRepairLoggingEnabled;
    }
}
