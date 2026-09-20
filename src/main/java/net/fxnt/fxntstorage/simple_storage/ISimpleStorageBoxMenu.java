package net.fxnt.fxntstorage.simple_storage;

import net.minecraft.world.item.ItemStack;

public interface ISimpleStorageBoxMenu {
    ItemStack getFilterItem();

    int getStoredAmount();

    int getMaxItemCapacity();

    boolean getVoidUpgrade();

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
