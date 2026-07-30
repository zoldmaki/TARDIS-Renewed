package dev.zoldmaki.renewedtardis.sonic;

import dev.zoldmaki.renewedtardis.access.TardisAccessService;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.item.ItemSonicScrewdriver;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisConsoleControl;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisEngineCore;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisExterior;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisFlightConsole;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisInteriorDoor;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisTransmatPad;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

/** Registers the built-in Phase 17 sonic actions. */
public final class SonicBuiltInActions {
    private static boolean registered;

    private SonicBuiltInActions() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        SonicActionRegistry.registerBlockAction(new LinkAction());
        SonicActionRegistry.registerBlockAction(new ReconfigureTardisDoorAction());
        SonicActionRegistry.registerBlockAction(new SchematicBlockAction());
        SonicActionRegistry.registerEntityAction(new SchematicEntityAction());
    }

    private static final class LinkAction implements ISonicBlockAction {
        @Override
        public SonicActionResult execute(SonicBlockContext context) {
            if (context.getMode() != SonicMode.LINK) {
                return SonicActionResult.PASS;
            }

            UUID tardisId = resolveTardisId(context.getWorld(), context.getPosition());
            if (tardisId == null) {
                return SonicActionResult.PASS;
            }
            TardisRecord record = TardisWorldData.get(context.getPlayer().getServer()).getById(tardisId);
            if (record == null) {
                send(context.getPlayer(), TextFormatting.RED, "That block points to a missing TARDIS record.");
                return SonicActionResult.FAIL;
            }
            if (!TardisAccessService.canUseSonic(context.getPlayer(), record)) {
                send(context.getPlayer(), TextFormatting.RED,
                    "You need key, console, or maintenance permission to link a sonic to this TARDIS.");
                return SonicActionResult.FAIL;
            }

            ItemSonicScrewdriver.bind(context.getStack(), record);
            send(context.getPlayer(), TextFormatting.GREEN,
                "Linked to " + record.getOwnerName() + "'s TARDIS (" + record.getTardisId() + ").");
            return SonicActionResult.SUCCESS;
        }
    }

    private static final class ReconfigureTardisDoorAction implements ISonicBlockAction {
        @Override
        public SonicActionResult execute(SonicBlockContext context) {
            if (context.getMode() != SonicMode.RECONFIGURE || !isTardisDoorBlock(context.getState().getBlock())) {
                return SonicActionResult.PASS;
            }

            UUID clickedId = resolveTardisId(context.getWorld(), context.getPosition());
            if (clickedId == null) {
                send(context.getPlayer(), TextFormatting.RED, "That TARDIS door is unbound.");
                return SonicActionResult.FAIL;
            }
            UUID linkedId = ItemSonicScrewdriver.getTardisId(context.getStack());
            if (linkedId == null) {
                send(context.getPlayer(), TextFormatting.YELLOW, "Link this sonic before reconfiguring a TARDIS door.");
                return SonicActionResult.FAIL;
            }
            if (!linkedId.equals(clickedId)) {
                send(context.getPlayer(), TextFormatting.RED, "This sonic is linked to a different TARDIS.");
                return SonicActionResult.FAIL;
            }

            TardisWorldData data = TardisWorldData.get(context.getPlayer().getServer());
            TardisRecord record = data.getById(linkedId);
            if (record == null) {
                send(context.getPlayer(), TextFormatting.RED, "The linked TARDIS record is missing.");
                return SonicActionResult.FAIL;
            }
            if (!TardisAccessService.canUseKey(context.getPlayer(), record)) {
                send(context.getPlayer(), TextFormatting.RED, "Reconfiguring the door lock requires key permission.");
                return SonicActionResult.FAIL;
            }

            boolean locked = !record.isExteriorLocked();
            data.setExteriorLocked(record.getTardisId(), locked);
            synchronizeExteriorTile(context.getPlayer(), record, locked);
            ItemSonicScrewdriver.bind(context.getStack(), record);
            send(context.getPlayer(), locked ? TextFormatting.YELLOW : TextFormatting.GREEN,
                "The linked TARDIS is now " + (locked ? "locked" : "unlocked") + ".");
            return SonicActionResult.SUCCESS;
        }
    }

    private static final class SchematicBlockAction implements ISonicBlockAction {
        @Override
        public SonicActionResult execute(SonicBlockContext context) {
            if (context.getMode() != SonicMode.SCHEMATIC) {
                return SonicActionResult.PASS;
            }

            IBlockState state = context.getState();
            Block block = state.getBlock();
            ResourceLocation id = block.getRegistryName();
            TileEntity tile = context.getWorld().getTileEntity(context.getPosition());
            String tileText = tile == null ? "none" : tile.getClass().getSimpleName();
            float hardness = state.getBlockHardness(context.getWorld(), context.getPosition());
            String harvestTool = block.getHarvestTool(state);
            int harvestLevel = block.getHarvestLevel(state);

            send(context.getPlayer(), TextFormatting.AQUA,
                "Block: " + (id == null ? block.getClass().getName() : id.toString())
                    + " at " + formatPosition(context.getWorld(), context.getPosition()) + ".");
            send(context.getPlayer(), TextFormatting.GRAY,
                "State: " + state.toString() + "; hardness=" + hardness
                    + "; harvest=" + (harvestTool == null ? "none" : harvestTool + " " + harvestLevel)
                    + "; tile=" + tileText + ".");
            return SonicActionResult.SUCCESS;
        }
    }

    private static final class SchematicEntityAction implements ISonicEntityAction {
        @Override
        public SonicActionResult execute(SonicEntityContext context) {
            if (context.getMode() != SonicMode.SCHEMATIC) {
                return SonicActionResult.PASS;
            }

            Entity target = context.getTarget();
            String health = "";
            if (target instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) target;
                health = "; health=" + Math.round(living.getHealth() * 10.0F) / 10.0F
                    + "/" + Math.round(living.getMaxHealth() * 10.0F) / 10.0F;
            }
            send(context.getPlayer(), TextFormatting.AQUA,
                "Entity: " + target.getName() + " [" + target.getClass().getName() + "]"
                    + health + "; UUID=" + target.getUniqueID() + ".");
            return SonicActionResult.SUCCESS;
        }
    }

    private static boolean isTardisDoorBlock(Block block) {
        return block == ModBlocks.TARDIS_EXTERIOR
            || block == ModBlocks.TARDIS_EXTERIOR_TOP
            || block == ModBlocks.TARDIS_INTERIOR_DOOR;
    }

    @Nullable
    private static UUID resolveTardisId(WorldServer world, BlockPos originalPos) {
        BlockPos pos = originalPos;
        if (world.getBlockState(pos).getBlock() == ModBlocks.TARDIS_EXTERIOR_TOP) {
            pos = pos.down();
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityTardisExterior) {
            return ((TileEntityTardisExterior) tile).getTardisId();
        }
        if (tile instanceof TileEntityTardisInteriorDoor) {
            return ((TileEntityTardisInteriorDoor) tile).getTardisId();
        }
        if (tile instanceof TileEntityTardisEngineCore) {
            return ((TileEntityTardisEngineCore) tile).getTardisId();
        }
        if (tile instanceof TileEntityTardisFlightConsole) {
            return ((TileEntityTardisFlightConsole) tile).getTardisId();
        }
        if (tile instanceof TileEntityTardisConsoleControl) {
            return ((TileEntityTardisConsoleControl) tile).getTardisId();
        }
        if (tile instanceof TileEntityTardisTransmatPad) {
            return ((TileEntityTardisTransmatPad) tile).getTardisId();
        }
        return null;
    }

    private static void synchronizeExteriorTile(EntityPlayerMP player, TardisRecord record, boolean locked) {
        DimensionPosition exterior = record.getExteriorPosition();
        if (exterior == null || player.getServer() == null) {
            return;
        }
        WorldServer world = player.getServer().getWorld(exterior.getDimension());
        if (world == null || !world.isBlockLoaded(exterior.getPosition())) {
            return;
        }
        TileEntity tile = world.getTileEntity(exterior.getPosition());
        if (tile instanceof TileEntityTardisExterior) {
            ((TileEntityTardisExterior) tile).setLocked(locked);
        }
    }

    private static String formatPosition(WorldServer world, BlockPos pos) {
        return "dim " + world.provider.getDimension() + " @ "
            + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static void send(EntityPlayerMP player, TextFormatting color, String message) {
        player.sendStatusMessage(
            new TextComponentString(color + "[SONIC] " + TextFormatting.RESET + message),
            false
        );
    }
}
