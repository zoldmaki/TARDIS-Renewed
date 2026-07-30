package mcp.mobius.waila.api;

import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Compile-only 1.12 Waila/HWYLA API surface; excluded from the released jar. */
public interface IWailaDataProvider {
    ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config);

    List<String> getWailaHead(
        ItemStack itemStack,
        List<String> currentTip,
        IWailaDataAccessor accessor,
        IWailaConfigHandler config
    );

    List<String> getWailaBody(
        ItemStack itemStack,
        List<String> currentTip,
        IWailaDataAccessor accessor,
        IWailaConfigHandler config
    );

    List<String> getWailaTail(
        ItemStack itemStack,
        List<String> currentTip,
        IWailaDataAccessor accessor,
        IWailaConfigHandler config
    );

    NBTTagCompound getNBTData(
        EntityPlayerMP player,
        TileEntity tileEntity,
        NBTTagCompound tag,
        World world,
        BlockPos pos
    );
}
