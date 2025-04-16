package net.fxnt.fxntstorage.controller;

import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.container.util.ImplementedContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class StorageInterfaceEntity extends BaseContainerBlockEntity implements ImplementedContainer {
    public int tick = 0;
    public StorageControllerEntity controller = null;

    public StorageInterfaceEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    public void setController(StorageControllerEntity controller) {
        // Check if already has controller to prevent switching networks constantly
        if (!checkController()) {
            this.controller = controller;
        }
    }

    private boolean checkController() {
        // Check controller still exists
        if (this.controller != null) {
            BlockEntity controllerCheck = Objects.requireNonNull(this.getLevel()).getBlockEntity(this.controller.getBlockPos());
            if (controllerCheck != null) {
                return controllerCheck.getBlockState().equals(this.controller.getBlockState());
            }
        }
        return false;
    }

    public void forgetController() {
        this.controller = null;
    }

    public void serverTick(Level level) {
        if (level.isClientSide) return;

        if (this.tick >= ConfigManager.CommonConfig.SIMPLE_STORAGE_NETWORK_UPDATE_TIME.get()) {
            this.tick = 0;
            if (this.controller != null && !checkController()) {
                forgetController();
            }
        }
        this.tick++;
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        if (this.controller == null) return NonNullList.create();
        return this.controller.storageNetwork.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {

    }

    @Override
    public int getContainerSize() {
        if (this.controller == null) return 0;
        return this.controller.storageNetwork.items.size();
    }

    @Override
    public int getMaxStackSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isEmpty() {
        if (this.controller == null) return true;
        int totalAmount = 0;
        for (ItemStack itemStack : this.controller.storageNetwork.items) {
            totalAmount += itemStack.getCount();
        }
        return totalAmount <= 0;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return false;
    }

    @Override
    public int @NotNull [] getSlotsForFace(Direction side) {
        if (this.controller == null) return new int[]{};
        return this.controller.getSlotsForFace(side);
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        if (this.controller == null) return ItemStack.EMPTY;
        return this.controller.storageNetwork.items.get(slot);
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack itemStack) {
        if (this.controller == null) return false;
        return this.controller.canPlaceItem(slot, itemStack);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
        return canPlaceItem(slot, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        if (this.controller == null) return false;
        return this.controller.canTakeItemThroughFace(slot, itemStack, direction);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack itemStack) {
        if (this.controller == null) return;
        this.controller.storageNetwork.setItem(slot, itemStack);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (this.controller == null) return ItemStack.EMPTY;
        return this.controller.storageNetwork.removeItem(slot, amount);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (this.controller == null) return ItemStack.EMPTY;
        return this.controller.storageNetwork.removeItemNoUpdate(slot);
    }

    @Override
    public void clearContent() {
    }

    @Override
    protected Component getDefaultName() {
        return Component.empty();
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return null;
    }

}
