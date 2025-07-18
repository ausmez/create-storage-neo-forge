package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record SetSortOrderPacket(SortOrder sortOrder) {

    public static void encode(@NotNull SetSortOrderPacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.sortOrder);
    }

    public static @NotNull SetSortOrderPacket decode(@NotNull FriendlyByteBuf buffer) {
        SortOrder sortOrder = buffer.readEnum(SortOrder.class);
        return new SetSortOrderPacket(sortOrder);
    }

}
