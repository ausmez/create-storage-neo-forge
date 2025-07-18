package net.fxnt.fxntstorage.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record PlayerInputPacket(float forwardImpulse, float leftImpulse) {

    public static void encode(@NotNull PlayerInputPacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.forwardImpulse);
        buffer.writeFloat(packet.leftImpulse);
    }

    public static @NotNull PlayerInputPacket decode(@NotNull FriendlyByteBuf buffer) {
        float forwardImpulse = buffer.readFloat();
        float leftImpulse = buffer.readFloat();
        return new PlayerInputPacket(forwardImpulse, leftImpulse);
    }

}
