package dev.zoldmaki.renewedtardis.block;

import dev.zoldmaki.renewedtardis.RenewedTardisCreativeTab;
import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.init.ModItems;
import dev.zoldmaki.renewedtardis.init.ModSounds;
import dev.zoldmaki.renewedtardis.item.ItemTardisKey;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisExterior;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * One-block exterior prototype used to validate persistent linking, tile
 * entity storage, ownership checks, and key locking before the final model and
 * interior teleportation are introduced.
 */
public final class BlockTardisExterior extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public BlockTardisExterior() {
        super(Material.IRON);
        setRegistryName(RenewedTardisMod.MOD_ID, "tardis_exterior");
        setTranslationKey(RenewedTardisMod.MOD_ID + ".tardis_exterior");
        setCreativeTab(RenewedTardisCreativeTab.INSTANCE);
        setHardness(3.0F);
        setResistance(20.0F);
        setSoundType(SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityTardisExterior();
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

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityTardisExterior)) {
            return false;
        }

        TileEntityTardisExterior exterior = (TileEntityTardisExterior) tile;
        ItemStack held = player.getHeldItem(hand);

        if (!held.isEmpty() && held.getItem() == ModItems.TARDIS_KEY && player instanceof EntityPlayerMP) {
            return handleKey(world, pos, (EntityPlayerMP) player, held, exterior);
        }

        if (!exterior.isBound()) {
            send(player, TextFormatting.YELLOW, "This exterior is unbound. Use a TARDIS Key on it.");
            return true;
        }

        if (exterior.isLocked()) {
            world.playSound(null, pos, ModSounds.LOCKED, SoundCategory.BLOCKS, 0.8F, 1.0F);
            send(player, TextFormatting.RED, "The TARDIS exterior is locked.");
            return true;
        }

        send(player, TextFormatting.AQUA, "The door opens, but the interior dimension is not connected yet.");
        return true;
    }

    private static boolean handleKey(
        World world,
        BlockPos pos,
        EntityPlayerMP player,
        ItemStack key,
        TileEntityTardisExterior exterior
    ) {
        if (world.getMinecraftServer() == null) {
            return false;
        }

        TardisWorldData data = TardisWorldData.get(world.getMinecraftServer());
        UUID keyTardisId = ItemTardisKey.getTardisId(key);
        TardisRecord record;

        if (keyTardisId == null) {
            record = data.getOrCreateForOwner(player.getUniqueID(), player.getName());
            ItemTardisKey.bind(key, record);
            keyTardisId = record.getTardisId();
        } else {
            record = data.getById(keyTardisId);
        }

        if (record == null) {
            send(player, TextFormatting.RED, "This key points to a missing TARDIS record.");
            return true;
        }

        if (!exterior.isBound()) {
            DimensionPosition existingPosition = record.getExteriorPosition();
            if (existingPosition != null && !matches(existingPosition, world, pos)) {
                send(
                    player,
                    TextFormatting.RED,
                    "That TARDIS already has an exterior. Use /tr exterior to relocate it safely."
                );
                return true;
            }

            exterior.bind(record);
            data.setExteriorPosition(
                record.getTardisId(),
                new DimensionPosition(world.provider.getDimension(), pos)
            );
            send(player, TextFormatting.GOLD, "The exterior binds to your TARDIS and locks.");
            return true;
        }

        if (!keyTardisId.equals(exterior.getTardisId())) {
            world.playSound(null, pos, ModSounds.LOCKED, SoundCategory.BLOCKS, 0.8F, 0.9F);
            send(player, TextFormatting.RED, "This key does not belong to that TARDIS.");
            return true;
        }

        exterior.setLocked(!exterior.isLocked());
        world.playSound(null, pos, ModSounds.LOCKED, SoundCategory.BLOCKS, 0.7F, exterior.isLocked() ? 0.9F : 1.2F);
        send(
            player,
            exterior.isLocked() ? TextFormatting.RED : TextFormatting.GREEN,
            exterior.isLocked() ? "TARDIS locked." : "TARDIS unlocked."
        );
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && world.getMinecraftServer() != null) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityTardisExterior) {
                UUID tardisId = ((TileEntityTardisExterior) tile).getTardisId();
                if (tardisId != null) {
                    TardisWorldData data = TardisWorldData.get(world.getMinecraftServer());
                    TardisRecord record = data.getById(tardisId);
                    if (record != null && matches(record.getExteriorPosition(), world, pos)) {
                        data.setExteriorPosition(tardisId, null);
                    }
                }
            }
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.byHorizontalIndex(meta & 3);
        return getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    private static boolean matches(DimensionPosition position, World world, BlockPos pos) {
        return position != null
            && position.getDimension() == world.provider.getDimension()
            && position.getPosition().equals(pos);
    }

    private static void send(EntityPlayer player, TextFormatting color, String text) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TARDIS] " + TextFormatting.RESET + text),
            false
        );
    }
}
