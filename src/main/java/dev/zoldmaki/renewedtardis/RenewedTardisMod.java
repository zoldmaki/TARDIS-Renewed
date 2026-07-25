package dev.zoldmaki.renewedtardis;

import dev.zoldmaki.renewedtardis.command.CommandTardisRenewed;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.init.ModItems;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
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
    public static final String VERSION = "0.3.0-alpha";

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("TARDIS Renewed Phase 2 pre-initialization started.");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        OreDictionary.registerOre("ingotChronosteel", ModItems.CHRONOSTEEL_INGOT);
        OreDictionary.registerOre("blockChronosteel", ModBlocks.CHRONOSTEEL_BLOCK);

        logger.info("TARDIS Renewed Phase 2 loaded: persistent TARDIS registry, TARDIS Key, and test commands.");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandTardisRenewed());
    }
}
