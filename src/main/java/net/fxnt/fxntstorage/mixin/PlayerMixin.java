package net.fxnt.fxntstorage.mixin;

import net.fxnt.fxntstorage.config.ConfigManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Redirect(method = "getDigSpeed", at = @At(value = "INVOKE", target = "net/minecraft/world/entity/player/Player.onGround()Z"))

    private boolean fxnt$applyMiningPenalty(Player player) {
        // Only apply penalty when NOT on ground AND config is TRUE
        return player.onGround() || !ConfigManager.CommonConfig.JETPACK_MINING_PENALTY.get();
    }

}