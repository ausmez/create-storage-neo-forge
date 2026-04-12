package net.fxnt.fxntstorage.container;

import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.world.inventory.Slot;

public interface ISortableStorageBox {
    int getContainerSize();

    SortOrder getSortOrder();

    void setSortOrder(SortOrder order);

    Slot getPlayerSlot(int slotIndex);

    Slot getHotbarSlot(int slotIndex);
}
