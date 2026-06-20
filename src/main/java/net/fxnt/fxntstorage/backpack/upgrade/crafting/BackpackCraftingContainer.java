package net.fxnt.fxntstorage.backpack.upgrade.crafting;

import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BackpackCraftingContainer implements CraftingContainer {
    public static final int WIDTH = 3;
    public static final int HEIGHT = 3;
    private static final int SIZE = WIDTH * HEIGHT;

    private final IItemHandlerModifiable handler;
    private final int startIndex;
    private final Runnable onChanged;

    private boolean suppressNotify = false;

    public BackpackCraftingContainer(IBackpackContainer backpack, int startIndex, Runnable onChanged) {
        this.handler = backpack.getItemHandler();
        this.startIndex = startIndex;
        this.onChanged = onChanged;
    }

    public void setSuppressNotify(boolean suppress) {
        this.suppressNotify = suppress;
    }

    private void notifyChanged() {
        if (!suppressNotify) {
            onChanged.run();
        }
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SIZE; i++) {
            if (!handler.getStackInSlot(startIndex + i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return slot < 0 || slot >= SIZE ? ItemStack.EMPTY : handler.getStackInSlot(startIndex + slot);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SIZE) return ItemStack.EMPTY;
        ItemStack stack = handler.getStackInSlot(startIndex + slot);
        handler.setStackInSlot(startIndex + slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= SIZE) return ItemStack.EMPTY;
        ItemStack stack = handler.getStackInSlot(startIndex + slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = stack.split(amount);
        handler.setStackInSlot(startIndex + slot, stack);
        if (!removed.isEmpty()) notifyChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= SIZE) return;
        handler.setStackInSlot(startIndex + slot, stack);
        notifyChanged();
    }

    @Override
    public void setChanged() {
        notifyChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SIZE; i++) {
            handler.setStackInSlot(startIndex + i, ItemStack.EMPTY);
        }
        notifyChanged();
    }

    @Override
    public @NotNull List<ItemStack> getItems() {
        List<ItemStack> items = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            items.add(handler.getStackInSlot(startIndex + i));
        }
        return items;
    }

    @Override
    public void fillStackedContents(@NotNull StackedContents contents) {
        for (int i = 0; i < SIZE; i++) {
            contents.accountSimpleStack(handler.getStackInSlot(startIndex + i));
        }
    }
}
