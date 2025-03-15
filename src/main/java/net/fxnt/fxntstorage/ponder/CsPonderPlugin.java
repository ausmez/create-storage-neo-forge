package net.fxnt.fxntstorage.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CsPonderPlugin implements PonderPlugin {
    @Override
    public @NotNull String getModId() {
        return FXNTStorage.MOD_ID;
    }

    @Override
    public void registerScenes(@NotNull PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ModPonder.Scenes.register(helper);
    }

    @Override
    public void registerTags(@NotNull PonderTagRegistrationHelper<ResourceLocation> helper) {
        ModPonder.Tags.register(helper);
    }
}
