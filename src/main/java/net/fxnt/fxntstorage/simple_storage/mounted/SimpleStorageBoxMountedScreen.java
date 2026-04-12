package net.fxnt.fxntstorage.simple_storage.mounted;

import net.fxnt.fxntstorage.simple_storage.AbstractSimpleStorageBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SimpleStorageBoxMountedScreen extends AbstractSimpleStorageBoxScreen<SimpleStorageBoxMountedMenu> {

    public static SimpleStorageBoxMountedScreen createScreen(SimpleStorageBoxMountedMenu menu, Inventory playerInventory, Component title) {
        return new SimpleStorageBoxMountedScreen(menu, playerInventory, title);
    }

    public SimpleStorageBoxMountedScreen(SimpleStorageBoxMountedMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
