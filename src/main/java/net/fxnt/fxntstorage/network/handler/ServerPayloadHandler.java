package net.fxnt.fxntstorage.network.handler;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.backpack.main.BackpackMenu;
import net.fxnt.fxntstorage.backpack.upgrade.BackpackOnBackUpgradeHandler;
import net.fxnt.fxntstorage.backpack.upgrade.JetpackHandler;
import net.fxnt.fxntstorage.backpack.upgrade.JetpackManager;
import net.fxnt.fxntstorage.backpack.util.BackpackHandler;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.container.StorageBoxMenu;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedMenu;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedStorage;
import net.fxnt.fxntstorage.network.packet.*;
import net.fxnt.fxntstorage.simple_storage.mounted.SimpleStorageBoxMountedStorage;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ServerPayloadHandler {
    private static final ServerPayloadHandler INSTANCE = new ServerPayloadHandler();

    public static ServerPayloadHandler getInstance() {
        return INSTANCE;
    }

    public void handleBackpackHotkeyPacket(final BackpackHotkeyPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            byte key = packet.hotKey();

            if (key == Util.OPEN_BACKPACK)
                BackpackHandler.openBackpackFromInventory(player, Util.BACKPACK_ON_BACK);
            if (key == Util.CLOSE_BACKPACK && player.containerMenu instanceof BackpackMenu)
                player.closeContainer();
            if (key == Util.TOGGLE_HOVER) {
                JetpackHandler jetpackHandler = JetpackManager.getJetpackHandler(player);
                if (jetpackHandler.calculateJetPackFuel(player) > 0) {
                    jetpackHandler.toggleHover();
                }
            }
        });
    }

    public void handleBackpackMenuCtrlPacket(final BackpackMenuCtrlPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            boolean ctrlKeyDown = packet.ctrlKeyDown();

            if (player.containerMenu instanceof BackpackMenu backpackMenu)
                backpackMenu.ctrlKeyDown = ctrlKeyDown;
        });
    }

    public void handleJetpackFlyPacket(final JetpackFlyPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            byte type = packet.keyPress();

            if (type == Util.JETPACK_KEY_PRESS)
                JetpackHandler.flyingOnKeyPress(player);
            if (type == Util.JETPACK_KEY_RELEASE)
                JetpackHandler.flyingOnKeyRelease(player);
        });
    }

    public void handlePickBlockUpgradePacket(final PickBlockUpgradePacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            new BackpackOnBackUpgradeHandler(player).applyPickBlockUpgrade(packet.stack());
        });
    }

    public void handlePlayerInputPacket(final PlayerInputPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            JetpackHandler.processPlayerInputPacket(player, packet.forwardImpulse(), packet.leftImpulse());
        });
    }

    public void handleSortInventoryPacket(final SortInventoryPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            // Backpack sorting
            if (packet.invType() == Util.INV_TYPE_BACKPACK && player.containerMenu instanceof BackpackMenu menu) {
                menu.sortBackpackItems(packet.slotStart(), packet.slotEnd(), packet.sortOrder());
            }
            // StorageBox sorting
            if (packet.invType() == Util.INV_TYPE_STORAGE_BOX && player.containerMenu instanceof StorageBoxMenu menu) {
                menu.sortStorageItems(packet.slotStart(), packet.slotEnd(), packet.sortOrder());
            }
            // StorageBoxMounted sorting
            if (packet.invType() == Util.INV_TYPE_STORAGE_BOX && player.containerMenu instanceof StorageBoxMountedMenu menu) {
                menu.sortStorageItems(packet.slotStart(), packet.slotEnd(), packet.sortOrder());
            }
        });
    }

    public void handleSyncClientSettingsPacket(final SyncClientSettingsPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            ListTag prefersSilkTouchList = new ListTag();
            for (int i = 0; i < packet.prefersSilkTouchList().size(); i++) {
                prefersSilkTouchList.add(StringTag.valueOf(packet.prefersSilkTouchList().get(i)));
            }
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("DisplayFeederMessage", packet.displayFeederMessage());
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("IgnoreFanProcessing", packet.ignoreFanProcessing());
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("PreferSilkTouch", packet.preferSilkTouch());
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).put("PrefersSilkTouchList", prefersSilkTouchList);
        });
    }

    public void handleSetSortOrderPacket(SetSortOrderPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            if (player.containerMenu instanceof BackpackMenu menu) {
                menu.container.setSortOrder(packet.sortOrder());
                menu.container.setDataChanged();
            }

            if (player.containerMenu instanceof StorageBoxMenu menu) {
                menu.setSortOrder(packet.sortOrder());
            }

            if (player.containerMenu instanceof StorageBoxMountedMenu menu) {
                menu.setSortOrder(packet.sortOrder());
            }
        });
    }

    public void handleSetMountedStorageDirtyPacket(SetMountedStorageDirtyPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            Entity entity = player.level().getEntity(packet.entityId());
            if (entity instanceof AbstractContraptionEntity contraptionEntity) {
                MountedItemStorage storage = contraptionEntity.getContraption().getStorage().getAllItemStorages().get(packet.localPos());
                if (storage instanceof SimpleStorageBoxMountedStorage) {
                    ((SimpleStorageBoxMountedStorage) storage).markDirty();
                } else if (storage instanceof StorageBoxMountedStorage) {
                    ((StorageBoxMountedStorage) storage).markDirty();
                }
            }
        });
    }
}
