package net.fxnt.fxntstorage.containers;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.fxnt.fxntstorage.containers.util.EnumProperties;
import net.fxnt.fxntstorage.init.ModBlockEntities;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("deprecation")
public class StorageBox extends BaseEntityBlock implements IWrenchable {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<EnumProperties.StorageUsed> STORAGE_USED = EnumProperty.create("storage_used", EnumProperties.StorageUsed.class);
    public static final BooleanProperty VOID_UPGRADE = BooleanProperty.create("void_upgrade");

    private final String title;
    private final int slotCount;

    private long lastClickTime;
    private UUID lastClickUUID;

    public StorageBox(int slotCount, String title) {
        super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK));
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(STORAGE_USED, EnumProperties.StorageUsed.EMPTY)
                .setValue(VOID_UPGRADE, false));
        this.slotCount = slotCount;
        this.title = "container.fxntstorage." + title;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        StorageBoxEntity blockEntity = new StorageBoxEntity(pPos, pState);
        blockEntity.initializeEntity(title, slotCount);
        return blockEntity;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, ModBlockEntities.STORAGE_BOX_ENTITY.get(),
                (pLevel1, pPos, pState1, pBlockEntity) -> pBlockEntity.tick(pLevel1, pPos, pState1));
    }

    @Override
    public void setPlacedBy(@NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        if (pStack.hasCustomHoverName()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof StorageBoxEntity) {
                ((StorageBoxEntity) blockEntity).getDisplayName();
            }
        }
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
    public @NotNull InteractionResult use(@NotNull BlockState pState, Level pLevel, @NotNull BlockPos pPos, @NotNull Player pPlayer, @NotNull InteractionHand pHand, @NotNull BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            if (pHand == InteractionHand.OFF_HAND) return InteractionResult.SUCCESS;
            if (!hitFront(pState, pHit)) return InteractionResult.PASS;

            /*
                Single-click: insert 1 stack from main hand
                Single-click (sneaking): open GUI screen
                Single-click (wrench): toggle void mode?

                Double-click: insert every item in player inventory matching filter if hand is EMPTY
                Double-click (sneaking): <nothing>
             */

            BlockEntity entity = pLevel.getBlockEntity(pPos);

            if (entity instanceof StorageBoxEntity) {
                ItemStack itemInHand = pPlayer.getItemInHand(InteractionHand.MAIN_HAND);

                final int INTERACTION_COOLDOWN = 10; // measured in ticks
                if (pLevel.getGameTime() - lastClickTime < INTERACTION_COOLDOWN && pPlayer.getUUID().equals(lastClickUUID)) {
                    // Double Right-click
                    if (itemInHand.isEmpty()) {
                        ((StorageBoxEntity) entity).transferToStorage(pState, pLevel, pPlayer, true);
                    }
                } else {
                    // Single Right-Click
                    if (itemInHand.is(AllTags.AllItemTags.WRENCH.tag)) {
                        // Right-Click with Create Wrench in hand will toggle void mode
                        ((StorageBoxEntity) entity).toggleVoidUpgrade();
                        return InteractionResult.SUCCESS;
                    }

                    if (pPlayer.isShiftKeyDown()) {
                        // Single Right-click while sneaking will open container GUI screen
                        NetworkHooks.openScreen(((ServerPlayer) pPlayer), (MenuProvider) entity, pPos);
                        return InteractionResult.CONSUME;
                    }

                    if (!itemInHand.isEmpty()) {
                        // Current item in player hand will be inserted into container
                        ((StorageBoxEntity) entity).transferToStorage(pState, pLevel, pPlayer, false);
                    }
                }

                lastClickTime = pLevel.getGameTime();
                lastClickUUID = pPlayer.getUUID();

            }

        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
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
        CompoundTag tag = pContext.getItemInHand().getTag();
        boolean voidUpgrade = tag != null && tag.getCompound("BlockEntityTag").getBoolean("voidUpgrade");
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite()).setValue(VOID_UPGRADE, voidUpgrade);
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) {
        return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
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
            double percentFull = (double) entity.calculatePercentageUsed() / 100;
            return (int) Math.min(percentFull * 15, 15);
        }
        return 0;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        // Prevent the wrench from rotating the storage box
        return InteractionResult.SUCCESS;
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
