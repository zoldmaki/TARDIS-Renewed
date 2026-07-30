package dev.zoldmaki.renewedtardis.block;

import dev.zoldmaki.renewedtardis.RenewedTardisCreativeTab;
import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.access.TardisAccessService;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.dimension.InteriorAddress;
import dev.zoldmaki.renewedtardis.item.ItemTardisKey;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisTransmatPad;
import dev.zoldmaki.renewedtardis.transmat.TardisTransmatService;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/** Active interior endpoint for the legacy-style TARDIS transmat. */
public final class BlockTardisTransmatPad extends Block {
    public BlockTardisTransmatPad() {
        super(Material.IRON);
        setRegistryName(RenewedTardisMod.MOD_ID, "tardis_transmat_pad");
        setTranslationKey(RenewedTardisMod.MOD_ID + ".tardis_transmat_pad");
        setCreativeTab(RenewedTardisCreativeTab.INSTANCE);
        setHardness(3.0F);
        setResistance(12.0F);
        setSoundType(SoundType.METAL);
        setLightLevel(0.45F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityTardisTransmatPad();
    }

    @Override
    public void onBlockPlacedBy(
        World world,
        BlockPos pos,
        IBlockState state,
        EntityLivingBase placer,
        ItemStack stack
    ) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (world.isRemote || !(placer instanceof EntityPlayerMP) || world.getMinecraftServer() == null) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) placer;
        TardisWorldData data = TardisWorldData.get(world.getMinecraftServer());
        TardisRecord owned = data.getByOwner(player.getUniqueID());
        if (owned != null && isInsideRecordCell(world, pos, owned)) {
            bindPad(world, pos, data, owned);
            send(player, TextFormatting.GREEN, "This pad is now the active interior transmat point.");
        }
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
        if (!(player instanceof EntityPlayerMP) || world.getMinecraftServer() == null) {
            return false;
        }

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        TardisWorldData data = TardisWorldData.get(world.getMinecraftServer());
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty() && held.getItem() instanceof ItemTardisKey) {
            UUID keyId = ItemTardisKey.getTardisId(held);
            if (keyId == null) {
                send(serverPlayer, TextFormatting.YELLOW, "Bind this TARDIS key before configuring the pad.");
                return true;
            }
            TardisRecord keyRecord = data.getById(keyId);
            if (keyRecord == null) {
                send(serverPlayer, TextFormatting.RED, "That key points to a missing TARDIS record.");
                return true;
            }
            if (!TardisAccessService.canMaintain(player, keyRecord)
                && !TardisAccessService.canUseConsole(player, keyRecord)) {
                send(serverPlayer, TextFormatting.RED,
                    "Configuring a transmat pad requires maintenance or console permission.");
                return true;
            }
            if (!isInsideRecordCell(world, pos, keyRecord)) {
                send(serverPlayer, TextFormatting.RED,
                    "A TARDIS transmat point must be placed inside that TARDIS's own interior cell.");
                return true;
            }
            bindPad(world, pos, data, keyRecord);
            send(serverPlayer, TextFormatting.GREEN,
                "This pad is now the active transmat point for " + keyRecord.getOwnerName() + "'s TARDIS.");
            return true;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityTardisTransmatPad)) {
            send(serverPlayer, TextFormatting.RED, "This transmat pad has no link tile entity.");
            return true;
        }
        UUID tardisId = ((TileEntityTardisTransmatPad) tile).getTardisId();
        if (tardisId == null) {
            TardisRecord owned = data.getByOwner(serverPlayer.getUniqueID());
            if (owned != null && isInsideRecordCell(world, pos, owned)) {
                bindPad(world, pos, data, owned);
                tardisId = owned.getTardisId();
            } else {
                send(serverPlayer, TextFormatting.YELLOW, "Use a bound TARDIS key to configure this pad.");
                return true;
            }
        }

        TardisRecord record = data.getById(tardisId);
        if (record == null) {
            send(serverPlayer, TextFormatting.RED, "This pad points to a missing TARDIS record.");
            return true;
        }
        DimensionPosition active = record.getTransmatPosition();
        if (active == null
            || active.getDimension() != world.provider.getDimension()
            || !active.getPosition().equals(pos)) {
            send(serverPlayer, TextFormatting.YELLOW,
                "This is an inactive pad. Use a bound key to make it the active transmat point.");
            return true;
        }
        TardisTransmatService.transmat(serverPlayer, record);
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && world.getMinecraftServer() != null) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityTardisTransmatPad) {
                UUID tardisId = ((TileEntityTardisTransmatPad) tile).getTardisId();
                if (tardisId != null) {
                    TardisWorldData data = TardisWorldData.get(world.getMinecraftServer());
                    TardisRecord record = data.getById(tardisId);
                    DimensionPosition active = record == null ? null : record.getTransmatPosition();
                    if (active != null
                        && active.getDimension() == world.provider.getDimension()
                        && active.getPosition().equals(pos)) {
                        data.setTransmatPosition(tardisId, null);
                    }
                }
            }
        }
        super.breakBlock(world, pos, state);
    }

    public static void bindPad(World world, BlockPos pos, TardisWorldData data, TardisRecord record) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityTardisTransmatPad)) {
            throw new IllegalStateException("The TARDIS transmat pad has no tile entity");
        }
        ((TileEntityTardisTransmatPad) tile).bind(record.getTardisId());
        data.setTransmatPosition(
            record.getTardisId(),
            new DimensionPosition(world.provider.getDimension(), pos)
        );
    }

    private static boolean isInsideRecordCell(World world, BlockPos pos, TardisRecord record) {
        DimensionPosition interior = record.getInteriorPosition();
        if (interior == null || interior.getDimension() != world.provider.getDimension()) {
            return false;
        }
        BlockPos center = interior.getPosition();
        int halfSpacing = InteriorAddress.CELL_SPACING / 2;
        return Math.abs(pos.getX() - center.getX()) < halfSpacing
            && Math.abs(pos.getZ() - center.getZ()) < halfSpacing;
    }

    private static void send(EntityPlayerMP player, TextFormatting color, String text) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TRANSMAT] " + TextFormatting.RESET + text),
            false
        );
    }
}
