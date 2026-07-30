package dev.zoldmaki.renewedtardis.integration;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.integration.computercraft.ComputerCraftIntegration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInterModComms;

/** Optional integration bootstrap which never hard-requires another mod. */
public final class IntegrationManager {
    private static boolean hwylaImcSent;

    private IntegrationManager() {
    }

    /** Startup-time hooks which must be sent before post-initialization. */
    public static void initializeStartup() {
        initializeHwyla();
    }

    /** Late hooks which require the optional target mod to have initialized. */
    public static void initializeLate() {
        ComputerCraftIntegration.registerIfAvailable();
    }

    private static void initializeHwyla() {
        if (!TardisConfig.isHwylaIntegrationEnabled() || !isHwylaInstalled()) {
            return;
        }
        try {
            hwylaImcSent = FMLInterModComms.sendMessage(
                "waila",
                "register",
                "dev.zoldmaki.renewedtardis.integration.hwyla.HwylaIntegration.register"
            );
            if (hwylaImcSent) {
                RenewedTardisMod.logger.info("Queued the optional HWYLA/Waila tooltip integration.");
            } else {
                RenewedTardisMod.logger.warn("HWYLA/Waila was detected, but its registration IMC message was rejected.");
            }
        } catch (RuntimeException exception) {
            hwylaImcSent = false;
            RenewedTardisMod.logger.error("Could not initialize the optional HWYLA/Waila integration.", exception);
        }
    }

    public static boolean isHwylaInstalled() {
        return Loader.isModLoaded("waila");
    }

    public static boolean isHwylaRegistrationQueued() {
        return hwylaImcSent;
    }

    public static boolean isComputerCraftInstalled() {
        return ComputerCraftIntegration.isInstalled();
    }

    public static boolean isComputerCraftRegistered() {
        return ComputerCraftIntegration.isRegistered();
    }

    public static String getComputerCraftError() {
        return ComputerCraftIntegration.getLastError();
    }

    public static String describeSide() {
        return FMLCommonHandler.instance().getEffectiveSide().isClient() ? "client" : "server";
    }
}
