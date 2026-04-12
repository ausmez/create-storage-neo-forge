package net.fxnt.fxntstorage.container;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class StorageBoxScreen extends AbstractStorageBoxScreen<StorageBoxMenu> {

    public static StorageBoxScreen createScreen(StorageBoxMenu menu, Inventory playerInventory, Component title) {
        return new StorageBoxScreen(menu, playerInventory, title);
    }

    public StorageBoxScreen(StorageBoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}