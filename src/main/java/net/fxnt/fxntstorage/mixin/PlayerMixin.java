package net.fxnt.fxntstorage.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @ModifyExpressionValue(
            method = "getDigSpeed",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/entity/player/Player.onGround()Z"
            )
    )

    private boolean fxnt$applyMiningPenalty(boolean original) {
        // Only apply penalty when NOT on ground AND config is TRUE
        return original || !ConfigManager.ServerConfig.JETPACK_MINING_PENALTY.get();
    }
}