package net.fxnt.fxntstorage.backpack.util;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class BackpackClientHelper {

    public static ItemStack getEquippedBackpackStack(LocalPlayer player) {
        if (player == null) return ItemStack.EMPTY;

        // Check chest slot first
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.getItem() instanceof BackpackItem) {
            return chestStack;
        }

        // Check Curios
        if (FXNTStorage.curiosLoaded) {
            Optional<ItemStack> backSlot = CuriosApi.getCuriosInventory(player)
                    .flatMap(handler -> handler.getStacksHandler("back"))
                    .map(handler -> handler.getStacks().getStackInSlot(0));
            return backSlot
                    .filter(stack -> stack.getItem() instanceof BackpackItem)
                    .orElse(ItemStack.EMPTY);
        }

        return ItemStack.EMPTY;
    }

}
