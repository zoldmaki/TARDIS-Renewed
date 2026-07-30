package dev.zoldmaki.renewedtardis.integration.computercraft;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.config.TardisConfig;
import dev.zoldmaki.renewedtardis.damage.TardisSubsystem;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.energy.TardisEnergyService;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisEngineCore;
import dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeService;
import dev.zoldmaki.renewedtardis.upgrade.TardisUpgradeType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

/**
 * Reflection-only ComputerCraft/CC:Tweaked bridge. No ComputerCraft API class
 * is packaged in TARDIS Renewed and the bridge remains dormant when absent.
 */
public final class ComputerCraftIntegration {
    private static final String[] METHOD_NAMES = {
        "getId",
        "getOwner",
        "getState",
        "getFlightProgress",
        "getEnergy",
        "getDestination",
        "getExterior",
        "getSystems",
        "getUpgrades",
        "getRooms",
        "getAccess"
    };

    private static boolean attempted;
    private static boolean installed;
    private static boolean registered;
    private static String lastError = "";
    private static Object providerReference;
    private static Class<?> peripheralInterface;
    private static Class<?> luaContextInterface;
    private static Class<?> luaTaskInterface;

    private ComputerCraftIntegration() {
    }

    public static void registerIfAvailable() {
        if (attempted) {
            return;
        }
        attempted = true;
        installed = Loader.isModLoaded("computercraft");
        if (!installed || !TardisConfig.isComputerCraftIntegrationEnabled()) {
            return;
        }

        try {
            Class<?> apiClass = Class.forName("dan200.computercraft.api.ComputerCraftAPI");
            Class<?> providerInterface = Class.forName(
                "dan200.computercraft.api.peripheral.IPeripheralProvider"
            );
            peripheralInterface = Class.forName("dan200.computercraft.api.peripheral.IPeripheral");
            luaContextInterface = Class.forName("dan200.computercraft.api.lua.ILuaContext");
            luaTaskInterface = Class.forName("dan200.computercraft.api.lua.ILuaTask");

            providerReference = Proxy.newProxyInstance(
                providerInterface.getClassLoader(),
                new Class<?>[] { providerInterface },
                new PeripheralProviderHandler()
            );
            Method registerProvider = apiClass.getMethod("registerPeripheralProvider", providerInterface);
            registerProvider.invoke(null, providerReference);
            registered = true;
            RenewedTardisMod.logger.info(
                "Registered the optional read-only ComputerCraft engine-core peripheral."
            );
        } catch (Throwable throwable) {
            registered = false;
            lastError = describeThrowable(throwable);
            RenewedTardisMod.logger.error(
                "ComputerCraft was detected, but the optional TARDIS peripheral could not be registered: {}",
                lastError
            );
        }
    }

    public static boolean isInstalled() {
        return attempted ? installed : Loader.isModLoaded("computercraft");
    }

    public static boolean isRegistered() {
        return registered;
    }

    public static String getLastError() {
        return lastError;
    }

