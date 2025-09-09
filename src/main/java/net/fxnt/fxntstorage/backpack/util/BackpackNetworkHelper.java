package net.fxnt.fxntstorage.backpack.util;

import net.fxnt.fxntstorage.network.packet.PickBlockUpgradePacket;
import net.fxnt.fxntstorage.network.packet.SortInventoryPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class BackpackNetworkHelper {

    public static void sortBackpack(int pSlotId, SortOrder pSortOrder) {
        int slotStart;
        int slotEnd;

        if (pSlotId < Util.ITEM_SLOT_END_RANGE) { // BackpackSlots
            slotStart = Util.ITEM_SLOT_START_RANGE;
            slotEnd = Util.ITEM_SLOT_END_RANGE;
        } else if (pSlotId < Util.TOOL_SLOT_END_RANGE) {
            slotStart = Util.TOOL_SLOT_START_RANGE;
            slotEnd = Util.TOOL_SLOT_END_RANGE;
        } else if (pSlotId < Util.UPGRADE_SLOT_END_RANGE) {
            return; // Don't sort upgrade slots
        } else if (pSlotId < Util.UPGRADE_SLOT_END_RANGE + 27) {
            slotStart = Util.UPGRADE_SLOT_END_RANGE;
            slotEnd = Util.UPGRADE_SLOT_END_RANGE + 27;
        } else {
            slotStart = Util.UPGRADE_SLOT_END_RANGE + 27;
            slotEnd = Util.UPGRADE_SLOT_END_RANGE + 36;
        }

        PacketDistributor.sendToServer(new SortInventoryPacket(Util.INV_TYPE_BACKPACK, slotStart, slotEnd, pSortOrder));
    }

    public static void doPickBlock(ItemStack stack) {
        PacketDistributor.sendToServer(new PickBlockUpgradePacket(stack));
    }

}
