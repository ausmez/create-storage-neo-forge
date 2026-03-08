package net.fxnt.fxntstorage.mixin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Shadow
    @Final
    @Mutable
    public static Codec<ItemStack> CODEC;

    @Unique
    private static int MAX_COUNT = 1048576;

    @Inject(
            method = "<clinit>",
            at = @At("TAIL")
    )

    private static void fxnt$overwriteCodec(CallbackInfo ci) {
        CODEC = Codec.lazyInitialized(() ->
                RecordCodecBuilder.create(instance ->
                        instance.group(
                                ItemStack.ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
                                ExtraCodecs.intRange(1, MAX_COUNT).fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
                                DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                                        .forGetter(ItemStack::getComponentsPatch)
                        ).apply(instance, ItemStack::new)
                )
        );
    }
}
