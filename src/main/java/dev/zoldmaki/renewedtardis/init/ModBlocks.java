package dev.zoldmaki.renewedtardis.init;

import dev.zoldmaki.renewedtardis.block.BlockChronosteel;
import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.block.BlockTardisFlightConsole;
import dev.zoldmaki.renewedtardis.block.BlockTardisInteriorDoor;

public final class ModBlocks {
    public static final BlockChronosteel CHRONOSTEEL_BLOCK = new BlockChronosteel();
    public static final BlockTardisExterior TARDIS_EXTERIOR = new BlockTardisExterior();
    public static final BlockTardisInteriorDoor TARDIS_INTERIOR_DOOR = new BlockTardisInteriorDoor();
    public static final BlockTardisFlightConsole TARDIS_FLIGHT_CONSOLE = new BlockTardisFlightConsole();

    private ModBlocks() {
    }
}
