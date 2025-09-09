package net.fxnt.fxntstorage.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record JetpackFuelSyncPacket(float fuelRemaining, long serverTime) {

    public static void encode(@NotNull JetpackFuelSyncPacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.fuelRemaining);
        buffer.writeLong(packet.serverTime);
    }

    public static @NotNull JetpackFuelSyncPacket decode(@NotNull FriendlyByteBuf buffer) {
        float fuelRemaining = buffer.readFloat();
        long serverTime = buffer.readLong();
        return new JetpackFuelSyncPacket(fuelRemaining, serverTime);
    }

}
