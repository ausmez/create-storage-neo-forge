package net.fxnt.fxntstorage.network;

import net.fxnt.fxntstorage.backpack.main.BackpackMenu;
import net.fxnt.fxntstorage.container.StorageBoxMenu;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedMenu;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SetSortOrderPacket {
    private final SortOrder sortOrder;

    public SetSortOrderPacket(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public static void encode(@NotNull SetSortOrderPacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.sortOrder);
    }

    public static @NotNull SetSortOrderPacket decode(@NotNull FriendlyByteBuf buffer) {
        SortOrder sortOrder = buffer.readEnum(SortOrder.class);
        return new SetSortOrderPacket(sortOrder);
    }

    public static void handle(SetSortOrderPacket packet, @NotNull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();

            if (player.containerMenu instanceof BackpackMenu menu) {
                menu.container.setSortOrder(packet.sortOrder);
                menu.container.setDataChanged();
            }

            if (player.containerMenu instanceof StorageBoxMenu menu) {
                menu.setSortOrder(packet.sortOrder);
            }

            if (player.containerMenu instanceof StorageBoxMountedMenu menu) {
                menu.setSortOrder(packet.sortOrder);
            }

        });
        ctx.get().setPacketHandled(true);
    }

}
