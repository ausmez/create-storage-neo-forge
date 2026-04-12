package net.fxnt.fxntstorage.controller;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.StorageNetworkHighlightPacket;
import net.fxnt.fxntstorage.network.packet.StorageNetworkSyncPacket;
import net.fxnt.fxntstorage.storage_network.StorageNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@SuppressWarnings("deprecation")
public class StorageController extends BaseEntityBlock implements IWrenchable {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected_to_network");

    private static class ClickData {
        long lastClickTime;
        BlockPos lastBlockPos;
    }

    private final Map<Player, ClickData> CLICK_DATA = new WeakHashMap<>();

    public StorageController(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(CONNECTED, false));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        BlockEntityType<?> type = ModBlockEntities.STORAGE_CONTROLLER_ENTITY.get();
        return new StorageControllerEntity(type, pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.STORAGE_CONTROLLER_ENTITY.get(), (world, blockPos, blockState, entity) -> entity.serverTick(world, blockPos, blockState));
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pPlayer.isSpectator() || !hitFront(pState, pHit)) return InteractionResult.PASS;
        if (pLevel.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity entity = pLevel.getBlockEntity(pPos);
        if (entity instanceof StorageControllerEntity controllerEntity && pState.getValue(CONNECTED)) {
            ItemStack handItem = pPlayer.getMainHandItem();

            long currentTime = pPlayer.level().getGameTime();
            ClickData data = CLICK_DATA.computeIfAbsent(pPlayer, p -> new ClickData());
            boolean isDoubleClick = currentTime - data.lastClickTime < 10
                    && data.lastBlockPos == pPos;

            if (isDoubleClick) {
                data.lastClickTime = 0;
            } else {
                data.lastClickTime = currentTime;
                data.lastBlockPos = pPos;

                if (handItem.is(AllTags.AllItemTags.WRENCH.tag)) {
                    boolean enabled = controllerEntity.toggleHighlight((ServerPlayer) pPlayer);

                    StorageNetwork network = controllerEntity.getConnectedNetwork();
                    Set<BlockPos> components = network.getComponents();

                    if (enabled)
                        ModNetwork.sendToPlayer((ServerPlayer) pPlayer, new StorageNetworkSyncPacket(pPos, components));
                    ModNetwork.sendToPlayer((ServerPlayer) pPlayer, new StorageNetworkHighlightPacket(pPos, enabled));

                    pPlayer.displayClientMessage(
                            Component.translatable(enabled ? "fxntstorage.storage_controller.highlight_enabled" : "fxntstorage.storage_controller.highlight_disabled")
                                    .withStyle(ChatFormatting.YELLOW),
                            true
                    );
                } else {
                    controllerEntity.transferItemsFromPlayer(pPlayer);
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
        ModNetwork.sendToAllPlayers(new StorageNetworkHighlightPacket(pPos, false));
    }

    public boolean hitFront(BlockState blockState, BlockHitResult hit) {
        Direction side = hit.getDirection();
        return blockState.getValue(FACING) == side;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, CONNECTED);
    }

}
