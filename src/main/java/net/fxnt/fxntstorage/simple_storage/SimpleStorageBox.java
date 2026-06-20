package net.fxnt.fxntstorage.simple_storage;

import com.mojang.serialization.MapCodec;
import net.fxnt.fxntstorage.container.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

public class SimpleStorageBox extends BaseEntityBlock {
    public static final MapCodec<SimpleStorageBox> CODEC = simpleCodec(SimpleStorageBox::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<EnumProperties.StorageUsed> STORAGE_USED = EnumProperty.create("storage_used", EnumProperties.StorageUsed.class);
    public static final BooleanProperty COMPACTING = BooleanProperty.create("compacting");
    public static final BooleanProperty VOID_UPGRADE = BooleanProperty.create("void_upgrade");

    private static class ClickData {
        long lastClickTime;
        long lastAttackTime;
        BlockPos lastBlockPos;
    }

    private final Map<Player, ClickData> CLICK_DATA = new WeakHashMap<>();

    public SimpleStorageBox(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(STORAGE_USED, EnumProperties.StorageUsed.EMPTY)
                .setValue(COMPACTING, false)
                .setValue(VOID_UPGRADE, false)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> dropConsumer) {
        if (!state.isAir() && level instanceof ServerLevel serverLevel) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            LootParams.Builder lootParams = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, explosion.getDirectSourceEntity());
            state.getDrops(lootParams).forEach(drop -> dropConsumer.accept(drop, pos));
        }
        state.onBlockExploded(level, pos, explosion);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        BlockEntityType<?> type = ModBlockEntities.SIMPLE_STORAGE_BOX_ENTITY.get();
        return new SimpleStorageBoxEntity(type, blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.SIMPLE_STORAGE_BOX_ENTITY.get(),
                (world, blockPos, blockState, entity) -> entity.serverTick(world, blockPos, blockState));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) level.invalidateCapabilities(pos);
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
            be.initBlockState(level);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isSpectator() || !hitFront(state, hitResult)) return InteractionResult.PASS;
        if (level.isClientSide) {
            if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() && !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }

        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SimpleStorageBoxEntity simpleStorageBoxEntity) {
            ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);

            long currentTime = player.level().getGameTime();
            ClickData data = CLICK_DATA.computeIfAbsent(player, p -> new ClickData());
            boolean isDoubleClick = currentTime - data.lastClickTime < 10
                    && data.lastBlockPos == simpleStorageBoxEntity.getBlockPos();

            if (isDoubleClick) {
                // Double Right-click
                if (mainHandItem.isEmpty() || ItemStack.isSameItemSameComponents(mainHandItem, simpleStorageBoxEntity.filterItem)) {
                    simpleStorageBoxEntity.transferToStorage(player, true);
                }
                data.lastClickTime = 0;
            } else {
                data.lastClickTime = currentTime;
                data.lastBlockPos = simpleStorageBoxEntity.getBlockPos();

                if (mainHandItem.isEmpty()) {
                    if (player.isShiftKeyDown()) {
                        // If interacting with an empty hand while sneaking then open menu
                        player.openMenu(simpleStorageBoxEntity, pos);
                        return InteractionResult.CONSUME;
                    }
                } else if (mainHandItem.is(Tags.Items.TOOLS_WRENCH) && simpleStorageBoxEntity.getStoredAmount() == 0 && !simpleStorageBoxEntity.filterItem.isEmpty()) {
                    // If box empty, holding wrench & has filter item then remove filter
                    simpleStorageBoxEntity.removeFilter();
                } else {
                    // Prevent wrench from being placed in the filter
                    if (!mainHandItem.is(Tags.Items.TOOLS_WRENCH)) {
                        // Set filter if item is not an upgrade or empty hand and no items exist and no filter exists
                        if (!mainHandItem.isEmpty() && !mainHandItem.is(ModTags.Items.STORAGE_BOX_UPGRADE) && simpleStorageBoxEntity.getStoredAmount() == 0 && simpleStorageBoxEntity.filterItem.isEmpty()) {
                            simpleStorageBoxEntity.setFilter(mainHandItem);
                        }

                        // Transfer items from player to box
                        simpleStorageBoxEntity.transferToStorage(player, false);
                        return InteractionResult.CONSUME;
                    }
                }
            }

            simpleStorageBoxEntity.setChanged();

            if (!isDoubleClick && mainHandItem.isEmpty()) {
                return InteractionResult.PASS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (player.isSpectator() || level.isClientSide) return;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        Direction face = Util.getAttackedBlockFace(state, level, pos, player, blockEntity);
        if (face != state.getValue(FACING)) return;

        if (!(blockEntity instanceof SimpleStorageBoxEntity simpleStorageBox)) return;

        Item item = player.getItemInHand(InteractionHand.MAIN_HAND).getItem();
        if (!(item instanceof PickaxeItem || item instanceof AxeItem)) {
            ClickData data = CLICK_DATA.computeIfAbsent(player, p -> new ClickData());
            long currentTime = level.getGameTime();

            if (currentTime - data.lastAttackTime > 1) {
                if (simpleStorageBox.compactingUpgrade && simpleStorageBox.compactingChain != null) {
                    simpleStorageBox.transferFromStorage(player, simpleStorageBox.compactingSelectedTier);
                } else {
                    simpleStorageBox.transferFromStorage(player);
                }
                data.lastAttackTime = currentTime;
            }
        }
    }

    private boolean hitFront(BlockState blockState, BlockHitResult hit) {
        Direction side = hit.getDirection();
        return blockState.getValue(FACING) == side;
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
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING, STORAGE_USED, COMPACTING, VOID_UPGRADE);
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
