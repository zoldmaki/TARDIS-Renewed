package dev.zoldmaki.renewedtardis.block;

import dev.zoldmaki.renewedtardis.RenewedTardisCreativeTab;
import dev.zoldmaki.renewedtardis.RenewedTardisMod;
import dev.zoldmaki.renewedtardis.access.TardisAccessService;
import dev.zoldmaki.renewedtardis.data.DimensionPosition;
import dev.zoldmaki.renewedtardis.data.TardisRecord;
import dev.zoldmaki.renewedtardis.data.TardisWorldData;
import dev.zoldmaki.renewedtardis.exterior.ExteriorShell;
import dev.zoldmaki.renewedtardis.init.ModBlocks;
import dev.zoldmaki.renewedtardis.init.ModItems;
import dev.zoldmaki.renewedtardis.init.ModSounds;
import dev.zoldmaki.renewedtardis.interaction.TardisInteractionGuard;
import dev.zoldmaki.renewedtardis.item.ItemTardisKey;
import dev.zoldmaki.renewedtardis.teleport.TardisTeleportService;
import dev.zoldmaki.renewedtardis.tile.TileEntityTardisExterior;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Lower half of the two-block exterior. It owns the tile entity and persistent
 * TARDIS link while the upper half reserves the visible/collision space.
 */
