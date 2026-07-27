package dev.zoldmaki.renewedtardis.block;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.interaction.TardisInteractionGuard;
import dev.zoldmaki.renewedtardis.teleport.TardisTeleportService;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisInteriorDoor;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/** Prototype interior exit door generated inside each TARDIS room. */
public final class BlockTardisInteriorDoor extends Block {
    public BlockTardisInteriorDoor() {
        super(Material.IRON);
        setRegistryName(RenewedTardisMod.MOD_ID, "tardis_interior_door");
        setTranslationKey(RenewedTardisMod.MOD_ID + ".tardis_interior_door");
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
        return new TileEntityTardisInteriorDoor();
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
        if (!(tile instanceof TileEntityTardisInteriorDoor)) {
            return false;
        }

        UUID tardisId = ((TileEntityTardisInteriorDoor) tile).getTardisId();
        if (tardisId == null) {
            send(player, TextFormatting.RED, "This interior door has lost its TARDIS link.");
            return true;
        }

        TardisWorldData data = TardisWorldData.get(world.getMinecraftServer());
        TardisRecord record = data.getById(tardisId);
        if (record == null) {
            send(player, TextFormatting.RED, "This interior belongs to a missing TARDIS record.");
            return true;
        }

        if (TardisTeleportService.exitToExterior((EntityPlayerMP) player, record)) {
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 0.75F, 0.9F);
        }
        return true;
    }

    private static void send(EntityPlayer player, TextFormatting color, String text) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TARDIS] " + TextFormatting.RESET + text),
            false
        );
    }
}
