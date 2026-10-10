package dev.zoldmaki.renewedtardis.init;

import dev.zoldmaki.renewedtardis.block.BlockChronosteel;
import dev.zoldmaki.renewedtardis.block.BlockGravityLift;
import dev.zoldmaki.renewedtardis.block.BlockTardisConsoleControl;
import dev.zoldmaki.renewedtardis.block.BlockTardisConsolePanel;
import dev.zoldmaki.renewedtardis.block.BlockTardisExterior;
import dev.zoldmaki.renewedtardis.block.BlockTardisExteriorTop;
import dev.zoldmaki.renewedtardis.block.BlockTardisEngineCore;
import dev.zoldmaki.renewedtardis.block.BlockTardisFlightConsole;
import dev.zoldmaki.renewedtardis.block.BlockTardisInteriorDoor;
import dev.zoldmaki.renewedtardis.block.BlockTardisTransmatPad;
import net.minecraft.block.Block;

public final class ModBlocks {
    public static final BlockChronosteel CHRONOSTEEL_BLOCK = new BlockChronosteel();
    public static final BlockTardisExterior TARDIS_EXTERIOR = new BlockTardisExterior();
    public static final BlockTardisExteriorTop TARDIS_EXTERIOR_TOP = new BlockTardisExteriorTop();
    public static final BlockTardisInteriorDoor TARDIS_INTERIOR_DOOR = new BlockTardisInteriorDoor();
    public static final BlockTardisEngineCore TARDIS_ENGINE_CORE = new BlockTardisEngineCore();
    public static final BlockTardisTransmatPad TARDIS_TRANSMAT_PAD = new BlockTardisTransmatPad();
    public static final BlockGravityLift GRAVITY_LIFT = new BlockGravityLift();
    public static final BlockTardisConsolePanel TARDIS_CONSOLE_PANEL = new BlockTardisConsolePanel();

    /** Retained for save compatibility with Phase 6-8 prototype interiors. */
    public static final BlockTardisFlightConsole TARDIS_FLIGHT_CONSOLE = new BlockTardisFlightConsole();

    public static final BlockTardisConsoleControl TARDIS_TAKEOFF_CONTROL = new BlockTardisConsoleControl(
        "tardis_takeoff_control",
        BlockTardisConsoleControl.ControlType.TAKEOFF
    );
    public static final BlockTardisConsoleControl TARDIS_LANDING_CONTROL = new BlockTardisConsoleControl(
        "tardis_landing_control",
        BlockTardisConsoleControl.ControlType.LANDING
    );
    public static final BlockTardisConsoleControl TARDIS_DESTINATION_CONTROL = new BlockTardisConsoleControl(
        "tardis_destination_control",
        BlockTardisConsoleControl.ControlType.DESTINATION
    );
    public static final BlockTardisConsoleControl TARDIS_FACING_CONTROL = new BlockTardisConsoleControl(
        "tardis_facing_control",
        BlockTardisConsoleControl.ControlType.FACING
    );
    public static final BlockTardisConsoleControl TARDIS_STATUS_CONTROL = new BlockTardisConsoleControl(
        "tardis_status_control",
        BlockTardisConsoleControl.ControlType.STATUS
    );

    private ModBlocks() {
    }

    public static boolean isTardisInteractionBlock(Block block) {
        return block == TARDIS_EXTERIOR
            || block == TARDIS_EXTERIOR_TOP
            || block == TARDIS_INTERIOR_DOOR
            || block == TARDIS_FLIGHT_CONSOLE
            || block == TARDIS_CONSOLE_PANEL
            || block == TARDIS_TAKEOFF_CONTROL
            || block == TARDIS_LANDING_CONTROL
            || block == TARDIS_DESTINATION_CONTROL
            || block == TARDIS_FACING_CONTROL
            || block == TARDIS_STATUS_CONTROL
            || block == TARDIS_ENGINE_CORE;
    }
}
