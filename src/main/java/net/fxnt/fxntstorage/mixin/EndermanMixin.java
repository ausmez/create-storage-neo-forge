package net.fxnt.fxntstorage.mixin;

import net.fxnt.fxntstorage.init.ModEffects;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
public class EndermanMixin {

    @Inject(
            method = "isLookingAtMe",
            at = @At("HEAD"),
            cancellable = true
    )

    private void fxnt$calmEndermanEffect(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player.hasEffect(ModEffects.CALM_ENDERMEN)) {
            cir.setReturnValue(false);
        }
    }
}
