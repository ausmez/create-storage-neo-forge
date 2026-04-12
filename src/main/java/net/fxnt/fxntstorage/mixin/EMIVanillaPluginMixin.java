package net.fxnt.fxntstorage.mixin;

import dev.emi.emi.VanillaPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.fxnt.fxntstorage.compat.emi.EMICraftingRecipeHandler;
import net.fxnt.fxntstorage.compat.emi.EMIInventoryRecipeHandler;
import net.fxnt.fxntstorage.compat.emi.EMIStonecuttingRecipeHandler;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(VanillaPlugin.class)
public class EMIVanillaPluginMixin {
    @Inject(
            method = "register(Ldev/emi/emi/api/EmiRegistry;)V",
            at = @At(value = "HEAD"),
            remap = false
    )
    private void fxnt$injectVanillaCrafting(EmiRegistry registry, CallbackInfo ci) {
        registry.addRecipeHandler(null, new EMIInventoryRecipeHandler());
        registry.addRecipeHandler(MenuType.CRAFTING, new EMICraftingRecipeHandler());
        registry.addRecipeHandler(MenuType.STONECUTTER, new EMIStonecuttingRecipeHandler());
    }
}
