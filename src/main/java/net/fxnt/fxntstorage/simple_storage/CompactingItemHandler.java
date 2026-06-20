package net.fxnt.fxntstorage.simple_storage;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Virtual item handler exposing T0/T1/T2 slots backed by a single T0 storage count.
 * Slots are ordered highest-tier-first so automation extracts the most compact form first.
 * Slot 0 = highest tier (T2 for 3-tier chains, T1 for 2-tier chains).
 * Slot 1 = middle tier (T1 for 3-tier, T0 for 2-tier).
 * Slot 2 = T0 (3-tier chains only).
 */
public class CompactingItemHandler implements IItemHandler {
    private final SimpleStorageBoxEntity entity;
    private final CompactingChain chain;

    public CompactingItemHandler(SimpleStorageBoxEntity entity, CompactingChain chain) {
        this.entity = entity;
        this.chain = chain;
    }

    @Override
    public int getSlots() {
        return chain.tiers();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        Item item = slotItem(slot);
        if (item == null) return ItemStack.EMPTY;
        int t0Stored = entity.itemHandler.getStackInSlot(0).getCount();
        int count = t0Stored / t0PerUnit(slot);
        return count > 0 ? new ItemStack(item, count) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot >= getSlots()) return stack;

        Item tieredItem = slotItem(slot);
        if (tieredItem == null || stack.getItem() != tieredItem) return stack;

        int t0PerUnit = t0PerUnit(slot);
        int t0Units = stack.getCount() * t0PerUnit;
        int currentT0 = entity.itemHandler.getStackInSlot(0).getCount();
        int capacity = entity.maxItemCapacity;
        int space = capacity - currentT0;

        if (space <= 0) return entity.voidUpgrade ? ItemStack.EMPTY : stack;

        // Round down to whole tier units
        int canFitT0 = Math.min(t0Units, space);
        canFitT0 = (canFitT0 / t0PerUnit) * t0PerUnit;

        if (canFitT0 <= 0) return entity.voidUpgrade ? ItemStack.EMPTY : stack;

        if (!simulate) {
            ItemStack stored = entity.itemHandler.getStackInSlot(0);
            if (stored.isEmpty()) {
                entity.itemHandler.setStackInSlot(0, new ItemStack(chain.t0(), canFitT0));
            } else {
                stored.grow(canFitT0);
            }
            entity.setChanged();
        }

        int consumed = canFitT0 / t0PerUnit;
        int remaining = stack.getCount() - consumed;
        if (remaining <= 0) return ItemStack.EMPTY;
        return entity.voidUpgrade ? ItemStack.EMPTY : stack.copyWithCount(remaining);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot >= getSlots()) return ItemStack.EMPTY;
        Item item = slotItem(slot);
        if (item == null) return ItemStack.EMPTY;

        int t0PerUnit = t0PerUnit(slot);
        int t0Stored = entity.itemHandler.getStackInSlot(0).getCount();
        int available = t0Stored / t0PerUnit;
        if (available <= 0) return ItemStack.EMPTY;

        int toExtract = Math.min(amount, available);
        if (!simulate) {
            int t0Cost = toExtract * t0PerUnit;
            ItemStack stored = entity.itemHandler.getStackInSlot(0);
            stored.shrink(t0Cost);
            if (stored.isEmpty()) entity.itemHandler.setStackInSlot(0, ItemStack.EMPTY);
            entity.setChanged();
        }
        return new ItemStack(item, toExtract);
    }

    @Override
    public int getSlotLimit(int slot) {
        int t0PerUnit = t0PerUnit(slot);
        return t0PerUnit > 0 ? entity.maxItemCapacity / t0PerUnit : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        Item item = slotItem(slot);
        return item != null && stack.getItem() == item;
    }

    // slot 0 = highest tier, slot (tiers-1) = T0
    private int tierIndex(int slot) {
        return chain.tiers() - 1 - slot;
    }

    private @Nullable Item slotItem(int slot) {
        return switch (tierIndex(slot)) {
            case 0 -> chain.t0();
            case 1 -> chain.t1();
            case 2 -> chain.t2();
            default -> null;
        };
    }

    private int t0PerUnit(int slot) {
        return switch (tierIndex(slot)) {
            case 0 -> 1;
            case 1 -> chain.t0ToT1();
            case 2 -> chain.t0ToT1() * chain.t1ToT2();
            default -> 0;
        };
    }
}
