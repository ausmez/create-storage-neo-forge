package net.fxnt.fxntstorage.compat;

import net.fxnt.fxntstorage.backpack.client.menu.slot.BackpackSlot;
import net.fxnt.fxntstorage.backpack.client.menu.slot.ToolSlot;
import net.fxnt.fxntstorage.backpack.client.menu.slot.UpgradeSlot;
import net.fxnt.fxntstorage.init.ModCompats;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.InterModComms;

public class InventorySorterCompat {
    private static final String CONTAINER_BLACKLIST = "containerblacklist";
    private static final String SLOT_BLACKLIST = "slotblacklist";

    public static void sendIMC() {
        InterModComms.sendTo(ModCompats.INVENTORY_SORTER, SLOT_BLACKLIST, BackpackSlot.class::getName);
        InterModComms.sendTo(ModCompats.INVENTORY_SORTER, SLOT_BLACKLIST, UpgradeSlot.class::getName);
        InterModComms.sendTo(ModCompats.INVENTORY_SORTER, SLOT_BLACKLIST, ToolSlot.class::getName);
        InterModComms.sendTo(ModCompats.INVENTORY_SORTER, CONTAINER_BLACKLIST, () -> BuiltInRegistries.MENU.getKey(ModMenuTypes.BACKPACK_MENU.get()));
        InterModComms.sendTo(ModCompats.INVENTORY_SORTER, CONTAINER_BLACKLIST, () -> BuiltInRegistries.MENU.getKey(ModMenuTypes.STORAGE_BOX_MENU.get()));
        InterModComms.sendTo(ModCompats.INVENTORY_SORTER, CONTAINER_BLACKLIST, () -> BuiltInRegistries.MENU.getKey(ModMenuTypes.STORAGE_BOX_MOUNTED_MENU.get()));
    }
}
