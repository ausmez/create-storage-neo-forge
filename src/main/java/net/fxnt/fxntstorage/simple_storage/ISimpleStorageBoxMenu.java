package net.fxnt.fxntstorage.simple_storage;

import net.minecraft.world.item.ItemStack;

public interface ISimpleStorageBoxMenu {
    ItemStack getFilterItem();

    int getStoredAmount();

    int getMaxItemCapacity();

    boolean getVoidUpgrade();

    // Display-only values for the GUI. With a compacting upgrade these are expressed in
    // highest-tier units (e.g. iron blocks) so the player sees "32 * stack size" capacity
    // rather than the raw internal T0 (nugget) counts. Default to the raw values.
    default int getDisplayedStoredAmount() {
        return getStoredAmount();
    }

    default int getDisplayedMaxCapacity() {
        return getMaxItemCapacity();
    }

    default ItemStack getDisplayedItem() {
        return getFilterItem();
    }
}
