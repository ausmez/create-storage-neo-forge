package net.fxnt.fxntstorage.simple_storage;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllTags;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.fxnt.fxntstorage.init.ModTags;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class SimpleStorageBox extends BaseEntityBlock {
    public static final MapCodec<SimpleStorageBox> CODEC = simpleCodec(SimpleStorageBox::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<EnumProperties.StorageUsed> STORAGE_USED = EnumProperty.create("storage_used", EnumProperties.StorageUsed.class);

    private long lastClickTime;
    private UUID lastClickUUID;
    private long lastAttackTime;
    private UUID lastAttackUUID;

    public SimpleStorageBox(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(STORAGE_USED, EnumProperties.StorageUsed.EMPTY)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        BlockEntityType<?> type = ModBlockEntities.SIMPLE_STORAGE_BOX_ENTITY.get();
        return new SimpleStorageBoxEntity(type, pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.SIMPLE_STORAGE_BOX_ENTITY.get(), (type, world, pos, entity) -> {
            if (entity instanceof SimpleStorageBoxEntity) {
                entity.serverTick(type);
            }
        });
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) level.invalidateCapabilities(pos);
        ((SimpleStorageBoxEntity) Objects.requireNonNull(level.getBlockEntity(pos))).forceTick();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!state.is(newState.getBlock()))
            level.invalidateCapabilities(pos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SimpleStorageBoxEntity be) {
            if (stack.has(DataComponents.CUSTOM_NAME)) {
                be.getDisplayName();
            }
            be.forceTick();
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isSpectator() || !hitFront(state, hitResult)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SimpleStorageBoxEntity simpleStorageBoxEntity) {

            ItemStack handItem = player.getItemInHand(InteractionHand.MAIN_HAND);

            long INTERACTION_COOLDOWN = 8; // 8 ticks is generally equivalent to ~400ms
            if (level.getGameTime() - lastClickTime < INTERACTION_COOLDOWN && player.getUUID().equals(lastClickUUID)) {
                // Double Right-click
                if (handItem.isEmpty() || ItemStack.isSameItemSameComponents(handItem, simpleStorageBoxEntity.filterItem)) {
                    simpleStorageBoxEntity.transferToStorage(player, true);
                }
            } else {
                if (handItem.isEmpty()) {
                    if (player.isShiftKeyDown()) {
                        // If interacting with an empty hand while sneaking then open menu
                        player.openMenu(simpleStorageBoxEntity, pos);
                        return InteractionResult.CONSUME;
                    }
                } else if (handItem.is(AllTags.AllItemTags.WRENCH.tag) && simpleStorageBoxEntity.getStoredAmount() == 0 && !simpleStorageBoxEntity.filterItem.isEmpty()) {
                    // If box empty, holding wrench & has filter item then remove filter
                    simpleStorageBoxEntity.removeFilter();
                } else {
                    // Prevent wrench from being placed in the filter
                    if (!handItem.is(AllTags.AllItemTags.WRENCH.tag)) {
                        // Set filter if item is not an upgrade or empty hand and no items exist and no filter exists
                        if (!handItem.isEmpty() && !handItem.is(ModTags.Items.STORAGE_BOX_UPGRADE) && simpleStorageBoxEntity.getStoredAmount() == 0 && simpleStorageBoxEntity.filterItem.isEmpty()) {
                            simpleStorageBoxEntity.setFilter(handItem);
                        }

                        // Transfer items from player to box
                        simpleStorageBoxEntity.transferToStorage(player, false);
                    }
                }
            }

            lastClickTime = level.getGameTime();
            lastClickUUID = player.getUUID();

            simpleStorageBoxEntity.setChanged();
        }
        return InteractionResult.PASS;
    }

    @Override
    public void attack(BlockState blockState, Level level, BlockPos blockPos, Player player) {
        if (player.isSpectator() || level.isClientSide) return;

        BlockHitResult hit = rayTraceEyes(level, player, blockPos);
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(blockPos)) return;
        if (!hitFront(blockState, hit)) return;

        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        Item item = player.getItemInHand(InteractionHand.MAIN_HAND).getItem();
        if (blockEntity instanceof SimpleStorageBoxEntity storageBoxEntity && !(item instanceof AxeItem)) {
            // Small cooldown to prevent double-extraction
            if (lastAttackTime == 0 || level.getGameTime() - lastAttackTime > 1 && player.getUUID().equals(lastAttackUUID)) {
                storageBoxEntity.transferFromStorage(player);
            }

            lastAttackTime = level.getGameTime();
            lastAttackUUID = player.getUUID();
        }
    }

    private boolean hitFront(BlockState blockState, BlockHitResult hit) {
        Direction side = hit.getDirection();
        return blockState.getValue(FACING) == side;
    }

    public static BlockHitResult rayTraceEyes(Level level, Player player, BlockPos blockPos) {
        Vec3 eyePos = player.getEyePosition(1);
        Vec3 lookVector = player.getViewVector(1);
        Vec3 endPos = eyePos.add(lookVector.scale(eyePos.distanceTo(Vec3.atCenterOf(blockPos)) + 1));
        ClipContext context = new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        return level.clip(context);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING, STORAGE_USED);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof SimpleStorageBoxEntity entity) {
            double percentage = (double) entity.storedAmount / entity.maxItemCapacity;
            return (int) Math.min(percentage * 15, 15);
        }
        return 0;
    }

}
