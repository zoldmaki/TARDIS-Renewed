package dev.zoldmaki.renewedtardis.client.config;

import java.util.Collections;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

/** Enables the Config button for TARDIS Renewed in Forge's Mods menu. */
public final class TardisConfigGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraftInstance) {
        // No client state is required.
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiTardisConfig(parentScreen);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set runtimeGuiCategories() {
        return Collections.emptySet();
    }
}
