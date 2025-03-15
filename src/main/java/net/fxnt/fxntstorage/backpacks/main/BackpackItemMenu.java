package net.fxnt.fxntstorage.backpacks.main;

import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class BackpackItemMenu extends BackpackMenu {

    public BackpackItemMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new BackpackContainer(buf.readItem(), playerInventory.player), buf.readByte());
    }

    public BackpackItemMenu(int containerId, Inventory playerInventory, BackpackContainer container, byte backPackType) {
        super(ModMenuTypes.BACK_PACK_ITEM_MENU.get(), containerId, playerInventory, container, backPackType);
    }

}
