package dev.zoldmaki.renewedtardis.sonic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ordered registry for sonic actions. Add-ons may register actions during
 * pre-initialization. The first non-PASS result wins.
 */
public final class SonicActionRegistry {
    private static final List<ISonicBlockAction> BLOCK_ACTIONS = new ArrayList<>();
    private static final List<ISonicEntityAction> ENTITY_ACTIONS = new ArrayList<>();

    private SonicActionRegistry() {
    }

    public static void registerBlockAction(ISonicBlockAction action) {
        if (action != null && !BLOCK_ACTIONS.contains(action)) {
            BLOCK_ACTIONS.add(action);
        }
    }

    public static void registerEntityAction(ISonicEntityAction action) {
        if (action != null && !ENTITY_ACTIONS.contains(action)) {
            ENTITY_ACTIONS.add(action);
        }
    }

    public static SonicActionResult dispatchBlock(SonicBlockContext context) {
        for (ISonicBlockAction action : BLOCK_ACTIONS) {
            SonicActionResult result = action.execute(context);
            if (result != SonicActionResult.PASS) {
                return result;
            }
        }
        return SonicActionResult.PASS;
    }

    public static SonicActionResult dispatchEntity(SonicEntityContext context) {
        for (ISonicEntityAction action : ENTITY_ACTIONS) {
            SonicActionResult result = action.execute(context);
            if (result != SonicActionResult.PASS) {
                return result;
            }
        }
        return SonicActionResult.PASS;
    }

    public static List<ISonicBlockAction> getBlockActions() {
        return Collections.unmodifiableList(BLOCK_ACTIONS);
    }

    public static List<ISonicEntityAction> getEntityActions() {
        return Collections.unmodifiableList(ENTITY_ACTIONS);
    }
}
