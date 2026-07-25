package dev.zoldmaki.renewedtardis.init;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

public final class ModSounds {
    public static final SoundEvent AMBIENT = create("ambient");
    public static final SoundEvent ENGINES = create("engines");
    public static final SoundEvent ENGINE_DRUM = create("engine_drum");
    public static final SoundEvent LANDING = create("landing");
    public static final SoundEvent LANDING_INTERIOR = create("landing_interior");
    public static final SoundEvent LEVEL_UP = create("level_up");
    public static final SoundEvent LOCKED = create("locked");
    public static final SoundEvent SONIC = create("sonic");
    public static final SoundEvent TAKEOFF = create("takeoff");
    public static final SoundEvent TRANSMAT = create("transmat");
    public static final SoundEvent TRANSMAT_FAIL = create("transmat_fail");
    public static final SoundEvent CRACK = create("crack");
    public static final SoundEvent DECORATE = create("decorate");

    public static final SoundEvent[] ALL = {
        AMBIENT,
        ENGINES,
        ENGINE_DRUM,
        LANDING,
        LANDING_INTERIOR,
        LEVEL_UP,
        LOCKED,
        SONIC,
        TAKEOFF,
        TRANSMAT,
        TRANSMAT_FAIL,
        CRACK,
        DECORATE
    };

    private ModSounds() {
    }

    private static SoundEvent create(String path) {
        ResourceLocation id = new ResourceLocation(RenewedTardisMod.MOD_ID, path);
        return new SoundEvent(id).setRegistryName(id);
    }
}
