package dev.zoldmaki.renewedtardis.block;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.console.ConsoleControlId;
import dev.zoldmaki.renewedtardis.console.ConsoleHitRegionRegistry;
import dev.zoldmaki.renewedtardis.console.ConsolePanel;
import dev.zoldmaki.renewedtardis.console.TardisConsoleInteractionService;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.interaction.TardisInteractionGuard;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisConsolePanel;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Generated placeholder panel block. Its physical hit map proves interaction routing
 * before the final Blockbench console and model-specific hit boxes exist.
 */
public final class BlockTardisConsolePanel extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    private static final AxisAlignedBB PANEL_BOX = new AxisAlignedBB(
        0.0D,
        0.0D,
        0.0D,
        1.0D,
        0.75D,
        1.0D
    );

    public BlockTardisConsolePanel() {
        super(Material.IRON);
        setRegistryName(RenewedTardisMod.MOD_ID, "tardis_console_panel");
        setTranslationKey(RenewedTardisMod.MOD_ID + ".tardis_console_panel");
        setBlockUnbreakable();
        setResistance(6000000.0F);
        setSoundType(SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.SOUTH));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityTardisConsolePanel();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return PANEL_BOX;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public IBlockState getStateForPlacement(
        World world,
        BlockPos pos,
        EnumFacing facing,
        float hitX,
        float hitY,
        float hitZ,
        int meta,
        EntityLivingBase placer
    ) {
        EnumFacing horizontal = placer == null ? EnumFacing.SOUTH : placer.getHorizontalFacing().getOpposite();
        return getDefaultState().withProperty(FACING, horizontal);
    }

    @Override
    public boolean onBlockActivated(
        World world,
        BlockPos pos,
        IBlockState state,
        EntityPlayer player,
        EnumHand hand,
        EnumFacing clickedFace,
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

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityTardisConsolePanel)) {
            send(player, TextFormatting.RED, "This console panel has no link tile.");
            return true;
        }
        UUID tardisId = ((TileEntityTardisConsolePanel) tile).getTardisId();
        if (tardisId == null) {
            send(player, TextFormatting.RED, "This console panel has lost its TARDIS link.");
            return true;
        }

        TardisWorldData data = TardisWorldData.get(world.getMinecraftServer());
        TardisRecord record = data.getById(tardisId);
        if (record == null) {
            send(player, TextFormatting.RED, "This console panel belongs to a missing TARDIS record.");
            return true;
        }

        ConsolePanel panel = ConsolePanel.fromFacing(state.getValue(FACING));
        if (panel == null) {
            send(player, TextFormatting.RED, "This console panel has an invalid orientation.");
            return true;
        }
        ConsoleControlId control = ConsoleHitRegionRegistry.resolve(panel, hitX, hitY, hitZ);
        if (control == null) {
            send(player, TextFormatting.YELLOW, "No placeholder control occupies that part of the panel.");
            return true;
        }
        if (!TardisInteractionGuard.tryBegin(player, world, pos, control.ordinal() + 1)) {
            return true;
        }

        return TardisConsoleInteractionService.interact(
            (EntityPlayerMP) player,
            data,
            record,
            panel,
            control,
            pos,
            hand
        );
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

    private static void send(EntityPlayer player, TextFormatting color, String text) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TARDIS] " + TextFormatting.RESET + text),
            false
        );
    }
}
