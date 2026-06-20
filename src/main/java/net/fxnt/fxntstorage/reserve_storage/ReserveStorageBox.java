package net.fxnt.fxntstorage.reserve_storage;

import com.mojang.serialization.MapCodec;
import net.fxnt.fxntstorage.container.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class ReserveStorageBox extends BaseEntityBlock {
    public static final MapCodec<ReserveStorageBox> CODEC = simpleCodec(ReserveStorageBox::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<EnumProperties.StorageUsed> STORAGE_USED = EnumProperty.create("storage_used", EnumProperties.StorageUsed.class);

    public ReserveStorageBox(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(STORAGE_USED, EnumProperties.StorageUsed.EMPTY)
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReserveStorageBoxEntity(ModBlockEntities.RESERVE_STORAGE_BOX_ENTITY.get(), pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.RESERVE_STORAGE_BOX_ENTITY.get(),
                (world, blockPos, blockState, entity) -> entity.tick(world, blockPos, blockState));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ReserveStorageBoxEntity be) {
            be.initBlockState(level);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) level.invalidateCapabilities(pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!state.is(newState.getBlock())) level.invalidateCapabilities(pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isSpectator() || !hitFront(state, hitResult))
            return InteractionResult.PASS;
        if (level.isClientSide) {
            if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() && !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }

        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof ReserveStorageBoxEntity blockEntity) {
            ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);

            if (player.isShiftKeyDown()) {
                player.openMenu(blockEntity, buf -> {
                    buf.writeBoolean(false);
                    buf.writeBlockPos(pos);
                });
                return InteractionResult.FAIL;
            }

            if (!mainHandItem.isEmpty()) {
                IItemHandlerModifiable handler = blockEntity.getAutomationHandler();

                ItemStack remainder = ItemHandlerHelper.insertItem(handler, mainHandItem, false);
                player.setItemInHand(InteractionHand.MAIN_HAND, remainder);
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    private boolean hitFront(BlockState blockState, BlockHitResult hit) {
        Direction side = hit.getDirection();
        return blockState.getValue(FACING) == side;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING, STORAGE_USED));
    }
}
