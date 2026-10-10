package dev.zoldmaki.renewedtardis;

import dev.zoldmaki.renewedtardis.command.CommandTardisRenewed;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.dimension.TardisDimensionManager;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.init.ModItems;
import dev.zoldmaki.renewedtardis.integration.IntegrationManager;
import dev.zoldmaki.renewedtardis.network.ModNetwork;
import dev.zoldmaki.renewedtardis.sonic.SonicBuiltInActions;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisConsoleControl;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisConsolePanel;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisEngineCore;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisExterior;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisFlightConsole;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisInteriorDoor;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisTransmatPad;
import dev.zoldmaki.renewedtardis.tile.TileEntityGravityLift;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = RenewedTardisMod.MOD_ID,
    name = RenewedTardisMod.NAME,
    version = RenewedTardisMod.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:forge@[14.23.5.2847,)",
    useMetadata = true,
    guiFactory = "dev.zoldmaki.renewedtardis.client.config.TardisConfigGuiFactory"
)
public final class RenewedTardisMod {
    public static final String MOD_ID = "renewedtardis";
    public static final String NAME = "TARDIS Renewed";
    public static final String VERSION = "0.22.6-alpha";

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        TardisConfig.load(event.getSuggestedConfigurationFile());
        TardisDimensionManager.register(TardisConfig.getInteriorDimensionId());
        ModNetwork.registerMessages();
        SonicBuiltInActions.register();
        GameRegistry.registerTileEntity(
            TileEntityTardisExterior.class,
            new ResourceLocation(MOD_ID, "tardis_exterior")
        );
        GameRegistry.registerTileEntity(
            TileEntityTardisInteriorDoor.class,
            new ResourceLocation(MOD_ID, "tardis_interior_door")
        );
        GameRegistry.registerTileEntity(
            TileEntityTardisFlightConsole.class,
            new ResourceLocation(MOD_ID, "tardis_flight_console")
        );
        GameRegistry.registerTileEntity(
            TileEntityTardisConsoleControl.class,
            new ResourceLocation(MOD_ID, "tardis_console_control")
        );
        GameRegistry.registerTileEntity(
            TileEntityTardisConsolePanel.class,
            new ResourceLocation(MOD_ID, "tardis_console_panel")
        );

        GameRegistry.registerTileEntity(
            TileEntityTardisEngineCore.class,
            new ResourceLocation(MOD_ID, "tardis_engine_core")
        );
        GameRegistry.registerTileEntity(
            TileEntityTardisTransmatPad.class,
            new ResourceLocation(MOD_ID, "tardis_transmat_pad")
        );
        GameRegistry.registerTileEntity(
            TileEntityGravityLift.class,
            new ResourceLocation(MOD_ID, "gravity_lift")
        );

        logger.info(
            "TARDIS Renewed Phase 21 pre-initialization started with interior dimension {}.",
            TardisDimensionManager.getDimensionId()
        );
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        OreDictionary.registerOre("ingotChronosteel", ModItems.CHRONOSTEEL_INGOT);
        OreDictionary.registerOre("blockChronosteel", ModBlocks.CHRONOSTEEL_BLOCK);

        IntegrationManager.initializeStartup();
        logger.info("TARDIS Renewed Phase 21 initialization completed: physical console state, registries, and placeholder panel routing are available.");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        IntegrationManager.initializeLate();
        logger.info("TARDIS Renewed Phase 21 optional integration initialization completed.");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandTardisRenewed());
    }
}
