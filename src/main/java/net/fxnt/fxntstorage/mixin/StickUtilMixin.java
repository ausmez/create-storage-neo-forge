package net.fxnt.fxntstorage.mixin;

import mrbysco.constructionstick.basics.StickUtil;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(StickUtil.class)
public class StickUtilMixin {

    // Injected for countItem() used for the placement preview grid
    @Inject(method = "getFullInv", at = @At("RETURN"))
    private static void fxnt$addBackpackToFullInv(Player player, CallbackInfoReturnable<List<ItemStack>> cir) {
        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        if (backpack.isEmpty()) return;
        cir.getReturnValue().add(backpack);
    }

    // Injected for SupplierInventory.takeItemStack() used for actual block placement
    @Inject(method = "getMainInv", at = @At("RETURN"), cancellable = true)
    private static void fxnt$addBackpackToMainInv(Player player, CallbackInfoReturnable<List<ItemStack>> cir) {
        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        if (backpack.isEmpty()) return;
        List<ItemStack> inv = new ArrayList<>(cir.getReturnValue());
        inv.add(backpack);
        cir.setReturnValue(inv);
    }
}
