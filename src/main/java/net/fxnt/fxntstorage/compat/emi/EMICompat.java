package net.fxnt.fxntstorage.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.screen.EmiScreenManager;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackScreen;
import net.fxnt.fxntstorage.container.StorageBoxScreen;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxScreen;
import net.minecraft.client.renderer.Rect2i;

@EmiEntrypoint
public class EMICompat implements EmiPlugin {
    private static Bounds asEmiRect(Rect2i rect) {
        return new Bounds(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

    public static void onCraftingPanelToggled() {
        EmiScreenManager.forceRecalculate();
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addGenericExclusionArea((screen, consumer) -> {
            if (screen instanceof StorageBoxScreen storageBoxScreen) {
                storageBoxScreen.getExclusionZones().stream().map(EMICompat::asEmiRect).forEach(consumer);
            }
        });

        registry.addGenericExclusionArea((screen, consumer) -> {
            if (screen instanceof BackpackScreen backPackScreen) {
                backPackScreen.getExclusionZones().stream().map(EMICompat::asEmiRect).forEach(consumer);
            }
        });

        registry.addDragDropHandler(BackpackScreen.class, new EMIDragDropFilterHandler());
        registry.addDragDropHandler(ReserveStorageBoxScreen.class, new EMIReserveStorageDragDropHandler());

        registry.addRecipeHandler(ModMenuTypes.BACKPACK_MENU.get(), new EMIBackpackCraftingRecipeHandler());
    }
}
