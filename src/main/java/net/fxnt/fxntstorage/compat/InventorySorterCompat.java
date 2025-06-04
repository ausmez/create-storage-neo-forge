package net.fxnt.fxntstorage.compat;

import net.fxnt.fxntstorage.backpack.main.BackpackSlot;
import net.fxnt.fxntstorage.backpack.main.ToolSlot;
import net.fxnt.fxntstorage.backpack.main.UpgradeSlot;
import net.fxnt.fxntstorage.init.ModCompats;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.registries.ForgeRegistries;

public class InventorySorterCompat {
    private static final String CONTAINER_BLACKLIST = "containerblacklist";
    private static final String SLOT_BLACKLIST = "slotblacklist";

    public static void sendIMC() {
        InterModComms.sendTo(ModCompats.INVENTORY_SORTER, SLOT_BLACKLIST, BackpackSlot.class::getName);
        InterModComms.sendTo(ModCompats.INVENTORY_SORTER, SLOT_BLACKLIST, UpgradeSlot.class::getName);
        InterModComms.sendTo(ModCompats.INVENTORY_SORTER, SLOT_BLACKLIST, ToolSlot.class::getName);
        InterModComms.sendTo(ModCompats.INVENTORY_SORTER, CONTAINER_BLACKLIST, () -> ForgeRegistries.MENU_TYPES.getKey(ModMenuTypes.BACKPACK_BLOCK_MENU.get()));
        InterModComms.sendTo(ModCompats.INVENTORY_SORTER, CONTAINER_BLACKLIST, () -> ForgeRegistries.MENU_TYPES.getKey(ModMenuTypes.BACKPACK_ITEM_MENU.get()));
        InterModComms.sendTo(ModCompats.INVENTORY_SORTER, CONTAINER_BLACKLIST, () -> ForgeRegistries.MENU_TYPES.getKey(ModMenuTypes.STORAGE_BOX_MENU.get()));
//        InterModComms.sendTo(ModCompats.INVENTORY_SORTER, CONTAINER_BLACKLIST, () -> ForgeRegistries.MENU_TYPES.getKey(ModMenuTypes.STORAGE_BOX_MOUNTED_MENU.get()));
    }

}
