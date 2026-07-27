package dev.zoldmaki.renewedtardis.block;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.exterior.ExteriorShell;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Reserved upper half of the exterior. It delegates interaction and destruction
 * to the linked lower block, which owns the tile entity and persistent data.
 */
public final class BlockTardisExteriorTop extends Block {
    public static final PropertyDirection FACING = BlockTardisExterior.FACING;
    public static final PropertyEnum<ExteriorShell> SHELL = BlockTardisExterior.SHELL;

    private static final AxisAlignedBB BOUNDS = new AxisAlignedBB(
        1.0D / 16.0D,
        0.0D,
        1.0D / 16.0D,
        15.0D / 16.0D,
        1.0D,
        15.0D / 16.0D
    );

    public BlockTardisExteriorTop() {
        super(Material.IRON);
        setRegistryName(RenewedTardisMod.MOD_ID, "tardis_exterior_top");
        setTranslationKey(RenewedTardisMod.MOD_ID + ".tardis_exterior_top");
        setHardness(3.0F);
        setResistance(20.0F);
        setSoundType(SoundType.METAL);
        setLightOpacity(0);
        setDefaultState(
            blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(SHELL, ExteriorShell.POLICE_BOX)
        );
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
        BlockPos lowerPos = pos.down();
        IBlockState lowerState = world.getBlockState(lowerPos);
        if (lowerState.getBlock() != ModBlocks.TARDIS_EXTERIOR) {
            if (!world.isRemote) {
                world.setBlockToAir(pos);
            }
            return false;
        }

        return ModBlocks.TARDIS_EXTERIOR.onBlockActivated(
            world,
            lowerPos,
            lowerState,
            player,
            hand,
            facing,
            hitX,
            hitY + 1.0F,
            hitZ
        );
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && !BlockTardisExterior.isStructureChangeInProgress()) {
            BlockPos lowerPos = pos.down();
            BlockTardisExterior.clearSavedExteriorIfPresent(world, lowerPos);
            BlockTardisExterior.beginStructureChange();
            try {
                if (world.getBlockState(lowerPos).getBlock() == ModBlocks.TARDIS_EXTERIOR) {
                    world.setBlockToAir(lowerPos);
                }
            } finally {
                BlockTardisExterior.endStructureChange();
            }
        }
        super.breakBlock(world, pos, state);
    }


    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Item.getItemFromBlock(ModBlocks.TARDIS_EXTERIOR);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BOUNDS;
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
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.byHorizontalIndex(meta & 3);
        ExteriorShell shell = ExteriorShell.fromMetadata((meta >> 2) & 3);
        return getDefaultState().withProperty(FACING, facing).withProperty(SHELL, shell);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex()
            | (state.getValue(SHELL).getMetadata() << 2);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, SHELL);
    }
}
