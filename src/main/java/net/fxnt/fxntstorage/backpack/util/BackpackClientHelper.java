package net.fxnt.fxntstorage.backpack.util;

import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

@OnlyIn(Dist.CLIENT)
public class BackpackClientHelper {

    public static ItemStack getEquippedBackpackStack(LocalPlayer player) {
        if (player == null) return ItemStack.EMPTY;

        // Check chest slot first
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.getItem() instanceof BackpackItem) {
            return chestStack;
        }

        // Get the Curios item handler
        LazyOptional<ICuriosItemHandler> curios = player.getCapability(CuriosCapability.INVENTORY, null);

        return curios
                .map(handler -> handler.getStacksHandler("back"))
                .flatMap(stacksHandler -> stacksHandler
                        .map(handler -> handler.getStacks().getStackInSlot(0))
                        .filter(itemStack -> itemStack.getItem() instanceof BackpackItem)
                )
                .orElse(ItemStack.EMPTY);
    }

}
