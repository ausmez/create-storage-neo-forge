package net.fxnt.fxntstorage.backpack.main;

import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class BackpackItemMenu extends BackpackMenu {

    public BackpackItemMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new BackpackContainer(ItemStack.parseOptional(playerInventory.player.registryAccess(), Objects.requireNonNull(buf.readNbt())), playerInventory.player), buf.readByte());
    }

    public BackpackItemMenu(int containerId, Inventory playerInventory, BackpackContainer container, byte backPackType) {
        super(ModMenuTypes.BACKPACK_ITEM_MENU.get(), containerId, playerInventory, container, backPackType);
    }

}
