package net.fxnt.fxntstorage.simple_storage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SimpleStorageBoxScreen extends AbstractSimpleStorageBoxScreen<SimpleStorageBoxMenu> {

    public static SimpleStorageBoxScreen createScreen(SimpleStorageBoxMenu menu, Inventory playerInventory, Component title) {
        return new SimpleStorageBoxScreen(menu, playerInventory, title);
    }

    public SimpleStorageBoxScreen(SimpleStorageBoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
