package dev.zoldmaki.renewedtardis.network;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** Central packet channel for server-authoritative TARDIS interactions. */
public final class ModNetwork {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(
        RenewedTardisMod.MOD_ID
    );

    private static boolean registered;

    private ModNetwork() {
    }

    public static void registerMessages() {
        if (registered) {
            return;
        }

        CHANNEL.registerMessage(
            MessageActivateTardisBlock.Handler.class,
            MessageActivateTardisBlock.class,
            0,
            Side.SERVER
        );
        registered = true;
    }
}
