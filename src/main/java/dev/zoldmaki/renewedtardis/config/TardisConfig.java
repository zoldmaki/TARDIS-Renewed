package dev.zoldmaki.renewedtardis.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/** Loads startup settings for dimensions and the prototype flight system. */
public final class TardisConfig {
    public static final int DEFAULT_INTERIOR_DIMENSION_ID = 1212;
    public static final int DEFAULT_MINIMUM_FLIGHT_SECONDS = 8;
    public static final int DEFAULT_MAXIMUM_FLIGHT_SECONDS = 60;
    public static final int DEFAULT_BLOCKS_PER_EXTRA_SECOND = 256;
    public static final int DEFAULT_CROSS_DIMENSION_PENALTY_SECONDS = 10;
    public static final int DEFAULT_SAFE_LANDING_SEARCH_RADIUS = 16;

    private static int interiorDimensionId = DEFAULT_INTERIOR_DIMENSION_ID;
    private static int minimumFlightSeconds = DEFAULT_MINIMUM_FLIGHT_SECONDS;
    private static int maximumFlightSeconds = DEFAULT_MAXIMUM_FLIGHT_SECONDS;
    private static int blocksPerExtraSecond = DEFAULT_BLOCKS_PER_EXTRA_SECOND;
    private static int crossDimensionPenaltySeconds = DEFAULT_CROSS_DIMENSION_PENALTY_SECONDS;
    private static int safeLandingSearchRadius = DEFAULT_SAFE_LANDING_SEARCH_RADIUS;

    private TardisConfig() {
    }

    public static void load(File file) {
        Configuration configuration = new Configuration(file);
        configuration.load();

        String dimensionCategory = "dimensions";
        configuration.setCategoryComment(
            dimensionCategory,
            "Dimension registration settings. These values are read only during game startup."
        );

        Property dimensionProperty = configuration.get(
            dimensionCategory,
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

        interiorDimensionId = dimensionProperty.getInt(DEFAULT_INTERIOR_DIMENSION_ID);
        if (interiorDimensionId == -1 || interiorDimensionId == 0 || interiorDimensionId == 1) {
            throw new IllegalArgumentException(
                "TARDIS interior dimension ID cannot use vanilla dimension ID " + interiorDimensionId
            );
        }

        String flightCategory = "flight";
        configuration.setCategoryComment(
            flightCategory,
            "Prototype flight timing and recovery settings. Times use real elapsed seconds and survive restarts."
        );

        minimumFlightSeconds = configuration.getInt(
            "minimumFlightSeconds",
            flightCategory,
            DEFAULT_MINIMUM_FLIGHT_SECONDS,
            0,
            3600,
            "Shortest normal flight duration in seconds."
        );
        maximumFlightSeconds = configuration.getInt(
            "maximumFlightSeconds",
            flightCategory,
            DEFAULT_MAXIMUM_FLIGHT_SECONDS,
            0,
            3600,
            "Longest normal flight duration in seconds. Must not be below minimumFlightSeconds."
        );
        blocksPerExtraSecond = configuration.getInt(
            "blocksPerExtraSecond",
            flightCategory,
            DEFAULT_BLOCKS_PER_EXTRA_SECOND,
            1,
            1000000,
            "Horizontal blocks of same-dimension travel that add one second to the minimum flight time."
        );
        crossDimensionPenaltySeconds = configuration.getInt(
            "crossDimensionPenaltySeconds",
            flightCategory,
            DEFAULT_CROSS_DIMENSION_PENALTY_SECONDS,
            0,
            3600,
            "Additional seconds added when departure and destination are in different dimensions."
        );
        safeLandingSearchRadius = configuration.getInt(
            "safeLandingSearchRadius",
            flightCategory,
            DEFAULT_SAFE_LANDING_SEARCH_RADIUS,
            1,
            64,
            "Maximum horizontal radius used by /tr land nearest and /tr land departure."
        );

        if (maximumFlightSeconds < minimumFlightSeconds) {
            maximumFlightSeconds = minimumFlightSeconds;
            configuration.get(flightCategory, "maximumFlightSeconds", DEFAULT_MAXIMUM_FLIGHT_SECONDS)
                .set(maximumFlightSeconds);
        }

        if (configuration.hasChanged()) {
            configuration.save();
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
}
