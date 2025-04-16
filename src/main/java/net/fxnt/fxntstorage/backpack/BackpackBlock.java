package net.fxnt.fxntstorage.backpack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fxnt.fxntstorage.backpack.util.BackpackHandler;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.cache.BackpackShapeCache;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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
    public static final MapCodec<BackpackBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BlockBehaviour.propertiesCodec(),
                    Codec.INT.fieldOf("max_stack_size").forGetter(BackpackBlock::getStackMultiplier)
            ).apply(instance, BackpackBlock::new));
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public final int stackMultiplier;

    public static final int itemSlotCount = 108;
    public static final int toolSlotCount = 24;
    public static final int upgradeSlotCount = 6;
    public static final int totalSlotCount = itemSlotCount + toolSlotCount + upgradeSlotCount;

    public BackpackBlock(Properties pProperties, int stackMultiplier) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
        this.stackMultiplier = stackMultiplier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        BlockEntityType<?> type = ModBlockEntities.BACKPACK_ENTITY.get();
        BackpackEntity blockEntity = new BackpackEntity(type, pPos, pState);
        blockEntity.setData(totalSlotCount, stackMultiplier);
        return blockEntity;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, ModBlockEntities.BACKPACK_ENTITY.get(), (type, wor, pos, entity) -> entity.serverTick(type));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || stack == null) return;
        if (level.getBlockEntity(pos) instanceof BackpackEntity be) {
            be.readInventory(stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
            if (stack.has(DataComponents.CUSTOM_NAME))
                be.setCustomName(stack.getHoverName());
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

    public int getStackMultiplier() {
        return this.stackMultiplier;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player.isCrouching() && !BackpackHelper.isWearingBackpack(player)) {
            // Equip the backpack to the back or chest slot
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof BackpackEntity backpackEntity)) {
                return InteractionResult.FAIL;
            }

            // TODO: Is this needed here?
            // level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.5F, 1.0F);

            ItemStack itemStack = new ItemStack(BackpackItem.byBlock(this));
            itemStack = saveEntityToStack(backpackEntity, itemStack);
            level.removeBlock(pos, false);

            boolean equipped = BackpackHelper.equipBackpack(player, itemStack);

            return (equipped) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BackpackEntity backPackEntity) {
            BackpackHandler.openBackpackFromBlock((ServerPlayer) player, backPackEntity);
        }
        return InteractionResult.CONSUME;
    }

    private ItemStack saveEntityToStack(BackpackEntity blockEntity, ItemStack itemStack) {
        itemStack = blockEntity.saveToItemStack(itemStack);
        return itemStack;
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

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BackpackEntity backpackEntity) {
            return backpackEntity.calcRedstoneFromInventory();
        }
        return 0;
    }

}
