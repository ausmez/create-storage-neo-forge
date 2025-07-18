package net.fxnt.fxntstorage.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record KeyPressedPacket(byte hotkey, boolean pressed) {

    public static void encode(@NotNull KeyPressedPacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeByte(packet.hotkey);
        buffer.writeBoolean(packet.pressed);
    }

    public static @NotNull KeyPressedPacket decode(@NotNull FriendlyByteBuf buffer) {
        byte hotkey = buffer.readByte();
        boolean pressed = buffer.readBoolean();
        return new KeyPressedPacket(hotkey, pressed);
    }

}
