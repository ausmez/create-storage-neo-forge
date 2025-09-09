package net.fxnt.fxntstorage.controller;

import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.storage_network.StorageNetwork;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.EmptyHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static net.fxnt.fxntstorage.controller.StorageController.CONNECTED;

public class StorageControllerEntity extends BaseContainerBlockEntity {
    private static final int INTERACT_WINDOW = 600;

    private int tickCount = 0;
    private long lastInteractTime = 0;
    private UUID lastInteractPlayer = UUID.randomUUID();
    private byte lastInteractType = -1;
    public StorageNetwork storageNetwork;

    private final LazyOptional<IItemHandlerModifiable> lazyItemHandler = LazyOptional.of(() -> new StorageControllerHandler(this));

    public StorageControllerEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        getStorageNetwork();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    public void getStorageNetwork() {
        this.storageNetwork = new StorageNetwork(this);
    }

    public IItemHandlerModifiable getItemHandler() {
        return storageNetwork != null ? storageNetwork.getItemHandler() : new EmptyHandler();
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

        if (tickCount >= ConfigManager.CommonConfig.SIMPLE_STORAGE_NETWORK_UPDATE_TIME.get()) {
            tickCount = 0;
        }
        tickCount++;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return getItemHandler().getStackInSlot(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        getItemHandler().setStackInSlot(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return storageNetwork.canPlaceItem(slot, itemStack);
    }

    @Override
    public boolean canTakeItem(Container pTarget, int pIndex, ItemStack pStack) {
        return storageNetwork.canTakeItem(pIndex, pStack);
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

        if (Util.getMillis() < lastInteractTime + INTERACT_WINDOW
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
    protected @NotNull Component getDefaultName() {
        return Component.empty();
    }

    @Override
    protected AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory) {
        return new AbstractContainerMenu(null, pContainerId) {
            @Override
            public ItemStack quickMoveStack(Player player, int i) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return false;
            }
        };
    }

    private record StorageControllerHandler(
            StorageControllerEntity storageControllerEntity) implements IItemHandlerModifiable {

        private IItemHandlerModifiable get() {
            return storageControllerEntity.getItemHandler();
        }

        @Override
        public int getSlots() {
            return get().getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return get().getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return get().insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return get().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return get().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return get().isItemValid(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            get().setStackInSlot(slot, stack);
        }

    }

}
