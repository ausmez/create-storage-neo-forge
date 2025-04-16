package net.fxnt.fxntstorage.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.main.BackpackScreen;
import net.fxnt.fxntstorage.container.StorageBoxScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@JeiPlugin
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
public class JEICompat implements IModPlugin {

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "jei_compat");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(StorageBoxScreen.class, new IGuiContainerHandler<>() {
            @NotNull
            @Override
            public List<Rect2i> getGuiExtraAreas(StorageBoxScreen screen) {
                return screen.getExclusionZones();
            }
        });
        registration.addGuiContainerHandler(BackpackScreen.class, new IGuiContainerHandler<>() {
            @NotNull
            @Override
            public List<Rect2i> getGuiExtraAreas(BackpackScreen screen) {
                return screen.getExclusionZones();
            }
        });
    }
}
