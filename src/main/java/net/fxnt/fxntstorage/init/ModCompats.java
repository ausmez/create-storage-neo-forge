package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.compat.CarryOnCompat;
import net.fxnt.fxntstorage.compat.InventorySorterCompat;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

@Mod.EventBusSubscriber(modid = FXNTStorage.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCompats {
    public static final String CURIOS = "curios";
    public static final String CARRY_ON = "carryon";
    public static final String CONSTRUCTION_WAND = "constructionwand";
    public static final String EVERY_COMPAT = "everycomp";
    public static final String INVENTORY_SORTER = "inventorysorter";
    public static final String TOMS_STORAGE = "toms_storage";
    public static final String VANILLA_BACKPORT = "vanillabackport";

    @SubscribeEvent
    public static void enqueueCompatMessages(final InterModEnqueueEvent event) {
        event.enqueueWork(() -> isModLoaded(CARRY_ON, CarryOnCompat::sendIMC));
        event.enqueueWork(() -> isModLoaded(INVENTORY_SORTER, InventorySorterCompat::sendIMC));
    }

    private static void isModLoaded(String modId, Runnable runnable) {
        if (ModList.get().isLoaded(modId)) {
            runnable.run();
        }
    }

}
