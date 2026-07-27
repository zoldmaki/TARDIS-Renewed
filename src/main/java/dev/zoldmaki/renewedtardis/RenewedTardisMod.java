package dev.zoldmaki.renewedtardis;

import dev.zoldmaki.renewedtardis.command.CommandTardisRenewed;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.dimension.TardisDimensionManager;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.init.ModItems;
import dev.zoldmaki.renewedtardis.network.ModNetwork;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisConsoleControl;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisExterior;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisFlightConsole;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisInteriorDoor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
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
    useMetadata = true
)
public final class RenewedTardisMod {
    public static final String MOD_ID = "renewedtardis";
    public static final String NAME = "TARDIS Renewed";
    public static final String VERSION = "0.11.0-alpha";

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        TardisConfig.load(event.getSuggestedConfigurationFile());
        TardisDimensionManager.register(TardisConfig.getInteriorDimensionId());
        ModNetwork.registerMessages();
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

        logger.info(
            "TARDIS Renewed Phase 10 pre-initialization started with interior dimension {}.",
            TardisDimensionManager.getDimensionId()
        );
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        OreDictionary.registerOre("ingotChronosteel", ModItems.CHRONOSTEEL_INGOT);
        OreDictionary.registerOre("blockChronosteel", ModBlocks.CHRONOSTEEL_BLOCK);

        logger.info("TARDIS Renewed Phase 10 loaded: two-block exterior shells, front-door interaction, and chameleon foundation.");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandTardisRenewed());
    }
}
