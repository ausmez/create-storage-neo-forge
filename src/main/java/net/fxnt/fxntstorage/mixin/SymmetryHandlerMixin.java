package net.fxnt.fxntstorage.mixin;

import com.simibubi.create.content.equipment.symmetryWand.SymmetryHandler;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraftforge.event.level.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SymmetryHandler.class)
public class SymmetryHandlerMixin {
    @Inject(
            method = "onBlockPlaced(Lnet/minecraftforge/event/level/BlockEvent$EntityPlaceEvent;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )

    private static void fxnt$onBlockPlaced(BlockEvent.EntityPlaceEvent event, CallbackInfo ci) {
        if (event.getPlacedBlock().is(ModTags.Blocks.SYMMETRY_WAND_BLACKLIST)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onBlockDestroyed(Lnet/minecraftforge/event/level/BlockEvent$BreakEvent;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )

    private static void fxnt$onBlockDestroyed(BlockEvent.BreakEvent event, CallbackInfo ci) {
        if (event.getState().is(ModTags.Blocks.SYMMETRY_WAND_BLACKLIST)) {
            ci.cancel();
        }
    }
}
