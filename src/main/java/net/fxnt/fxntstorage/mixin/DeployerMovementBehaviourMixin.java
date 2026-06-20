package net.fxnt.fxntstorage.mixin;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.deployer.DeployerMovementBehaviour;
import net.fxnt.fxntstorage.util.DeployerContext;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeployerMovementBehaviour.class)
public class DeployerMovementBehaviourMixin {

    @Inject(method = "visitNewPosition", at = @At("HEAD"), remap = false)
    private void fxnt$onVisitNewPositionHead(MovementContext context, BlockPos pos, CallbackInfo ci) {
        DeployerContext.DEPLOYER_ACTIVE.set(true);
    }

    @Inject(method = "visitNewPosition", at = @At("RETURN"), remap = false)
    private void fxnt$onVisitNewPositionReturn(MovementContext context, BlockPos pos, CallbackInfo ci) {
        DeployerContext.DEPLOYER_ACTIVE.set(false);
    }
}