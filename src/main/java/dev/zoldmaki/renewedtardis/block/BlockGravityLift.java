package dev.zoldmaki.renewedtardis.block;

import dev.zoldmaki.renewedtardis.RenewedTardisCreativeTab;
import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.tile.TileEntityGravityLift;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/** A paired vertical transport pad inspired by the original gravity lift. */
public final class BlockGravityLift extends Block {
    public static final PropertyBool UP = PropertyBool.create("up");

    public BlockGravityLift() {
        super(Material.IRON);
        setRegistryName(RenewedTardisMod.MOD_ID, "gravity_lift");
        setTranslationKey(RenewedTardisMod.MOD_ID + ".gravity_lift");
        setCreativeTab(RenewedTardisCreativeTab.INSTANCE);
        setHardness(3.0F);
        setResistance(12.0F);
        setSoundType(SoundType.METAL);
        setLightLevel(0.55F);
        setDefaultState(blockState.getBaseState().withProperty(UP, Boolean.TRUE));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityGravityLift();
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
        boolean nowUp = !state.getValue(UP);
        world.setBlockState(pos, state.withProperty(UP, nowUp), 3);
        player.sendStatusMessage(
            new TextComponentString(
                TextFormatting.AQUA + "[GRAVITY LIFT] " + TextFormatting.RESET
                    + "Direction set to " + (nowUp ? "up" : "down") + "."
            ),
            false
        );
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityGravityLift) {
            ((TileEntityGravityLift) tile).clearActiveEntities();
        }
        return true;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(UP, (meta & 1) == 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(UP) ? 0 : 1;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, UP);
    }
}
