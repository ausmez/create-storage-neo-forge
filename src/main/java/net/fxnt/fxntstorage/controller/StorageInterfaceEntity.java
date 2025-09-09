package net.fxnt.fxntstorage.controller;

import net.fxnt.fxntstorage.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.EmptyHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class StorageInterfaceEntity extends BaseContainerBlockEntity {
    private int tickCount = 0;
    public StorageControllerEntity controller = null;

    private final LazyOptional<IItemHandlerModifiable> lazyItemHandler = LazyOptional.of(() -> new StorageInterfaceHandler(this));

    public StorageInterfaceEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
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

    public void setController(StorageControllerEntity controller) {
        // Check if already has controller to prevent switching networks constantly
        if (!checkController()) {
            this.controller = controller;
        }
    }

    private boolean checkController() {
        // Check controller still exists
        if (controller != null) {
            BlockEntity controllerCheck = Objects.requireNonNull(this.getLevel()).getBlockEntity(controller.getBlockPos());
            return controllerCheck == controller;
        }
        return false;
    }

    public void forgetController() {
        controller = null;
    }

    public void serverTick(Level level) {
        if (level.isClientSide) return;

        if (tickCount++ < ConfigManager.CommonConfig.SIMPLE_STORAGE_NETWORK_UPDATE_TIME.get()) return;
        tickCount = 0;

        if (controller != null && !checkController()) {
            forgetController();
        }
    }

    public IItemHandlerModifiable getItemHandler() {
        return controller != null ? controller.getItemHandler() : new EmptyHandler();
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
        IItemHandlerModifiable handler = getItemHandler();
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return getItemHandler().getStackInSlot(slot);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return controller != null && controller.canPlaceItem(slot, itemStack);
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        getItemHandler().setStackInSlot(slot, itemStack);
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

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.empty();
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new AbstractContainerMenu(null, i) {
            @Override
            public @NotNull ItemStack quickMoveStack(Player pPlayer, int pIndex) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player pPlayer) {
                return false;
            }
        };
    }

    private record StorageInterfaceHandler(
            StorageInterfaceEntity storageInterfaceEntity) implements IItemHandlerModifiable {

        private IItemHandlerModifiable get() {
            return storageInterfaceEntity.getItemHandler();
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
