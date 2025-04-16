package net.fxnt.fxntstorage.backpack.main;

import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class BackpackItemMenu extends BackpackMenu {

    public BackpackItemMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, new BackpackContainer(new ItemStack(ModBlocks.BACKPACK.asItem()), playerInventory.player), buf.readByte());
    }

    public BackpackItemMenu(int containerId, Inventory playerInventory, BackpackContainer container, byte backPackType) {
        super(ModMenuTypes.BACKPACK_ITEM_MENU.get(), containerId, playerInventory, container, backPackType);
    }

}