public final class BlockTardisExterior extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyEnum<ExteriorShell> SHELL = PropertyEnum.create("shell", ExteriorShell.class);

    private static final AxisAlignedBB BOUNDS = new AxisAlignedBB(
        1.0D / 16.0D,
        0.0D,
        1.0D / 16.0D,
        15.0D / 16.0D,
        1.0D,
        15.0D / 16.0D
    );
    private static final ThreadLocal<Integer> STRUCTURE_CHANGE_DEPTH = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return Integer.valueOf(0);
        }
    };

    public BlockTardisExterior() {
        super(Material.IRON);
        setRegistryName(RenewedTardisMod.MOD_ID, "tardis_exterior");
        setTranslationKey(RenewedTardisMod.MOD_ID + ".tardis_exterior");
        setCreativeTab(RenewedTardisCreativeTab.INSTANCE);
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
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityTardisExterior();
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        if (pos.getY() >= world.getHeight() - 1) {
            return false;
        }
        IBlockState upper = world.getBlockState(pos.up());
        return super.canPlaceBlockAt(world, pos)
            && (upper.getBlock() == ModBlocks.TARDIS_EXTERIOR_TOP || upper.getMaterial().isReplaceable());
    }

    @Override
    public IBlockState getStateForPlacement(
        World world,
        BlockPos pos,
        EnumFacing clickedFace,
        float hitX,
        float hitY,
        float hitZ,
        int meta,
        EntityLivingBase placer
    ) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        if (!world.isRemote && !isStructureChangeInProgress()) {
            synchronizeStructure(world, pos, state);
        }
    }

    @Override
    public boolean onBlockActivated(
        World world,
        BlockPos pos,
        IBlockState state,
        EntityPlayer player,
        EnumHand hand,
        EnumFacing clickedFace,
        float hitX,
        float hitY,
        float hitZ
    ) {
        if (world.isRemote) {
            return true;
        }
        if (!TardisInteractionGuard.tryBegin(player, world, pos)) {
            return true;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityTardisExterior)) {
            return false;
        }

        TileEntityTardisExterior exterior = (TileEntityTardisExterior) tile;
        ItemStack held = player.getHeldItem(hand);

        if (!held.isEmpty() && held.getItem() == ModItems.TARDIS_KEY && player instanceof EntityPlayerMP) {
            return handleKey(world, pos, (EntityPlayerMP) player, held, exterior);
        }

        if (!exterior.isBound()) {
            send(player, TextFormatting.YELLOW, "This exterior is unbound. Use a TARDIS Key on it.");
            return true;
        }

        if (exterior.isLocked()) {
            world.playSound(null, pos, ModSounds.LOCKED, SoundCategory.BLOCKS, 0.8F, 1.0F);
            send(player, TextFormatting.RED, "The TARDIS exterior is locked.");
            return true;
        }

        EnumFacing front = state.getValue(FACING);
        if (clickedFace != front) {
            send(
                player,
                TextFormatting.YELLOW,
                "The exterior doors are on the " + front.getName() + " side."
            );
            return true;
        }

        if (!(player instanceof EntityPlayerMP) || world.getMinecraftServer() == null) {
            return false;
        }

        TardisWorldData data = TardisWorldData.get(world.getMinecraftServer());
        TardisRecord record = data.getById(exterior.getTardisId());
        if (record == null) {
            send(player, TextFormatting.RED, "This exterior points to a missing TARDIS record.");
            return true;
        }

        if (record.isInFlight()) {
            send(player, TextFormatting.YELLOW, "The TARDIS is currently in flight.");
            return true;
        }
        if (!TardisAccessService.canEnter(player, record)) {
            send(
                player,
                TextFormatting.RED,
                "Access denied. This TARDIS currently allows " + record.getAccessMode().getDisplayName() + "."
            );
            return true;
        }

        world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 0.75F, 0.9F);
        TardisTeleportService.enterInterior((EntityPlayerMP) player, record);
        return true;
    }

    private static boolean handleKey(
        World world,
        BlockPos pos,
        EntityPlayerMP player,
        ItemStack key,
        TileEntityTardisExterior exterior
    ) {
        if (world.getMinecraftServer() == null) {
            return false;
        }

        TardisWorldData data = TardisWorldData.get(world.getMinecraftServer());
        UUID keyTardisId = ItemTardisKey.getTardisId(key);
        TardisRecord record;

        if (keyTardisId == null) {
            record = data.getOrCreateForOwner(player.getUniqueID(), player.getName());
            ItemTardisKey.bind(key, record);
            keyTardisId = record.getTardisId();
        } else {
            record = data.getById(keyTardisId);
        }

        if (record == null) {
            send(player, TextFormatting.RED, "This key points to a missing TARDIS record.");
            return true;
        }
        if (record.isInFlight()) {
            send(player, TextFormatting.YELLOW, "That TARDIS is currently in flight.");
            return true;
        }

        if (!exterior.isBound()) {
            if (!TardisAccessService.isOwner(player, record) && !TardisAccessService.isOperator(player)) {
                send(player, TextFormatting.RED, "Only the TARDIS owner or an operator can bind a new exterior.");
                return true;
            }
            DimensionPosition existingPosition = record.getExteriorPosition();
            if (existingPosition != null && !matches(existingPosition, world, pos)) {
                send(
                    player,
                    TextFormatting.RED,
                    "That TARDIS already has an exterior. Use /tr exterior to relocate it safely."
                );
                return true;
            }

            if (!applyShell(world, pos, record.getExteriorShell())) {
                send(player, TextFormatting.RED, "The exterior's upper half is obstructed and cannot be linked safely.");
                return true;
            }
            exterior.bind(record);
            data.setExteriorPosition(
                record.getTardisId(),
                new DimensionPosition(world.provider.getDimension(), pos)
            );
            send(player, TextFormatting.GOLD, "The exterior binds to your TARDIS and locks.");
            return true;
        }

        if (!keyTardisId.equals(exterior.getTardisId())) {
            world.playSound(null, pos, ModSounds.LOCKED, SoundCategory.BLOCKS, 0.8F, 0.9F);
            send(player, TextFormatting.RED, "This key does not belong to that TARDIS.");
            return true;
        }
        if (!TardisAccessService.canUseKey(player, record)) {
            world.playSound(null, pos, ModSounds.LOCKED, SoundCategory.BLOCKS, 0.8F, 0.9F);
            send(player, TextFormatting.RED, "You do not have key permission for this TARDIS.");
            return true;
        }

        exterior.setLocked(!exterior.isLocked());
        data.setExteriorLocked(record.getTardisId(), exterior.isLocked());
        world.playSound(null, pos, ModSounds.LOCKED, SoundCategory.BLOCKS, 0.7F, exterior.isLocked() ? 0.9F : 1.2F);
        send(
            player,
            exterior.isLocked() ? TextFormatting.RED : TextFormatting.GREEN,
            exterior.isLocked() ? "TARDIS locked." : "TARDIS unlocked."
        );
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        boolean internalStructureChange = isStructureChangeInProgress();
        if (!internalStructureChange) {
            beginStructureChange();
            try {
                if (world.getBlockState(pos.up()).getBlock() == ModBlocks.TARDIS_EXTERIOR_TOP) {
                    world.setBlockToAir(pos.up());
                }
            } finally {
                endStructureChange();
            }

            clearSavedExteriorIfPresent(world, pos);
        }
        super.breakBlock(world, pos, state);
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

    public static boolean placeStructure(World world, BlockPos pos, IBlockState lowerState) {
        return placeStructureDetailed(world, pos, lowerState, false).isSuccess();
    }

    /**
     * Places both exterior halves and reports the exact stage of any failure.
     * The force flag is intentionally destructive and allows an operator
     * recovery landing to replace whatever occupies the two target blocks.
     */
    public static StructurePlacementResult placeStructureDetailed(
        World world,
        BlockPos pos,
        IBlockState lowerState,
        boolean forceOverwrite
    ) {
        if (world == null) {
            return StructurePlacementResult.failure("no world was supplied", "world=null");
        }
        if (pos == null) {
            return StructurePlacementResult.failure("no landing position was supplied", "position=null");
        }
        if (lowerState == null || lowerState.getBlock() != ModBlocks.TARDIS_EXTERIOR) {
            return StructurePlacementResult.failure(
                "the requested lower block state is not the TARDIS exterior",
                "requestedLower=" + describeState(lowerState)
            );
        }
        if (pos.getY() < 0 || pos.getY() >= world.getHeight() - 1) {
            return StructurePlacementResult.failure(
                "the two-block exterior is outside the build height",
                "dimension=" + world.provider.getDimension() + ", target=" + pos
                    + ", worldHeight=" + world.getHeight()
            );
        }

        // Explicitly load the destination chunk before changing either half.
        // Both exterior halves share the same X/Z chunk; provideChunk is the
        // stable_39-compatible 1.12.2 API available through IChunkProvider.
        world.getChunkProvider().provideChunk(pos.getX() >> 4, pos.getZ() >> 4);

        IBlockState oldLower = world.getBlockState(pos);
        IBlockState oldUpper = world.getBlockState(pos.up());
        String before = "beforeLower=" + describeState(oldLower)
            + ", beforeUpper=" + describeState(oldUpper);

        if (forceOverwrite) {
            beginStructureChange();
            try {
                if (!clearForForcedPlacement(world, pos.up())) {
                    return StructurePlacementResult.failure(
                        "the upper target block resisted forced removal",
                        diagnostic(world, pos, lowerState, before, false, false)
                    );
                }
                if (!clearForForcedPlacement(world, pos)) {
                    return StructurePlacementResult.failure(
                        "the lower target block resisted forced removal",
                        diagnostic(world, pos, lowerState, before, false, false)
                    );
                }
            } finally {
                endStructureChange();
            }
            oldLower = world.getBlockState(pos);
            oldUpper = world.getBlockState(pos.up());
        } else {
            boolean lowerAvailable = oldLower.getBlock() == ModBlocks.TARDIS_EXTERIOR
                || oldLower.getMaterial().isReplaceable();
            boolean upperAvailable = oldUpper.getBlock() == ModBlocks.TARDIS_EXTERIOR_TOP
                || oldUpper.getMaterial().isReplaceable();
            if (!lowerAvailable || !upperAvailable) {
                return StructurePlacementResult.failure(
                    !lowerAvailable
                        ? "the lower target block is not replaceable"
                        : "the upper target block is not replaceable",
                    diagnostic(world, pos, lowerState, before, false, false)
                );
            }
        }

        IBlockState upperState = upperStateFrom(lowerState);
        boolean lowerCallReturned = true;
        boolean upperCallReturned = true;

        beginStructureChange();
        try {
            IBlockState currentLower = world.getBlockState(pos);
            if (!currentLower.equals(lowerState)) {
                lowerCallReturned = world.setBlockState(pos, lowerState, 3);
            }

            // In some modded server environments the boolean return can be
            // unreliable. The authoritative check is the state now in-world.
            if (!world.getBlockState(pos).equals(lowerState)) {
                String details = diagnostic(
                    world,
                    pos,
                    lowerState,
                    before,
                    lowerCallReturned,
                    false
                );
                removePartialPlacement(world, pos);
                return StructurePlacementResult.failure(
                    "the lower exterior block did not remain placed",
                    details
                );
            }

            ensureExteriorTileEntity(world, pos);

            IBlockState currentUpper = world.getBlockState(pos.up());
            if (!currentUpper.equals(upperState)) {
                upperCallReturned = world.setBlockState(pos.up(), upperState, 3);
            }

            if (!world.getBlockState(pos.up()).equals(upperState)) {
                String details = diagnostic(
                    world,
                    pos,
                    lowerState,
                    before,
                    lowerCallReturned,
                    upperCallReturned
                );
                removePartialPlacement(world, pos);
                return StructurePlacementResult.failure(
                    "the upper exterior block did not remain placed",
                    details
                );
            }

            ensureExteriorTileEntity(world, pos);
        } finally {
            endStructureChange();
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityTardisExterior)) {
            String details = diagnostic(
                world,
                pos,
                lowerState,
                before,
                lowerCallReturned,
                upperCallReturned
            );
            removeStructure(world, pos);
            return StructurePlacementResult.failure(
                "the exterior tile entity could not be created",
                details
            );
        }

        return StructurePlacementResult.success(
            diagnostic(world, pos, lowerState, before, lowerCallReturned, upperCallReturned)
        );
    }

    private static boolean clearForForcedPlacement(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (world.isAirBlock(pos)) {
            return true;
        }
        if (
            state.getBlock() == ModBlocks.TARDIS_EXTERIOR
                || state.getBlock() == ModBlocks.TARDIS_EXTERIOR_TOP
        ) {
            removeStructure(world, pos);
        } else {
            world.setBlockToAir(pos);
        }
        return world.isAirBlock(pos);
    }

    private static void removePartialPlacement(World world, BlockPos pos) {
        if (world.getBlockState(pos.up()).getBlock() == ModBlocks.TARDIS_EXTERIOR_TOP) {
            world.setBlockToAir(pos.up());
        }
        if (world.getBlockState(pos).getBlock() == ModBlocks.TARDIS_EXTERIOR) {
            world.setBlockToAir(pos);
        }
    }

    private static void ensureExteriorTileEntity(World world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock() != ModBlocks.TARDIS_EXTERIOR) {
            return;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityTardisExterior) {
            return;
        }
        if (tile != null) {
            world.removeTileEntity(pos);
        }
        TileEntityTardisExterior exterior = new TileEntityTardisExterior();
        world.setTileEntity(pos, exterior);
        exterior.markDirty();
    }

    private static String diagnostic(
        World world,
        BlockPos pos,
        IBlockState requestedLower,
        String before,
        boolean lowerCallReturned,
        boolean upperCallReturned
    ) {
        return "dimension=" + world.provider.getDimension()
            + ", target=" + pos
            + ", requestedLower=" + describeState(requestedLower)
            + ", requestedUpper=" + describeState(upperStateFrom(requestedLower))
            + ", lowerSetReturned=" + lowerCallReturned
            + ", upperSetReturned=" + upperCallReturned
            + ", " + before
            + ", afterLower=" + describeState(world.getBlockState(pos))
            + ", afterUpper=" + describeState(world.getBlockState(pos.up()))
            + ", tile=" + describeTile(world.getTileEntity(pos));
    }

    private static String describeState(IBlockState state) {
        if (state == null) {
            return "null";
        }
        String name = state.getBlock().getRegistryName() == null
            ? state.getBlock().getClass().getName()
            : state.getBlock().getRegistryName().toString();
        return name + state.getProperties().toString()
            + "{material=" + state.getMaterial().toString()
            + ", replaceable=" + state.getMaterial().isReplaceable() + "}";
    }

    private static String describeTile(TileEntity tile) {
        return tile == null ? "null" : tile.getClass().getName();
    }

    public static final class StructurePlacementResult {
        private final boolean success;
        private final String reason;
        private final String details;

        private StructurePlacementResult(boolean success, String reason, String details) {
            this.success = success;
            this.reason = reason;
            this.details = details;
        }

        private static StructurePlacementResult success(String details) {
            return new StructurePlacementResult(true, "success", details);
        }

        private static StructurePlacementResult failure(String reason, String details) {
            return new StructurePlacementResult(false, reason, details);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getReason() {
            return reason;
        }

        public String getDetails() {
            return details;
        }
    }

    public static boolean synchronizeStructure(World world, BlockPos pos, IBlockState lowerState) {
        if (lowerState.getBlock() != ModBlocks.TARDIS_EXTERIOR) {
            return false;
        }

        IBlockState upper = world.getBlockState(pos.up());
        if (upper.getBlock() != ModBlocks.TARDIS_EXTERIOR_TOP && !upper.getMaterial().isReplaceable()) {
            return false;
        }

        NBTTagCompound savedTileData = null;
        TileEntity originalTile = world.getTileEntity(pos);
        if (originalTile instanceof TileEntityTardisExterior) {
            savedTileData = originalTile.writeToNBT(new NBTTagCompound());
        }

        beginStructureChange();
        try {
            if (!world.getBlockState(pos).equals(lowerState) && !world.setBlockState(pos, lowerState, 3)) {
                return false;
            }
            restoreExteriorTile(world, pos, savedTileData);

            IBlockState desiredUpper = upperStateFrom(lowerState);
            boolean upperReady = world.getBlockState(pos.up()).equals(desiredUpper)
                || world.setBlockState(pos.up(), desiredUpper, 3);
            if (upperReady) {
                restoreExteriorTile(world, pos, savedTileData);
            }
            return upperReady;
        } finally {
            endStructureChange();
        }
    }

    /** Removes both exterior halves without treating the operation as a player break. */
    public static boolean removeStructure(World world, BlockPos pos) {
        BlockPos lowerPos = pos;
        if (world.getBlockState(pos).getBlock() == ModBlocks.TARDIS_EXTERIOR_TOP) {
            lowerPos = pos.down();
        }

        boolean hadLower = world.getBlockState(lowerPos).getBlock() == ModBlocks.TARDIS_EXTERIOR;
        boolean hadUpper = world.getBlockState(lowerPos.up()).getBlock() == ModBlocks.TARDIS_EXTERIOR_TOP;
        if (!hadLower && !hadUpper) {
            return true;
        }

        beginStructureChange();
        try {
            if (hadUpper) {
                world.setBlockToAir(lowerPos.up());
            }
            if (hadLower) {
                world.setBlockToAir(lowerPos);
            }
        } finally {
            endStructureChange();
        }
        return world.getBlockState(lowerPos).getBlock() != ModBlocks.TARDIS_EXTERIOR
            && world.getBlockState(lowerPos.up()).getBlock() != ModBlocks.TARDIS_EXTERIOR_TOP;
    }

    private static void restoreExteriorTile(World world, BlockPos pos, NBTTagCompound savedTileData) {
        if (savedTileData == null || world.getBlockState(pos).getBlock() != ModBlocks.TARDIS_EXTERIOR) {
            return;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityTardisExterior)) {
            tile = new TileEntityTardisExterior();
            world.setTileEntity(pos, tile);
        }
        tile.readFromNBT(savedTileData.copy());
        tile.markDirty();
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }

    public static boolean applyShell(World world, BlockPos pos, ExteriorShell shell) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != ModBlocks.TARDIS_EXTERIOR) {
            return false;
        }
        return synchronizeStructure(world, pos, state.withProperty(SHELL, shell));
    }

    private static IBlockState upperStateFrom(IBlockState lowerState) {
        return ModBlocks.TARDIS_EXTERIOR_TOP
            .getDefaultState()
            .withProperty(BlockTardisExteriorTop.FACING, lowerState.getValue(FACING))
            .withProperty(BlockTardisExteriorTop.SHELL, lowerState.getValue(SHELL));
    }

    static boolean isStructureChangeInProgress() {
        return STRUCTURE_CHANGE_DEPTH.get().intValue() > 0;
    }

    static void beginStructureChange() {
        STRUCTURE_CHANGE_DEPTH.set(Integer.valueOf(STRUCTURE_CHANGE_DEPTH.get().intValue() + 1));
    }

    static void endStructureChange() {
        int depth = STRUCTURE_CHANGE_DEPTH.get().intValue();
        STRUCTURE_CHANGE_DEPTH.set(Integer.valueOf(Math.max(0, depth - 1)));
    }

    static void clearSavedExteriorIfPresent(World world, BlockPos pos) {
        if (world.isRemote || world.getMinecraftServer() == null) {
            return;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityTardisExterior)) {
            return;
        }

        UUID tardisId = ((TileEntityTardisExterior) tile).getTardisId();
        if (tardisId == null) {
            return;
        }

        TardisWorldData data = TardisWorldData.get(world.getMinecraftServer());
        TardisRecord record = data.getById(tardisId);
        if (
            record != null
                && !record.isInFlight()
                && matches(record.getExteriorPosition(), world, pos)
        ) {
            data.setExteriorPosition(tardisId, null);
        }
    }

    private static boolean matches(DimensionPosition position, World world, BlockPos pos) {
        return position != null
            && position.getDimension() == world.provider.getDimension()
            && position.getPosition().equals(pos);
    }

    private static void send(EntityPlayer player, TextFormatting color, String text) {
        player.sendStatusMessage(
            new TextComponentString(color + "[TARDIS] " + TextFormatting.RESET + text),
            false
        );
    }
}
