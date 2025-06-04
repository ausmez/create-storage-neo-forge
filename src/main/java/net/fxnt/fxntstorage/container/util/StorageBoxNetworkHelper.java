package net.fxnt.fxntstorage.container.util;

import io.netty.buffer.Unpooled;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.ServerboundPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class StorageBoxNetworkHelper {
    public static final ResourceLocation SORT_STORAGE_BOX_INVENTORY = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sort_storage_box_inventory");

    private static final FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());

    public static void sortStorageBox(int pSlotId, int pSlotCount, SortOrder pSortOrder) {
        if (pSlotId < pSlotCount) {
            data.writeInt(0).writeInt(pSlotCount); // StorageBoxSlots
            data.writeEnum(pSortOrder);
        } else if (pSlotId < pSlotCount + 27) {
            data.writeInt(pSlotCount).writeInt(pSlotCount + 27); // PlayerSlots
            data.writeEnum(pSortOrder);
        } else {
            data.writeInt(pSlotCount + 27).writeInt(pSlotCount + 36); // Hot bar
            data.writeEnum(pSortOrder);
        }

        ModNetwork.sendToServer(new ServerboundPacket(SORT_STORAGE_BOX_INVENTORY, data));
    }

}
