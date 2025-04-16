package net.fxnt.fxntstorage.container.util;

import net.fxnt.fxntstorage.network.packet.SortInventoryPacket;
import net.fxnt.fxntstorage.util.Util;
import net.neoforged.neoforge.network.PacketDistributor;

public class StorageBoxNetworkHelper {

    public static void sortStorageBox(int pSlotId, int pSlotCount, Util.InventorySortOrder pSortOrder) {
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

        if (pSlotId < pSlotCount) { // StorageBoxSlots
            slotStart = 0;
            slotEnd = pSlotCount;
        } else if (pSlotId < pSlotCount + 27) { // PlayerSlots
            slotStart = pSlotCount;
            slotEnd = pSlotCount + 27;
//            data.writeInt(pSlotCount).writeInt(pSlotCount + 27).writeByte(sortOrder);
        } else { // Hot bar
            slotStart = pSlotCount + 27;
            slotEnd = pSlotCount + 36;
//            data.writeInt(pSlotCount + 27).writeInt(pSlotCount + 36).writeByte(sortOrder);
        }

        PacketDistributor.sendToServer(new SortInventoryPacket(Util.INV_TYPE_STORAGE_BOX, slotStart, slotEnd, sortOrder));
//        ModNetwork.sendToServer(new ServerboundPacket(SORT_STORAGE_BOX_INVENTORY, data));
    }

}
