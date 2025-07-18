package net.fxnt.fxntstorage.mixin;

import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.plugin.client.DefaultClientPlugin;
import net.fxnt.fxntstorage.compat.rei.REICraftingTransferHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("UnstableApiUsage")
@Mixin(DefaultClientPlugin.class)
public class REIDefaultClientPluginMixin {
    @Inject(
            method = "registerTransferHandlers(Lme/shedaniel/rei/api/client/registry/transfer/TransferHandlerRegistry;)V",
            at = @At(value = "HEAD"),
            remap = false
    )
    private void fxnt$injectVanillaCrafting(TransferHandlerRegistry registry, CallbackInfo ci) {
        registry.register(new REICraftingTransferHandler());
    }
}
