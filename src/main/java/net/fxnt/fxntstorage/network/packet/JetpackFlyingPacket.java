package net.fxnt.fxntstorage.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record JetpackFlyingPacket(boolean flying, boolean hovering) {

    public static void encode(@NotNull JetpackFlyingPacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.flying);
        buffer.writeBoolean(packet.hovering);
    }

    public static @NotNull JetpackFlyingPacket decode(@NotNull FriendlyByteBuf buffer) {
        boolean flying = buffer.readBoolean();
        boolean hovering = buffer.readBoolean();
        return new JetpackFlyingPacket(flying, hovering);
    }

}
