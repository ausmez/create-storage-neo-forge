package net.fxnt.fxntstorage.network.packet;

import net.minecraft.network.FriendlyByteBuf;

public record VisualJetpackAirPacket(int airRemaining) {

    public static void encode(VisualJetpackAirPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.airRemaining);
    }

    public static VisualJetpackAirPacket decode(FriendlyByteBuf buffer) {
        return new VisualJetpackAirPacket(buffer.readInt());
    }

}
