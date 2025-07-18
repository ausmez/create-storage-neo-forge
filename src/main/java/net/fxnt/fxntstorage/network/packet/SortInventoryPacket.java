package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record SortInventoryPacket(byte invType, int slotStart, int slotEnd, SortOrder sortOrder) {

    public static void encode(@NotNull SortInventoryPacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeByte(packet.invType);
        buffer.writeInt(packet.slotStart);
        buffer.writeInt(packet.slotEnd);
        buffer.writeEnum(packet.sortOrder);
    }

    public static @NotNull SortInventoryPacket decode(@NotNull FriendlyByteBuf buffer) {
        byte invType = buffer.readByte();
        int slotStart = buffer.readInt();
        int slotEnd = buffer.readInt();
        SortOrder sortOrder = buffer.readEnum(SortOrder.class);
        return new SortInventoryPacket(invType, slotStart, slotEnd, sortOrder);
    }

}
