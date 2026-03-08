package net.fxnt.fxntstorage.container.util;

import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.SortInventoryPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;

public class StorageBoxNetworkHelper {

    public static void sortStorageBox(int pSlotId, int pSlotCount, SortOrder pSortOrder) {
        int slotStart;
        int slotEnd;

        if (pSlotId < pSlotCount) { // StorageBoxSlots
            slotStart = 0;
            slotEnd = pSlotCount;
        } else if (pSlotId < pSlotCount + 27) { // PlayerSlots
            slotStart = pSlotCount;
            slotEnd = pSlotCount + 27;
        } else { // Hot bar
            slotStart = pSlotCount + 27;
            slotEnd = pSlotCount + 36;
        }

        ModNetwork.sendToServer(new SortInventoryPacket(Util.INV_TYPE_STORAGE_BOX, slotStart, slotEnd, pSortOrder));
    }
}
