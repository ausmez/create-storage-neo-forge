package net.fxnt.fxntstorage.containers.util;

import io.netty.buffer.Unpooled;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.ServerboundPacket;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class StorageBoxNetworkHelper {
    public static final ResourceLocation SORT_STORAGE_BOX_INVENTORY = new ResourceLocation(FXNTStorage.MOD_ID, "sort_storage_box_inventory");

    private static final FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());

    public static void sortStorageBox(int pSlotId, int pSlotCount, Util.InventorySortOrder pSortOrder) {
        byte sortOrder;
        if (pSortOrder == Util.InventorySortOrder.NAME) {
            sortOrder = 1;
        } else if (pSortOrder == Util.InventorySortOrder.TAG) {
            sortOrder = 2;
        } else {
            sortOrder = 0; // COUNT
        }

        if (pSlotId < pSlotCount) {
            data.writeInt(0).writeInt(pSlotCount).writeByte(sortOrder); // StorageBoxSlots
        } else if (pSlotId < pSlotCount + 27) {
            data.writeInt(pSlotCount).writeInt(pSlotCount + 27).writeByte(sortOrder); // PlayerSlots
        } else {
            data.writeInt(pSlotCount + 27).writeInt(pSlotCount + 36).writeByte(sortOrder); // Hot bar
        }

        ModNetwork.sendToServer(new ServerboundPacket(SORT_STORAGE_BOX_INVENTORY, data));
    }

}
