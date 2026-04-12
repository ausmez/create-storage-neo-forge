package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.compat.InventorySorterCompat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

@EventBusSubscriber(modid = FXNTStorage.MOD_ID)
public class ModCompats {
    public static final String CURIOS = "curios";
    public static final String CARRY_ON = "carryon";
    public static final String CONSTRUCTION_STICK = "constructionstick";
    public static final String EVERY_COMPAT = "everycomp";
    public static final String INVENTORY_SORTER = "inventorysorter";
    public static final String TOMS_STORAGE = "toms_storage";
    public static final String VANILLA_BACKPORT = "vanillabackport";

    @SubscribeEvent
    public static void enqueueCompatMessages(final InterModEnqueueEvent event) {
        event.enqueueWork(() -> isModLoaded(INVENTORY_SORTER, InventorySorterCompat::sendIMC));
    }

    private static void isModLoaded(String modId, Runnable runnable) {
        if (ModList.get().isLoaded(modId)) {
            runnable.run();
        }
    }
}