    private static final class PeripheralProviderHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            String name = method.getName();
            if ("getPeripheral".equals(name) && arguments != null && arguments.length >= 2) {
                World world = (World) arguments[0];
                BlockPos pos = (BlockPos) arguments[1];
                return createPeripheral(world, pos);
            }
            if ("toString".equals(name)) {
                return "TARDIS Renewed peripheral provider";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return arguments != null && arguments.length == 1 && proxy == arguments[0];
            }
            return defaultValue(method.getReturnType());
        }
    }

    @Nullable
    private static Object createPeripheral(World world, BlockPos pos) {
        if (world == null || pos == null || world.isRemote || peripheralInterface == null) {
            return null;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityTardisEngineCore)) {
            return null;
        }
        UUID tardisId = ((TileEntityTardisEngineCore) tile).getTardisId();
        if (tardisId == null) {
            return null;
        }
        return Proxy.newProxyInstance(
            peripheralInterface.getClassLoader(),
            new Class<?>[] { peripheralInterface },
            new TardisPeripheralHandler(world, pos.toImmutable(), tardisId)
        );
    }

    private static final class TardisPeripheralHandler implements InvocationHandler {
        private final World world;
        private final BlockPos position;
        private final UUID tardisId;

        private TardisPeripheralHandler(World world, BlockPos position, UUID tardisId) {
            this.world = world;
            this.position = position;
            this.tardisId = tardisId;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
            String name = method.getName();
            if ("getType".equals(name)) {
                return "tardis";
            }
            if ("getMethodNames".equals(name)) {
                return METHOD_NAMES.clone();
            }
            if ("callMethod".equals(name)) {
                int methodIndex = arguments == null || arguments.length < 3
                    ? -1
                    : ((Number) arguments[2]).intValue();
                Object luaContext = arguments == null || arguments.length < 2 ? null : arguments[1];
                return executeOnMainThread(luaContext, methodIndex);
            }
            if ("attach".equals(name) || "detach".equals(name)) {
                return null;
            }
            if ("equals".equals(name)) {
                Object other = arguments == null || arguments.length == 0 ? null : arguments[0];
                return isSamePeripheral(proxy, other);
            }
            if ("hashCode".equals(name)) {
                return 31 * tardisId.hashCode() + position.hashCode();
            }
            if ("toString".equals(name)) {
                return "TARDIS peripheral " + tardisId + " at " + position;
            }
            return defaultValue(method.getReturnType());
        }

        private Object[] executeOnMainThread(Object luaContext, final int methodIndex) throws Throwable {
            if (methodIndex < 0 || methodIndex >= METHOD_NAMES.length) {
                throw new IllegalArgumentException("Unknown TARDIS peripheral method index " + methodIndex);
            }
            if (luaContext == null || luaContextInterface == null || luaTaskInterface == null) {
                return readMethod(methodIndex);
            }

            Object task = Proxy.newProxyInstance(
                luaTaskInterface.getClassLoader(),
                new Class<?>[] { luaTaskInterface },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
                        if ("execute".equals(method.getName())) {
                            return readMethod(methodIndex);
                        }
                        if ("toString".equals(method.getName())) {
                            return "TARDIS peripheral read task";
                        }
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(method.getName())) {
                            return arguments != null && arguments.length == 1 && proxy == arguments[0];
                        }
                        return defaultValue(method.getReturnType());
                    }
                }
            );

            try {
                Method execute = luaContextInterface.getMethod("executeMainThreadTask", luaTaskInterface);
                Object result = execute.invoke(luaContext, task);
                return result instanceof Object[] ? (Object[]) result : new Object[0];
            } catch (InvocationTargetException exception) {
                throw exception.getCause() == null ? exception : exception.getCause();
            }
        }

        private Object[] readMethod(int methodIndex) {
            TardisRecord record = getRecord();
            if (record == null) {
                return new Object[] { null, "The engine core is no longer linked to a TARDIS." };
            }

            switch (methodIndex) {
                case 0:
                    return new Object[] { record.getTardisId().toString() };
                case 1:
                    return new Object[] { record.getOwnerName(), record.getOwnerId().toString() };
                case 2:
                    return new Object[] {
                        record.isInFlight() ? "in_flight" : "landed",
                        record.isExteriorLocked(),
                        record.getExteriorShell().getName(),
                        record.getFlightSpeed().getName(),
                        record.getFlightAccuracy().getName()
                    };
                case 3: {
                    long remaining = record.getFlightRemainingMillis(System.currentTimeMillis());
                    return new Object[] {
                        record.getFlightProgressPercent(System.currentTimeMillis()),
                        (remaining + 999L) / 1000L,
                        record.isFlightReady(System.currentTimeMillis())
                    };
                }
                case 4: {
                    MinecraftServer server = world.getMinecraftServer();
                    if (server != null) {
                        TardisEnergyService.refresh(TardisWorldData.get(server), record);
                    }
                    return new Object[] {
                        record.getArtronEnergy(),
                        record.getArtronCapacity(),
                        TardisEnergyService.getRegenerationPerSecond(record)
                    };
                }
                case 5:
                    return describePosition(record.getDestinationPosition(), record.getDestinationFacing());
                case 6: {
                    Object[] positionValues = describePosition(
                        record.getExteriorPosition(),
                        record.getFlightDepartureFacing()
                    );
                    if (positionValues.length == 0) {
                        return positionValues;
                    }
                    return new Object[] {
                        positionValues[0],
                        positionValues[1],
                        positionValues[2],
                        positionValues[3],
                        positionValues[4],
                        record.isExteriorLocked(),
                        record.getExteriorShell().getName()
                    };
                }
                case 7:
                    return new Object[] {
                        record.getSubsystemHealth(TardisSubsystem.ENGINES),
                        record.getSubsystemHealth(TardisSubsystem.NAVIGATION),
                        record.getSubsystemHealth(TardisSubsystem.STABILIZERS),
                        record.getSubsystemHealth(TardisSubsystem.DEMATERIALIZATION)
                    };
                case 8:
                    return new Object[] {
                        record.getUpgradeCount(TardisUpgradeType.SPEED),
                        record.getUpgradeCount(TardisUpgradeType.CAPACITY),
                        record.getUpgradeCount(TardisUpgradeType.REGENERATION),
                        record.getUpgradeCount(TardisUpgradeType.ROOM),
                        record.getUpgradeCount(TardisUpgradeType.PROTECTION),
                        record.getInstalledUpgradeCount(),
                        TardisConfig.getUpgradeSlotCount()
                    };
                case 9:
                    return new Object[] {
                        record.getRooms().size(),
                        TardisUpgradeService.getRoomLimit(record),
                        record.getRoomConnections().size()
                    };
                case 10:
                    return new Object[] { record.getAccessMode().getName(), record.getCrew().size() };
                default:
                    return new Object[0];
            }
        }

        @Nullable
        private TardisRecord getRecord() {
            if (world == null || world.isRemote) {
                return null;
            }
            TileEntity tile = world.getTileEntity(position);
            if (!(tile instanceof TileEntityTardisEngineCore)) {
                return null;
            }
            UUID currentId = ((TileEntityTardisEngineCore) tile).getTardisId();
            if (!tardisId.equals(currentId)) {
                return null;
            }
            MinecraftServer server = world.getMinecraftServer();
            return server == null ? null : TardisWorldData.get(server).getById(tardisId);
        }

        private boolean isSamePeripheral(Object proxy, Object other) {
            if (proxy == other) {
                return true;
            }
            if (other == null || !Proxy.isProxyClass(other.getClass())) {
                return false;
            }
            InvocationHandler handler;
            try {
                handler = Proxy.getInvocationHandler(other);
            } catch (IllegalArgumentException exception) {
                return false;
            }
            if (!(handler instanceof TardisPeripheralHandler)) {
                return false;
            }
            TardisPeripheralHandler otherHandler = (TardisPeripheralHandler) handler;
            return tardisId.equals(otherHandler.tardisId)
                && world == otherHandler.world
                && position.equals(otherHandler.position);
        }
    }

    private static Object[] describePosition(@Nullable DimensionPosition position, @Nullable net.minecraft.util.EnumFacing facing) {
        if (position == null) {
            return new Object[0];
        }
        BlockPos blockPos = position.getPosition();
        return new Object[] {
            blockPos.getX(),
            blockPos.getY(),
            blockPos.getZ(),
            position.getDimension(),
            facing == null ? "north" : facing.getName()
        };
    }

    @Nullable
    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE || !returnType.isPrimitive()) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Character.TYPE) {
            return '\0';
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Float.TYPE) {
            return 0.0F;
        }
        if (returnType == Double.TYPE) {
            return 0.0D;
        }
        return null;
    }

    private static String describeThrowable(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor instanceof InvocationTargetException
            && ((InvocationTargetException) cursor).getCause() != null) {
            cursor = ((InvocationTargetException) cursor).getCause();
        }
        String message = cursor.getMessage();
        return cursor.getClass().getSimpleName() + (message == null || message.isEmpty() ? "" : ": " + message);
    }
}
