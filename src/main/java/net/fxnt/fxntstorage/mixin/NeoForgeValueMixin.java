package net.fxnt.fxntstorage.mixin;

import com.mrcrayfish.configured.impl.neoforge.NeoForgeValue;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NeoForgeValue.class)
public class NeoForgeValueMixin {

    @Shadow
    @Final
    @Nullable
    public ModConfigSpec.ValueSpec valueSpec;

    @Inject(
            method = "getValidationHint",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    /*
        Workaround for bug with Configured when editing an entry on the EditListScreen and the validation returns false
        causing an NPE when getValidationHint() attempts to load a Min/Max value...
     */
    private void fxnt$injectGetValidationHint(CallbackInfoReturnable<Component> cir) {
        if (this.valueSpec != null && this.valueSpec.getRange() == null) {
            cir.setReturnValue(Component.translatable("fxntstorage.configuration.invalidBlock"));
        }
    }

}
