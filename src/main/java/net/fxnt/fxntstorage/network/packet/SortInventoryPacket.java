package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.container.StorageBoxMenu;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedMenu;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SortInventoryPacket(byte invType, int slotStart, int slotEnd, SortOrder sortOrder) {

    public static void encode(SortInventoryPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.invType);
        buffer.writeInt(packet.slotStart);
        buffer.writeInt(packet.slotEnd);
        buffer.writeEnum(packet.sortOrder);
    }

    public static SortInventoryPacket decode(FriendlyByteBuf buffer) {
        byte invType = buffer.readByte();
        int slotStart = buffer.readInt();
        int slotEnd = buffer.readInt();
        SortOrder sortOrder = buffer.readEnum(SortOrder.class);
        return new SortInventoryPacket(invType, slotStart, slotEnd, sortOrder);
    }

    public static void handle(SortInventoryPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();

            if (player != null) {
                byte sortType = packet.invType();
                // Backpack sorting
                if (sortType == Util.INV_TYPE_BACKPACK && player.containerMenu instanceof BackpackMenu menu)
                    menu.sortBackpackItems(packet.slotStart(), packet.slotEnd(), packet.sortOrder());
                if (sortType == Util.INV_TYPE_STORAGE_BOX) {
                    // StorageBox sorting
                    if (player.containerMenu instanceof StorageBoxMenu menu)
                        menu.sortStorageItems(packet.slotStart(), packet.slotEnd(), packet.sortOrder());
                    // StorageBoxMounted sorting
                    if (player.containerMenu instanceof StorageBoxMountedMenu menu)
                        menu.sortStorageItems(packet.slotStart(), packet.slotEnd(), packet.sortOrder());
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
