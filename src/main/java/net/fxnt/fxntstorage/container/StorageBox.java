package net.fxnt.fxntstorage.container;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.block.IBE;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class StorageBox extends BaseEntityBlock implements IBE<StorageBoxEntity> {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<EnumProperties.StorageUsed> STORAGE_USED = EnumProperty.create("storage_used", EnumProperties.StorageUsed.class);
    public static final BooleanProperty VOID_UPGRADE = BooleanProperty.create("void_upgrade");

    private final int slotCount;

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
    public RenderShape getRenderShape(BlockState pState) {
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        BlockEntityType<?> type = ModBlockEntities.STORAGE_BOX_ENTITY.get();
        StorageBoxEntity blockEntity = new StorageBoxEntity(type, pPos, pState);
        blockEntity.initializeEntity(slotCount);
        return blockEntity;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, ModBlockEntities.STORAGE_BOX_ENTITY.get(),
                (pLevel1, pPos, pState1, pBlockEntity) -> pBlockEntity.tick(pLevel1, pPos, pState1));
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof StorageBoxEntity be) {
            if (pStack.hasCustomHoverName()) {
                be.setCustomName(pStack.getHoverName());
            }
            CompoundTag tag = pStack.getTag();
            if (tag != null && tag.contains("BlockEntityTag", CompoundTag.TAG_COMPOUND)) {
                CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
                SortOrder order = (blockEntityTag.contains("SortOrder")) ? SortOrder.valueOf(blockEntityTag.getString("SortOrder")) : SortOrder.COUNT;
                be.setSortOrder(order);
            }
            be.forceNextTick();
        }
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        IBE.onRemove(pState, pLevel, pPos, pNewState);
        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    private boolean hitFront(BlockState blockState, BlockHitResult hit) {
        Direction side = hit.getDirection();
        return blockState.getValue(FACING) == side;
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pPlayer.isSpectator() || pHand == InteractionHand.OFF_HAND || !hitFront(pState, pHit))
            return InteractionResult.PASS;
        if (pLevel.isClientSide) return InteractionResult.SUCCESS;

            /*
                Single-click: insert 1 stack from main hand
                Single-click (sneaking): open GUI screen
                Single-click (wrench): toggle void mode?

                Double-click: insert every item in player inventory matching filter if hand is EMPTY
                Double-click (sneaking): <nothing>
             */
        BlockEntity entity = pLevel.getBlockEntity(pPos);

        if (entity instanceof StorageBoxEntity storageBoxEntity) {
            ItemStack itemInHand = pPlayer.getItemInHand(InteractionHand.MAIN_HAND);

            long currentTime = pPlayer.level().getGameTime();
            CompoundTag pd = pPlayer.getPersistentData();

            boolean isDoubleClick = (currentTime - pd.getLong("fxntstorage:last_click_time")) < 10
                    && pd.getInt("fxntstorage:last_click_type") == 1
                    && pd.getLong("fxntstorage:last_block_pos") == storageBoxEntity.getBlockPos().asLong();

            if (isDoubleClick) {
                pd.putInt("fxntstorage:last_click_type", 0);
                pd.remove("fxntstorage:last_block_pos");
                // Double Right-click
                if (itemInHand.isEmpty() && !storageBoxEntity.getFilter().getFilter().isEmpty()) {
                    storageBoxEntity.transferToStorage(pState, pPlayer, true);
                }
            } else {
                pd.putLong("fxntstorage:last_click_time", currentTime);
                pd.putLong("fxntstorage:last_block_pos", storageBoxEntity.getBlockPos().asLong());
                pd.putInt("fxntstorage:last_click_type", 1);
                // Single Right-Click
                if (itemInHand.is(AllTags.AllItemTags.WRENCH.tag)) {
                    // Right-Click with Create Wrench in hand will toggle void mode
                    storageBoxEntity.toggleVoidUpgrade();
                    return InteractionResult.SUCCESS;
                }

                if (pPlayer.isShiftKeyDown()) {
                    // Single Right-click while sneaking will open container GUI screen
                    NetworkHooks.openScreen(((ServerPlayer) pPlayer), (MenuProvider) entity, pPos);
                    return InteractionResult.CONSUME;
                }

                if (!itemInHand.isEmpty()) {
                    // Current item in player hand will be inserted into container
                    storageBoxEntity.transferToStorage(pState, pPlayer, false);
                }
            }
        }

        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public void attack(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
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
        CompoundTag tag = pContext.getItemInHand().getTag();
        boolean voidUpgrade = tag != null && tag.getCompound("BlockEntityTag").getBoolean("voidUpgrade");
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite()).setValue(VOID_UPGRADE, voidUpgrade);
    }

    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING, STORAGE_USED, VOID_UPGRADE);
    }

    @Override
    public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof StorageBoxEntity entity) {
            float percentFull = entity.calculatePercentageUsed() / 100;
            return (int) Math.min(percentFull * 15, 15);
        }
        return 0;
    }

    public static BlockHitResult rayTraceEyes(Level level, Player player, BlockPos blockPos) {
        Vec3 eyePos = player.getEyePosition(1);
        Vec3 lookVector = player.getViewVector(1);
        Vec3 endPos = eyePos.add(lookVector.scale(eyePos.distanceTo(Vec3.atCenterOf(blockPos)) + 1));
        ClipContext context = new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        return level.clip(context);
    }

}
