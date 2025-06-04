package net.fxnt.fxntstorage.container;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.block.IBE;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.fxnt.fxntstorage.init.ModDataComponents;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class StorageBox extends BaseEntityBlock implements IBE<StorageBoxEntity> {
    public static final MapCodec<StorageBox> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BlockBehaviour.propertiesCodec(),
                    Codec.INT.fieldOf("slot_count").forGetter(StorageBox::getSlotCount)
            ).apply(instance, StorageBox::new));
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<EnumProperties.StorageUsed> STORAGE_USED = EnumProperty.create("storage_used", EnumProperties.StorageUsed.class);
    public static final BooleanProperty VOID_UPGRADE = BooleanProperty.create("void_upgrade");

    private final int slotCount;

    private long lastClickTime;
    private UUID lastClickUUID;

    public StorageBox(Properties pProperties, int slotCount) {
        super(pProperties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(STORAGE_USED, EnumProperties.StorageUsed.EMPTY)
                .setValue(VOID_UPGRADE, false)
        );
        this.slotCount = slotCount;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public int getSlotCount() {
        return slotCount;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public Class<StorageBoxEntity> getBlockEntityClass() {
        return StorageBoxEntity.class;
    }

    @Override
    public BlockEntityType<? extends StorageBoxEntity> getBlockEntityType() {
        return ModBlockEntities.STORAGE_BOX_ENTITY.get();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        StorageBoxEntity storageBoxEntity = new StorageBoxEntity(ModBlockEntities.STORAGE_BOX_ENTITY.get(), blockPos, blockState);
        storageBoxEntity.initializeEntity(slotCount);
        return storageBoxEntity;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, ModBlockEntities.STORAGE_BOX_ENTITY.get(),
                (pLevel1, pPos, pState1, pBlockEntity) -> pBlockEntity.tick(pLevel1, pPos, pState1));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof StorageBoxEntity be) {
            if (stack.has(DataComponents.CUSTOM_NAME)) {
                be.setCustomName(stack.getHoverName());
            }
            SortOrder order = Optional.ofNullable(stack.get(ModDataComponents.INVENTORY_SORT_ORDER)).orElse(SortOrder.COUNT);
            be.setSortOrder(order);
            be.lastTick = 999;
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!state.is(newState.getBlock()))
            level.invalidateCapabilities(pos);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack item = new ItemStack(this);

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StorageBoxEntity sbe) {
            item.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(sbe.getStacks()));
        }
        return item;
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState pState) {
        return true;
    }

    private boolean hitFront(BlockState blockState, BlockHitResult hit) {
        Direction side = hit.getDirection();
        return blockState.getValue(FACING) == side;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (!hitFront(state, hitResult)) return InteractionResult.PASS;

            /*
                Single-click: insert 1 stack from main hand
                Single-click (sneaking): open GUI screen
                Single-click (wrench): toggle void mode?

                Double-click: insert every item in player inventory matching filter if hand is EMPTY
                Double-click (sneaking): <nothing>
             */
            BlockEntity entity = level.getBlockEntity(pos);

            if (entity instanceof StorageBoxEntity storageBoxEntity) {
                ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

                final int INTERACTION_COOLDOWN = 10; // measured in ticks
                if (level.getGameTime() - lastClickTime < INTERACTION_COOLDOWN && player.getUUID().equals(lastClickUUID)) {
                    // Double Right-click
                    if (itemInHand.isEmpty()) {
                        storageBoxEntity.transferToStorage(state, level, player, true);
                    }
                } else {
                    // Single Right-Click
                    if (itemInHand.is(AllTags.AllItemTags.WRENCH.tag)) {
                        // Right-Click with Create Wrench in hand will toggle void mode
                        storageBoxEntity.toggleVoidUpgrade();
                        return InteractionResult.SUCCESS;
                    }

                    if (player.isShiftKeyDown()) {
                        // Single Right-click while sneaking will open container GUI screen
                        player.openMenu(storageBoxEntity, pos);
                        return InteractionResult.CONSUME;
                    }

                    if (!itemInHand.isEmpty()) {
                        // Current item in player hand will be inserted into container
                        storageBoxEntity.transferToStorage(state, level, player, false);
                    }
                }

                lastClickTime = level.getGameTime();
                lastClickUUID = player.getUUID();
            }

        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void attack(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull Player pPlayer) {
        BlockHitResult hit = rayTraceEyes(pLevel, pPlayer, pPos);
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(pPos) || !hitFront(pState, hit)) {
            return;
        }

        /*
            Single-click: extract 1 item from container (matching filter) or from the first non-empty slot available
            Single-click (sneaking): extract 1 stack from container (matching filter) or from the first non-empty slot available

            Double-click: <nothing>
            Double-click (sneaking): <nothing>
        */
        StorageBoxEntity blockEntity = (StorageBoxEntity) pLevel.getBlockEntity(pPos);
        Item item = pPlayer.getItemInHand(InteractionHand.MAIN_HAND).getItem();
        if (blockEntity != null && !(item instanceof PickaxeItem || item instanceof AxeItem)) {
            blockEntity.transferFromStorage(pPlayer);
        }
    }

    @Nullable
    public static Direction getDirectionFacing(BlockState state) {
        if (!(state.getBlock() instanceof StorageBox)) return null;
        return ((StorageBox) state.getBlock()).getFacing(state);
    }

    protected Direction getFacing(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        boolean voidUpgrade = pContext.getItemInHand().getComponents().has(ModDataComponents.VOID_UPGRADE);
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite()).setValue(VOID_UPGRADE, voidUpgrade);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING, STORAGE_USED, VOID_UPGRADE);
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState pState, Level pLevel, @NotNull BlockPos pPos) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof StorageBoxEntity entity) {
            float percentFull = entity.calculatePercentageUsed() / 100;
            return (int) Math.min(percentFull * 15, 15);
        }
        return 0;
    }

    @NotNull
    public static BlockHitResult rayTraceEyes(@NotNull Level level, @NotNull Player player, @NotNull BlockPos blockPos) {
        Vec3 eyePos = player.getEyePosition(1);
        Vec3 lookVector = player.getViewVector(1);
        Vec3 endPos = eyePos.add(lookVector.scale(eyePos.distanceTo(Vec3.atCenterOf(blockPos)) + 1));
        ClipContext context = new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        return level.clip(context);
    }

}
