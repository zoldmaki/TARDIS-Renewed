package dev.zoldmaki.renewedtardis.block;

import dev.zoldmaki.renewedtardis.RenewedTardisCreativeTab;
import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.flight.TardisFlightService;
import dev.zoldmaki.renewedtardis.interaction.TardisInteractionGuard;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisFlightConsole;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/** Temporary functional control block for validating the flight state machine. */
public final class BlockTardisFlightConsole extends Block {
    public BlockTardisFlightConsole() {
        super(Material.IRON);
        setRegistryName(RenewedTardisMod.MOD_ID, "tardis_flight_console");
        setTranslationKey(RenewedTardisMod.MOD_ID + ".tardis_flight_console");
        setCreativeTab(RenewedTardisCreativeTab.INSTANCE);
        setBlockUnbreakable();
        setResistance(6000000.0F);
        setSoundType(SoundType.METAL);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityTardisFlightConsole();
    }

    @Override
    public boolean onBlockActivated(
        World world,
        BlockPos pos,
        IBlockState state,
        EntityPlayer player,
        EnumHand hand,
        EnumFacing facing,
        float hitX,
        float hitY,
        float hitZ
    ) {
        if (world.isRemote) {
            return true;
        }
        if (!TardisInteractionGuard.tryBegin(player, world, pos)) {
            return true;
        }
        if (!(player instanceof EntityPlayerMP) || world.getMinecraftServer() == null) {
            return false;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityTardisFlightConsole)) {
            return false;
        }

        UUID tardisId = ((TileEntityTardisFlightConsole) tile).getTardisId();
        if (tardisId == null) {
            send(player, TextFormatting.RED, "This console has lost its TARDIS link.");
            return true;
        }

        TardisWorldData data = TardisWorldData.get(world.getMinecraftServer());
        TardisRecord record = data.getById(tardisId);
        if (record == null) {
            send(player, TextFormatting.RED, "This console belongs to a missing TARDIS record.");
            return true;
        }

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        if (
            !record.getOwnerId().equals(serverPlayer.getUniqueID())
                && !serverPlayer.canUseCommand(2, "tardisrenewed")
        ) {
            send(player, TextFormatting.RED, "Only this TARDIS's owner can use the prototype flight control.");
            return true;
        }

        if (player.isSneaking()) {
            showStatus(player, record);
            return true;
        }

        if (record.isInFlight()) {
            TardisFlightService.land(serverPlayer, record, pos);
        } else {
            TardisFlightService.takeOff(serverPlayer, record, pos);
        }
        return true;
    }

    private static void showStatus(EntityPlayer player, TardisRecord record) {
        DimensionPosition destination = record.getDestinationPosition();
        send(
            player,
            TextFormatting.AQUA,
            "Flight state: " + (record.isInFlight() ? "IN FLIGHT" : "LANDED")
                + "; destination: " + (destination == null ? "not set" : destination.toString()) + "."
        );
    }

    private static void send(EntityPlayer player, TextFormatting color, String text) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TARDIS] " + TextFormatting.RESET + text),
            false
        );
    }
}
