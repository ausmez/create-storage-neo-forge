package net.fxnt.fxntstorage.compat.constructionwand;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IWandContainer {

    boolean matches(Player player, ItemStack inventoryStack);

    int countItems(Player player, ItemStack itemStack, ItemStack inventoryStack);

    int useItems(Player player, ItemStack itemStack, ItemStack inventoryStack, int count);

    default int signature(Player player, ItemStack inventoryStack) {
        int hash = System.identityHashCode(inventoryStack);
        return (hash == -1 || hash == 0) ? Integer.MIN_VALUE : hash;
    }
}
