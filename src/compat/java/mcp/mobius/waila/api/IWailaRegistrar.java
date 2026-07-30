package mcp.mobius.waila.api;

/** Compile-only 1.12 Waila/HWYLA API surface; excluded from the released jar. */
public interface IWailaRegistrar {
    void registerBodyProvider(IWailaDataProvider provider, Class<?> targetClass);

    void registerNBTProvider(IWailaDataProvider provider, Class<?> targetClass);
}
