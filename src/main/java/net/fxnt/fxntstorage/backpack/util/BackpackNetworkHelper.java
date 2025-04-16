package net.fxnt.fxntstorage.backpack.util;

import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.network.packet.BackpackMenuCtrlPacket;
import net.fxnt.fxntstorage.network.packet.PickBlockUpgradePacket;
import net.fxnt.fxntstorage.network.packet.SortInventoryPacket;
import net.fxnt.fxntstorage.network.packet.SyncClientSettingsPacket;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class BackpackNetworkHelper {
    public static void sendCtrlKeyDown() {
        PacketDistributor.sendToServer(new BackpackMenuCtrlPacket(true));
    }

    public static void sendCtrlKeyUp() {
        PacketDistributor.sendToServer(new BackpackMenuCtrlPacket(false));
    }

    public static void sortBackpack(int pSlotId, Util.InventorySortOrder pSortOrder) {
        byte sortOrder;
        int slotStart;
        int slotEnd;

        if (pSortOrder == Util.InventorySortOrder.NAME) {
            sortOrder = 1;
        } else if (pSortOrder == Util.InventorySortOrder.TAG) {
            sortOrder = 2;
        } else {
            sortOrder = 0; // COUNT
        }

        if (pSlotId < Util.ITEM_SLOT_END_RANGE) { // BackpackSlots
            slotStart = Util.ITEM_SLOT_START_RANGE;
            slotEnd = Util.ITEM_SLOT_END_RANGE;

        } else if (pSlotId < Util.TOOL_SLOT_END_RANGE) {
            // TODO: Sort tools into tool slots followed by standard items?
            slotStart = Util.TOOL_SLOT_START_RANGE + 5;
            slotEnd = Util.TOOL_SLOT_END_RANGE;

        } else if (pSlotId < Util.UPGRADE_SLOT_END_RANGE) {
            return; // We don't sort upgrade slots
        } else if (pSlotId < Util.UPGRADE_SLOT_END_RANGE + 27) {
            slotStart = Util.UPGRADE_SLOT_END_RANGE;
            slotEnd = Util.UPGRADE_SLOT_END_RANGE + 27;
        } else {
            slotStart = Util.UPGRADE_SLOT_END_RANGE + 27;
            slotEnd = Util.UPGRADE_SLOT_END_RANGE + 36;
        }

        PacketDistributor.sendToServer(new SortInventoryPacket(Util.INV_TYPE_BACKPACK, slotStart, slotEnd, sortOrder));
    }

    public static void sendClientSettings() {
        List<? extends String> prefersSilkTouchList = ConfigManager.ClientConfig.TOOLSWAP_PREFERS_SILK_TOUCH_LIST.get();
        boolean preferSilkTouch = ConfigManager.ClientConfig.TOOLSWAP_PREFER_SILK_TOUCH.get();
        boolean ignoreFanProcessing = ConfigManager.ClientConfig.MAGNET_IGNORE_FAN_PROCESSING.get();
        boolean displayFeederMessage = ConfigManager.ClientConfig.DISPLAY_FEEDER_MESSAGE.get();

        // TODO: Validate params
        PacketDistributor.sendToServer(new SyncClientSettingsPacket((List<String>) prefersSilkTouchList, preferSilkTouch, ignoreFanProcessing, displayFeederMessage));
    }

    public static void doPickBlock(ItemStack stack) {
        PacketDistributor.sendToServer(new PickBlockUpgradePacket(stack));
    }

}
