package net.fxnt.fxntstorage.backpack;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.cache.BackpackShapeCache;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@SuppressWarnings("deprecation")
public class BackpackBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
    public final int stackMultiplier;

    public BackpackBlock(Properties pProperties, int stackMultiplier) {
        super(pProperties.strength(0.2f, 600.0f));
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
        this.stackMultiplier = stackMultiplier;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        BlockEntityType<?> type = ModBlockEntities.BACKPACK_ENTITY.get();
        BackpackEntity blockEntity = new BackpackEntity(type, pPos, pState);
        blockEntity.setData(layout.getTotalSlots(), stackMultiplier);
        return blockEntity;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.BACKPACK_ENTITY.get(), (world, blockPos, blockState, blockEntity) -> blockEntity.serverTick(level));
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof BackpackEntity be) {
            CompoundTag tag = pStack.getOrCreateTag().getCompound("BlockEntityTag");
            be.saveAdditional(tag);
            if (pStack.hasCustomHoverName())
                be.setCustomName(pStack.getHoverName());
            SortOrder order = (tag.contains("SortOrder")) ? SortOrder.valueOf(tag.getString("SortOrder")) : SortOrder.COUNT;
            be.setSortOrder(order);
        }
    }

    public int getStackMultiplier() {
        return stackMultiplier;
    }

    public ItemStack saveEntityToStack(BackpackEntity blockEntity, ItemStack itemStack) {
        itemStack = blockEntity.saveToItemStack(itemStack);
        return itemStack;
    }

    @Override
    public @NotNull InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide || pHand == InteractionHand.OFF_HAND) return InteractionResult.SUCCESS;

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
        if (blockEntity instanceof BackpackEntity be) {
            NetworkHooks.openScreen((ServerPlayer) pPlayer, be, buf ->
                    buf.writeEnum(BackpackMenu.BackpackType.BLOCK).writeBlockPos(pPos));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BackpackShapeCache.getShape(state.getValue(FACING));
    }

    @Override
    public @NotNull VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return BackpackShapeCache.getShape(state.getValue(FACING));
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BackpackShapeCache.getShape(state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
        if (pLevel.getBlockEntity(pPos) instanceof BackpackEntity backpackEntity) {
            return backpackEntity.calcRedstoneFromInventory();
        }
        return 0;
    }
}
