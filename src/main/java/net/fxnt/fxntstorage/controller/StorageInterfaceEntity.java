package net.fxnt.fxntstorage.controller;

import net.fxnt.fxntstorage.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
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
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class StorageInterfaceEntity extends BaseContainerBlockEntity {
    private int tickCount = 0;
    public StorageControllerEntity controller = null;

    public StorageInterfaceEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    public void setController(StorageControllerEntity controller) {
        // Check if already has controller to prevent switching networks constantly
        if (!checkController()) {
            if (level != null) level.invalidateCapabilities(this.getBlockPos());
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
        return controller != null ? controller.getItemHandler() : new EmptyItemHandler();
    }

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        IItemHandlerModifiable handler = getItemHandler();
        NonNullList<ItemStack> list = NonNullList.withSize(handler.getSlots(), ItemStack.EMPTY);
        for (int i = 0; i < handler.getSlots(); i++) {
            list.set(i, handler.getStackInSlot(i));
        }
        return list;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {
        IItemHandlerModifiable handler = getItemHandler();
        for (int i = 0; i < handler.getSlots() && i < nonNullList.size(); i++) {
            handler.setStackInSlot(i, nonNullList.get(i));
        }
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

}
