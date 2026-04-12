package net.fxnt.fxntstorage.controller;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.fxnt.fxntstorage.network.packet.StorageNetworkHighlightPacket;
import net.fxnt.fxntstorage.network.packet.StorageNetworkSyncPacket;
import net.fxnt.fxntstorage.storage_network.StorageNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class StorageController extends BaseEntityBlock implements IWrenchable {
    public static final MapCodec<StorageController> CODEC = simpleCodec(StorageController::new);
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
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        BlockEntityType<?> type = ModBlockEntities.STORAGE_CONTROLLER_ENTITY.get();
        return new StorageControllerEntity(type, pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.STORAGE_CONTROLLER_ENTITY.get(),
                (world, blockPos, blockState, entity) -> entity.serverTick(world, blockPos, blockState));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isSpectator() || !hitFront(state, hitResult)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof StorageControllerEntity controllerEntity && state.getValue(CONNECTED)) {
            ItemStack handItem = player.getMainHandItem();

            long currentTime = player.level().getGameTime();
            ClickData data = CLICK_DATA.computeIfAbsent(player, p -> new ClickData());
            boolean isDoubleClick = currentTime - data.lastClickTime < 10
                    && data.lastBlockPos == pos;

            if (isDoubleClick) {
                data.lastClickTime = 0;
            } else {
                data.lastClickTime = currentTime;
                data.lastBlockPos = pos;

                if (handItem.is(Tags.Items.TOOLS_WRENCH)) {
                    boolean enabled = controllerEntity.toggleHighlight((ServerPlayer) player);

                    StorageNetwork network = controllerEntity.getConnectedNetwork();
                    Set<BlockPos> components = network.getComponents();

                    if (enabled)
                        PacketDistributor.sendToPlayer((ServerPlayer) player, new StorageNetworkSyncPacket(pos, components));
                    PacketDistributor.sendToPlayer((ServerPlayer) player, new StorageNetworkHighlightPacket(pos, enabled));

                    player.displayClientMessage(
                            Component.translatable(enabled ? "fxntstorage.storage_controller.highlight_enabled" : "fxntstorage.storage_controller.highlight_disabled")
                                    .withStyle(ChatFormatting.YELLOW),
                            true
                    );
                } else {
                    controllerEntity.transferItemsFromPlayer(player);
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        PacketDistributor.sendToAllPlayers(new StorageNetworkHighlightPacket(pos, false));
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
