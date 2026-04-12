package net.fxnt.fxntstorage.container.mounted;

import net.fxnt.fxntstorage.container.AbstractStorageBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class StorageBoxMountedScreen extends AbstractStorageBoxScreen<StorageBoxMountedMenu> {

    public static StorageBoxMountedScreen createScreen(StorageBoxMountedMenu menu, Inventory playerInventory, Component title) {
        return new StorageBoxMountedScreen(menu, playerInventory, title);
    }

    public StorageBoxMountedScreen(StorageBoxMountedMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
