package net.fxnt.fxntstorage.backpacks.main;

import net.fxnt.fxntstorage.backpacks.util.BackpackHandler;
import net.fxnt.fxntstorage.backpacks.util.BackpackHelper;
import net.fxnt.fxntstorage.cache.BackpackShapeCache;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class BackpackBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public final String title;
    public final int maxStackSize;

    public static final int itemSlotCount = 108;
    public static final int toolSlotCount = 24;
    public static final int upgradeSlotCount = 6;
    public static final int totalSlotCount = itemSlotCount + toolSlotCount + upgradeSlotCount;

    public BackpackBlock(String title, int maxStackSize) {
        super(Properties.copy(Blocks.WHITE_WOOL).noOcclusion());
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
        this.title = "container.fxntstorage." + title;
        this.maxStackSize = maxStackSize;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        BackpackEntity blockEntity = new BackpackEntity(pPos, pState);
        blockEntity.setData(totalSlotCount, maxStackSize);
        return blockEntity;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, ModBlockEntities.BACK_PACK_ENTITY.get(), (type, wor, pos, entity) -> entity.serverTick(type));
    }

    @Override
    public void setPlacedBy(@NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull BlockState pState, @Nullable LivingEntity pPlacer, @NotNull ItemStack pStack) {
        if (pStack.hasCustomHoverName()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof BackpackEntity) {
                ((BackpackEntity) blockEntity).setCustomName(pStack.getHoverName());
            }
        }
    }

    public static int getSlotCount() {
        return totalSlotCount;
    }

    public static int getItemSlotCount() {
        return itemSlotCount;
    }

    public static int getToolSlotCount() {
        return toolSlotCount;
    }

    public static int getUpgradeSlotCount() {
        return upgradeSlotCount;
    }

    public int getMaxStackSize() {
        return this.maxStackSize;
    }

    public ItemStack saveEntityToStack(@NotNull BackpackEntity blockEntity, ItemStack itemStack) {
        itemStack = blockEntity.saveToItemStack(itemStack);
        return itemStack;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull Player pPlayer, @NotNull InteractionHand pHand, @NotNull BlockHitResult pHit) {
        if (pLevel.isClientSide() || pHand == InteractionHand.OFF_HAND) return InteractionResult.SUCCESS;

        if (pPlayer.isCrouching() && !BackpackHelper.isWearingBackpack(pPlayer)) {
            // Equip the backpack to the back or chest slot
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (!(blockEntity instanceof BackpackEntity backPackEntity)) {
                return InteractionResult.FAIL;
            }

            pLevel.playSound(null, pPlayer.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.5F, 1.0F);

            ItemStack itemStack = new ItemStack(BackpackItem.byBlock(this));
            itemStack = saveEntityToStack(backPackEntity, itemStack);
            pLevel.removeBlock(pPos, false);

            boolean equipped = BackpackHelper.equipBackpack(pPlayer, itemStack);

            return (equipped) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }

        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof BackpackEntity backPackEntity) {
            BackpackHandler.openBackpackFromBlock((ServerPlayer) pPlayer, backPackEntity);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return BackpackShapeCache.getShape(state.getValue(FACING));
    }

    @Override
    public @NotNull VoxelShape getInteractionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return BackpackShapeCache.getShape(state.getValue(FACING));
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return BackpackShapeCache.getShape(state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING);
    }

}
