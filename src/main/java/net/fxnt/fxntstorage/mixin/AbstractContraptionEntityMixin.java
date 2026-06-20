package net.fxnt.fxntstorage.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.util.ContraptionInteractionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContraptionEntity.class)
public class AbstractContraptionEntityMixin {

    @Inject(method = "handlePlayerInteraction", at = @At("HEAD"), remap = false)
    private void fxnt$captureInteractionDirection(Player player, BlockPos localPos, Direction side,
            InteractionHand interactionHand, CallbackInfoReturnable<Boolean> cir) {
        ContraptionInteractionContext.INTERACTION_DIRECTION.set(side);
    }

    @Inject(method = "handlePlayerInteraction", at = @At("RETURN"), remap = false)
    private void fxnt$clearInteractionDirection(Player player, BlockPos localPos, Direction side,
            InteractionHand interactionHand, CallbackInfoReturnable<Boolean> cir) {
        ContraptionInteractionContext.INTERACTION_DIRECTION.remove();
    }
}