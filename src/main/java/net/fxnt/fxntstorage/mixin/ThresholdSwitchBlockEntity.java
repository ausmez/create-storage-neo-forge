package net.fxnt.fxntstorage.mixin;

import com.simibubi.create.compat.thresholdSwitch.ThresholdSwitchCompat;
import net.fxnt.fxntstorage.storage_network.StorageNetworkCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity.class)
public class ThresholdSwitchBlockEntity {
    @Shadow(remap = false)
    @Final
    @Mutable
    private static List<ThresholdSwitchCompat> COMPAT;

    @Inject(
            method = "<clinit>",
            at = @At("TAIL"),
            remap = false
    )

    private static void fxnt$addCustomCompat(CallbackInfo ci) {
        List<ThresholdSwitchCompat> mutable = new ArrayList<>(COMPAT);
        mutable.add(new StorageNetworkCompat());
        COMPAT = mutable;
    }
}
