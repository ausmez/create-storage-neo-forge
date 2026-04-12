package net.fxnt.fxntstorage.container;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Locale;

public class EnumProperties {
    public enum StorageUsed implements StringRepresentable {
        EMPTY, HAS_ITEMS, SLOTS_FILLED, FULL;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static StorageUsed calculateFillLevel(IItemHandler handler, int slotCount) {
        boolean allSlotsFull = true;
        int filledSlots = 0;

        for (int i = 0; i < slotCount; i++) {
            ItemStack slot = handler.getStackInSlot(i);
            if (!slot.isEmpty()) {
                filledSlots++;
                if (slot.getCount() < slot.getMaxStackSize()) allSlotsFull = false;
            } else {
                allSlotsFull = false;
            }
        }
        int emptySlots = slotCount - filledSlots;

        if (allSlotsFull) return StorageUsed.FULL;
        if (emptySlots == 0) return StorageUsed.SLOTS_FILLED;
        if (filledSlots > 0) return StorageUsed.HAS_ITEMS;
        return StorageUsed.EMPTY;
    }
}
