package net.fxnt.fxntstorage.controller;

import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.storage_network.StorageNetwork;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static net.fxnt.fxntstorage.controller.StorageController.CONNECTED;

public class StorageControllerEntity extends BaseContainerBlockEntity {
    public int tick = 0;
    public long lastInteractTime = 0;
    public UUID lastInteractPlayer = UUID.randomUUID();
    public byte lastInteractType = -1;
    public int interactWindow = 600;
    public StorageNetwork storageNetwork;

    public StorageControllerEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        getStorageNetwork();
    }

    public void getStorageNetwork() {
        this.storageNetwork = new StorageNetwork(this);
    }

    public IItemHandlerModifiable getItemHandler() {
        return storageNetwork != null ? storageNetwork.getItemHandler() : new EmptyItemHandler();
    }

    @Override
    public int getContainerSize() {
        return getItemHandler().getSlots();
    }

    @Override
    public int getMaxStackSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItemHandler().getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    public void serverTick(Level level, BlockPos blockPos) {
        if (level.isClientSide) return;

        if (storageNetwork != null) {
            storageNetwork.tick();

            BlockState blockState = getBlockState();
            boolean isConnected = !storageNetwork.boxes.isEmpty();

            if (blockState.getValue(CONNECTED) != isConnected) {
                level.setBlockAndUpdate(blockPos, blockState.setValue(CONNECTED, isConnected));
            }
        }

        if (tick >= ConfigManager.CommonConfig.SIMPLE_STORAGE_NETWORK_UPDATE_TIME.get()) {
            tick = 0;
        }
        tick++;
    }

    @Override
    public ItemStack getItem(int slot) {
        return getItemHandler().getStackInSlot(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        getItemHandler().setStackInSlot(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return storageNetwork.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return storageNetwork.canTakeItem(slot, stack);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        return getItemHandler().extractItem(slot, amount, false);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return getItemHandler().extractItem(slot, Integer.MAX_VALUE, false);
    }

    @Override
    public void clearContent() {
    }

    public void transferItemsFromPlayer(Player player) {
        ItemStack handItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (Util.getMillis() < lastInteractTime + interactWindow
                && player.getUUID().equals(lastInteractPlayer)
                && lastInteractType == 1
                && handItem.isEmpty()
        ) {
            transferAllItemsFromPlayer(player);
        } else if (!handItem.isEmpty()) {
            lastInteractTime = Util.getMillis();
            lastInteractPlayer = player.getUUID();
            lastInteractType = 1;
            doTransferItemsFromPlayer(player, handItem);
        }
    }

    public void transferAllItemsFromPlayer(Player player) {
        for (int i = 0; i <= player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            doTransferItemsFromPlayer(player, slotStack);
        }
    }

    private void doTransferItemsFromPlayer(Player player, ItemStack srcStack) {
        storageNetwork.insertItems(srcStack);
        player.getInventory().setChanged();
    }

    @Override
    protected @NotNull Component getDefaultName() { // Required for BaseContainerBlockEntity
        return Component.empty();
    }

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        IItemHandlerModifiable handler = storageNetwork.getItemHandler();
        NonNullList<ItemStack> list = NonNullList.withSize(handler.getSlots(), ItemStack.EMPTY);
        for (int i = 0; i < handler.getSlots(); i++) {
            list.set(i, handler.getStackInSlot(i));
        }
        return list;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {
        IItemHandlerModifiable handler = storageNetwork.getItemHandler();
        for (int i = 0; i < nonNullList.size() && i < handler.getSlots(); i++) {
            handler.setStackInSlot(i, nonNullList.get(i));
        }
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return null;
    }

}
