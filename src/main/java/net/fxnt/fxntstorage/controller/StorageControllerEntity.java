package net.fxnt.fxntstorage.controller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.math.AngleHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.storage_network.StorageNetwork;
import net.fxnt.fxntstorage.util.DoubleClickType;
import net.fxnt.fxntstorage.util.Icons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;

import java.util.*;

import static net.fxnt.fxntstorage.controller.StorageController.CONNECTED;

public class StorageControllerEntity extends SmartBlockEntity {
    protected ScrollOptionBehaviour<FillEmptyStorage> fillEmptyStorage;
    private final Set<UUID> highlightPlayers = new HashSet<>();

    private static class ClickData {
        long lastClickTime;
        DoubleClickType lastClickType;
    }
    private final Map<Player, ClickData> CLICK_DATA = new WeakHashMap<>();

    private StorageNetwork storageNetwork;

    public StorageControllerEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        getStorageNetwork();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        fillEmptyStorage = new ScrollOptionBehaviour<>(FillEmptyStorage.class,
                Component.translatable(FXNTStorage.MOD_ID + ".storage_controller.fill_empty_storage"), this, new FillEmptyStorageValueBox());
        fillEmptyStorage.value = 0;
        behaviours.add(fillEmptyStorage);
    }

    public void getStorageNetwork() {
        storageNetwork = new StorageNetwork(this);
    }

    public StorageNetwork getConnectedNetwork() {
        return storageNetwork;
    }

    public IItemHandlerModifiable getItemHandler() {
        return storageNetwork != null ? storageNetwork.getItemHandler() : new EmptyItemHandler();
    }

    public void serverTick(Level level, BlockPos blockPos) {
        if (level.isClientSide) return;

        if (storageNetwork != null) {
            storageNetwork.tick();

            BlockState blockState = getBlockState();
            boolean isConnected = !storageNetwork.getBoxes().isEmpty();

            if (blockState.getValue(CONNECTED) != isConnected) {
                level.setBlockAndUpdate(blockPos, blockState.setValue(CONNECTED, isConnected));
            }
        }
    }

    public void transferItemsFromPlayer(Player player) {
        ItemStack handItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (handItem.is(Tags.Items.TOOLS_WRENCH))
            return;

        long currentTime = player.level().getGameTime();
        ClickData data = CLICK_DATA.computeIfAbsent(player, p -> new ClickData());
        boolean isDoubleClick = currentTime - data.lastClickTime < 10
                && data.lastClickType == DoubleClickType.EMPTY_HAND;

        if (handItem.isEmpty()) {
            if (isDoubleClick) {
                transferAllItemsFromPlayer(player);
                data.lastClickTime = 0;
            } else {
                data.lastClickTime = currentTime;
            }
            data.lastClickType = DoubleClickType.EMPTY_HAND;
        } else {
            doTransferItemsFromPlayer(player, handItem);
            data.lastClickType = DoubleClickType.WITH_ITEM;
            data.lastClickTime = currentTime;
        }
    }

    public void transferAllItemsFromPlayer(Player player) {
        for (ItemStack slotStack : player.getInventory().items) {
            if (storageNetwork.isItemInNetwork(slotStack))
                doTransferItemsFromPlayer(player, slotStack);
        }
    }

    private void doTransferItemsFromPlayer(Player player, ItemStack srcStack) {
        storageNetwork.insertItems(srcStack);
        player.getInventory().setChanged();
    }

    private static class FillEmptyStorageValueBox extends CenteredSideValueBoxTransform {
        public FillEmptyStorageValueBox() {
            super((s, d) -> d == Direction.UP);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            super.rotate(level, pos, state, ms);
            Direction facing = state.getValue(StorageController.FACING);
            TransformStack.of(ms)
                    .rotateZDegrees(-AngleHelper.horizontalAngle(facing));
        }

        @Override
        public int getOverrideColor() {
            return 0x592424;
        }
    }

    public enum FillEmptyStorage implements INamedIconOptions {
        ALLOW_EMPTY_FILL(Icons.I_ALLOW_EMPTY_FILL),
        DENY_EMPTY_FILL(Icons.I_DENY_EMPTY_FILL);

        private final String translationKey;
        private final AllIcons icon;

        FillEmptyStorage(AllIcons icon) {
            this.icon = icon;
            this.translationKey = FXNTStorage.MOD_ID + ".storage_controller." + Lang.asId(name());
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }

    public boolean toggleHighlight(ServerPlayer player) {
        UUID id = player.getUUID();
        if (highlightPlayers.contains(id)) {
            highlightPlayers.remove(id);
            return false; // OFF
        } else {
            highlightPlayers.add(id);
            return true; // ON
        }
    }

    public Set<UUID> getAllHighlightingPlayers() {
        return highlightPlayers;
    }
}
