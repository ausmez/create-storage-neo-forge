package net.fxnt.fxntstorage.simple_storage;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.fxnt.fxntstorage.containers.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("deprecation")
public class SimpleStorageBox extends BaseEntityBlock implements IWrenchable {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<EnumProperties.StorageUsed> STORAGE_USED = EnumProperty.create("storage_used", EnumProperties.StorageUsed.class);

    private long lastClickTime;
    private UUID lastClickUUID;

    public SimpleStorageBox(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(STORAGE_USED, EnumProperties.StorageUsed.EMPTY)
        );
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        BlockEntityType<?> type = ModBlockEntities.SIMPLE_STORAGE_BOX_ENTITY.get();
        return new SimpleStorageBoxEntity(type, pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.SIMPLE_STORAGE_BOX_ENTITY.get(), (type, world, pos, entity) -> {
            if (entity instanceof SimpleStorageBoxEntity) {
                entity.serverTick(type, world);
            }
        });
    }

    @Override
    public void setPlacedBy(@NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        if (pStack.hasCustomHoverName()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof SimpleStorageBoxEntity) {
                ((SimpleStorageBoxEntity) blockEntity).getDisplayName();
            }
        }
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, Player pPlayer, @NotNull InteractionHand pHand, @NotNull BlockHitResult pHit) {
        if (pPlayer.isSpectator() || pLevel.isClientSide || pHand == InteractionHand.OFF_HAND)
            return InteractionResult.SUCCESS;
        if (!hitFront(pState, pHit)) return InteractionResult.PASS;

        BlockEntity entity = pLevel.getBlockEntity(pPos);
        if (entity instanceof SimpleStorageBoxEntity simpleStorageBoxEntity) {

            ItemStack handItem = pPlayer.getItemInHand(InteractionHand.MAIN_HAND);

            long INTERACTION_COOLDOWN = 8; // 8 ticks is generally equivalent to ~400ms
            if (pLevel.getGameTime() - lastClickTime < INTERACTION_COOLDOWN && pPlayer.getUUID().equals(lastClickUUID)) {
                // Double Right-click
                if (handItem.isEmpty() || ItemStack.isSameItemSameTags(handItem, simpleStorageBoxEntity.filterItem)) {
                    simpleStorageBoxEntity.transferToStorage(pPlayer, true);
                }
            } else {
                if (handItem.isEmpty()) {
                    if (pPlayer.isShiftKeyDown()) {
                        // If interacting with an empty hand while sneaking then open menu
                        NetworkHooks.openScreen(((ServerPlayer) pPlayer), (MenuProvider) entity, pPos);
                        return InteractionResult.CONSUME;
                    }
                } else if (handItem.is(AllTags.AllItemTags.WRENCH.tag) && simpleStorageBoxEntity.getStoredAmount() == 0 && !simpleStorageBoxEntity.filterItem.isEmpty()) {
                    // If box empty, holding wrench & has filter item then remove filter
                    simpleStorageBoxEntity.removeFilter();
                } else {
                    // Set filter if item is not an upgrade or empty hand and no items exist and no filter exists
                    if (!handItem.isEmpty() && !handItem.is(ModTags.Items.STORAGE_BOX_UPGRADE) && simpleStorageBoxEntity.getStoredAmount() == 0 && simpleStorageBoxEntity.filterItem.isEmpty()) {
                        simpleStorageBoxEntity.setFilter(handItem);
                    }

                    // Transfer items from player to box
                    simpleStorageBoxEntity.transferToStorage(pPlayer, false);
                }
            }

            lastClickTime = pLevel.getGameTime();
            lastClickUUID = pPlayer.getUUID();

            simpleStorageBoxEntity.setChanged();

        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Override
    public void attack(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos, @NotNull Player player) {
        if (player.isSpectator() || level.isClientSide) return;

        BlockHitResult hit = rayTraceEyes(level, player, blockPos);
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(blockPos)) return;
        if (!hitFront(blockState, hit)) return;

        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        Item item = player.getItemInHand(InteractionHand.MAIN_HAND).getItem();
        if (blockEntity instanceof SimpleStorageBoxEntity storageBoxEntity && !(item instanceof AxeItem)) {
            storageBoxEntity.transferFromStorage(player);
        }
    }

    private boolean hitFront(BlockState blockState, BlockHitResult hit) {
        Direction side = hit.getDirection();
        return blockState.getValue(FACING) == side;
    }

    @NotNull
    public static BlockHitResult rayTraceEyes(@NotNull Level level, @NotNull Player player, @NotNull BlockPos blockPos) {
        Vec3 eyePos = player.getEyePosition(1);
        Vec3 lookVector = player.getViewVector(1);
        Vec3 endPos = eyePos.add(lookVector.scale(eyePos.distanceTo(Vec3.atCenterOf(blockPos)) + 1));
        ClipContext context = new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        return level.clip(context);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState pState) {
        return RenderShape.MODEL;
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
    public @NotNull BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING, STORAGE_USED);
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState pState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState pState, Level pLevel, @NotNull BlockPos pPos) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof SimpleStorageBoxEntity entity) {
            double percentage = (double) entity.storedAmount / entity.maxItemCapacity;
            return (int) Math.min(percentage * 15, 15);
        }
        return 0;
    }

    @Nullable
    public static Direction getDirectionFacing(BlockState state) {
        if (!(state.getBlock() instanceof SimpleStorageBox)) return null;
        return ((SimpleStorageBox) state.getBlock()).getFacing(state);
    }

    protected Direction getFacing(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        // Prevent the wrench from rotating the storage box
        return InteractionResult.SUCCESS;
    }
}
