package dev.zoldmaki.renewedtardis.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/** Loads startup-only settings that affect world structure and registration. */
public final class TardisConfig {
    public static final int DEFAULT_INTERIOR_DIMENSION_ID = 1212;

    private static int interiorDimensionId = DEFAULT_INTERIOR_DIMENSION_ID;

    private TardisConfig() {
    }

    public static void load(File file) {
        Configuration configuration = new Configuration(file);
        configuration.load();

        String category = "dimensions";
        configuration.setCategoryComment(
            category,
            "Dimension registration settings. These values are read only during game startup."
        );

        Property dimensionProperty = configuration.get(
            category,
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

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static int getInteriorDimensionId() {
        return interiorDimensionId;
    }
}
