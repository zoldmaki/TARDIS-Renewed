package dev.zoldmaki.renewedtardis.client;

import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.block.BlockTardisConsolePanel;
import dev.zoldmaki.renewedtardis.console.ConsoleControlId;
import dev.zoldmaki.renewedtardis.console.ConsoleControlKind;
import dev.zoldmaki.renewedtardis.console.ConsoleHitRegionRegistry;
import dev.zoldmaki.renewedtardis.console.ConsolePanel;
import dev.zoldmaki.renewedtardis.console.TardisMainFlightConsoleService;
import dev.zoldmaki.renewedtardis.console.TardisUtilityConsoleService;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisConsolePanel;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Built-in crosshair label for placeholder console hit regions.
 *
 * <p>This intentionally does not depend on HWYLA. It exists so every physical
 * region remains testable before the final model gives each control an obvious
 * shape and animation.</p>
 */
@Mod.EventBusSubscriber(modid = RenewedTardisMod.MOD_ID, value = Side.CLIENT)
public final class ConsoleControlOverlay {
    private ConsoleControlOverlay() {
    }

    @SubscribeEvent
    public static void onOverlayText(RenderGameOverlayEvent.Text event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null || minecraft.player == null) {
            return;
        }
        RayTraceResult hit = minecraft.objectMouseOver;
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.getBlockPos() == null) {
            return;
        }

        BlockPos pos = hit.getBlockPos();
        IBlockState state = minecraft.world.getBlockState(pos);
        if (state.getBlock() != ModBlocks.TARDIS_CONSOLE_PANEL) {
            return;
        }

        ConsolePanel panel = ConsolePanel.fromFacing(state.getValue(BlockTardisConsolePanel.FACING));
        if (panel == null) {
            return;
        }

        Vec3d hitVec = hit.hitVec;
        float hitX = hitVec == null ? 0.5F : clamp((float) (hitVec.x - pos.getX()));
        float hitY = hitVec == null ? 0.5F : clamp((float) (hitVec.y - pos.getY()));
        float hitZ = hitVec == null ? 0.5F : clamp((float) (hitVec.z - pos.getZ()));
        ConsoleControlId control = ConsoleHitRegionRegistry.resolve(panel, hitX, hitY, hitZ);
        if (control == null) {
            return;
        }

        List<String> right = event.getRight();
        right.add(TextFormatting.AQUA + "TARDIS — " + panel.getDisplayName());
        String displayName = control.getDisplayName();
        TextFormatting displayColor = TextFormatting.WHITE;
        if (control.isWaypointButton()) {
            TileEntity tile = minecraft.world.getTileEntity(pos);
            if (tile instanceof TileEntityTardisConsolePanel) {
                TileEntityTardisConsolePanel panelTile = (TileEntityTardisConsolePanel) tile;
                int slot = control.getWaypointSlotIndex();
                displayName = "Waypoint " + panelTile.getWaypointName(slot);
                displayColor = waypointColor(panelTile.getWaypointColor(slot));
            }
        }
        right.add(displayColor + displayName);
        if (control.isWaypointButton()) {
            right.add(TextFormatting.DARK_GRAY + "Named paper: rename • Dye: recolor");
        }
        if (control.isTakeoffSequenceControl()) {
            right.add(
                TextFormatting.GOLD + "Takeoff step " + control.getTakeoffSequenceStep() + " / 4"
            );
        } else if (control.isSneakRequired()) {
            right.add(TextFormatting.YELLOW + "Sneak-right-click safety control");
        } else if (TardisMainFlightConsoleService.isReadoutControl(control)
            || TardisUtilityConsoleService.isReadoutControl(control)) {
            right.add(TextFormatting.DARK_GRAY + "Right-click: inspect / cycle readout");
        } else if (control.getKind() == ConsoleControlKind.PULL_SWITCH) {
            right.add(TextFormatting.DARK_GRAY + "Right-click: toggle");
        } else if (control.getKind().isDirectlyAdjustable()) {
            right.add(TextFormatting.DARK_GRAY + "Right-click: next • Sneak: previous");
        } else if (control.getKind().isReadOnly()) {
            right.add(TextFormatting.DARK_GRAY + "Read-only placeholder");
        }
    }

    private static TextFormatting waypointColor(int dyeDamage) {
        switch (dyeDamage) {
            case 0:
                return TextFormatting.DARK_GRAY;
            case 1:
                return TextFormatting.DARK_RED;
            case 2:
                return TextFormatting.DARK_GREEN;
            case 3:
                return TextFormatting.GOLD;
            case 4:
                return TextFormatting.DARK_BLUE;
            case 5:
                return TextFormatting.DARK_PURPLE;
            case 6:
                return TextFormatting.DARK_AQUA;
            case 7:
                return TextFormatting.GRAY;
            case 8:
                return TextFormatting.DARK_GRAY;
            case 9:
            case 13:
                return TextFormatting.LIGHT_PURPLE;
            case 10:
                return TextFormatting.GREEN;
            case 11:
                return TextFormatting.YELLOW;
            case 12:
                return TextFormatting.AQUA;
            case 14:
                return TextFormatting.GOLD;
            case 15:
            default:
                return TextFormatting.WHITE;
        }
    }

    private static float clamp(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0.5F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
