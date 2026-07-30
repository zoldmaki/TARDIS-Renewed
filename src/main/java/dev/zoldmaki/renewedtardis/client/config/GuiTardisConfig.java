package dev.zoldmaki.renewedtardis.client.config;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

/** Forge-native configuration screen shown from the Mods menu. */
public final class GuiTardisConfig extends GuiConfig {
    public GuiTardisConfig(GuiScreen parentScreen) {
        super(
            parentScreen,
            getElements(),
            RenewedTardisMod.MOD_ID,
            RenewedTardisMod.MOD_ID,
            false,
            false,
            RenewedTardisMod.NAME + " Configuration"
        );
        this.titleLine2 = GuiConfig.getAbridgedConfigPath(TardisConfig.getConfigPath());
    }

    private static List<IConfigElement> getElements() {
        Configuration configuration = TardisConfig.getConfiguration();
        List<IConfigElement> elements = new ArrayList<>();
        if (configuration == null) {
            return elements;
        }
        elements.add(new ConfigElement(configuration.getCategory(TardisConfig.CATEGORY_DIMENSIONS)));
        elements.add(new ConfigElement(configuration.getCategory(TardisConfig.CATEGORY_FLIGHT)));
        elements.add(new ConfigElement(configuration.getCategory(TardisConfig.CATEGORY_ENERGY)));
        elements.add(new ConfigElement(configuration.getCategory(TardisConfig.CATEGORY_DAMAGE)));
        elements.add(new ConfigElement(configuration.getCategory(TardisConfig.CATEGORY_UPGRADES)));
        elements.add(new ConfigElement(configuration.getCategory(TardisConfig.CATEGORY_INTEGRATIONS)));
        elements.add(new ConfigElement(configuration.getCategory(TardisConfig.CATEGORY_MAINTENANCE)));
        return elements;
    }
}
