package net.fxnt.fxntstorage.reserve_storage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import static net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxEntity.*;

public class ReserveStorageBoxAutomationHandler implements IItemHandlerModifiable {
    private final ItemStackHandler delegate;

    public ReserveStorageBoxAutomationHandler(ItemStackHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getSlots() {
        return STORAGE_SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot >= STORAGE_SLOTS) return stack;
        return delegate.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack slotStack = delegate.getStackInSlot(slot);
        if (slotStack.isEmpty() || amount <= 0) return ItemStack.EMPTY;

        int minimum = getConfiguredMinimum(slotStack);
        if (minimum <= 0) return delegate.extractItem(slot, amount, simulate);

        int total = getTotalStorageCount(slotStack);
        int extractable = Math.max(0, total - minimum);
        if (extractable == 0) return ItemStack.EMPTY;

        return delegate.extractItem(slot, Math.min(amount, extractable), simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot >= STORAGE_SLOTS) return false;
        return delegate.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        delegate.setStackInSlot(slot, stack);
    }

    private int getConfiguredMinimum(ItemStack item) {
        for (int i = 0; i < GHOST_SLOTS; i++) {
            ItemStack ghost = delegate.getStackInSlot(GHOST_SLOT_OFFSET + i);
            if (!ghost.isEmpty() && ItemStack.isSameItemSameComponents(ghost, item)) {
                return ghost.getCount();
            }
        }
        return 0;
    }

    private int getTotalStorageCount(ItemStack item) {
        int total = 0;
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            ItemStack stack = delegate.getStackInSlot(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
