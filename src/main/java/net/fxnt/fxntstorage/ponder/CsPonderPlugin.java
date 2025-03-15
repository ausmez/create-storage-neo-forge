package net.fxnt.fxntstorage.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.resources.ResourceLocation;

public class CsPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return FXNTStorage.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ModPonder.Scenes.register(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        ModPonder.Tags.register(helper);
    }
}
