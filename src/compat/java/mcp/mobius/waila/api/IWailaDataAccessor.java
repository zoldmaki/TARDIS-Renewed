package mcp.mobius.waila.api;

import net.minecraft.nbt.NBTTagCompound;

/** Compile-only 1.12 Waila/HWYLA API surface; excluded from the released jar. */
public interface IWailaDataAccessor {
    NBTTagCompound getNBTData();
}
