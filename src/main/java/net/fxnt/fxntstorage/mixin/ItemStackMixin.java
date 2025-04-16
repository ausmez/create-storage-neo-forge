package net.fxnt.fxntstorage.mixin;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements DataComponentHolder, IItemStackExtension, MutableDataComponentHolder {

    // FIXME: This effectively allows all ItemStacks in the game to stack up to 1,048,576. Should really restrict this
    //        to the classes that need it such as SimpleStorageBoxEntity (1,048,576) and BackpackEntity (2,048)
    @ModifyConstant(method = "lambda$static$3", constant = @Constant(intValue = 99))
    private static int fxnt$maxStackSize(int original) {
        return 2048 * 512;
    }

}
