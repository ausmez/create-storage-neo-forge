package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.container.StorageBoxMenu;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedMenu;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetSortOrderPacket(SortOrder sortOrder) {

    public static void encode(SetSortOrderPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.sortOrder);
    }

    public static SetSortOrderPacket decode(FriendlyByteBuf buffer) {
        SortOrder sortOrder = buffer.readEnum(SortOrder.class);
        return new SetSortOrderPacket(sortOrder);
    }

    public static void handle(SetSortOrderPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();

            if (player != null) {
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
            }
        });
        context.get().setPacketHandled(true);
    }
}
