package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PlayerInputPacket(float forwardImpulse, float leftImpulse) {

    public static void encode(PlayerInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.forwardImpulse);
        buffer.writeFloat(packet.leftImpulse);
    }

    public static PlayerInputPacket decode(FriendlyByteBuf buffer) {
        float forwardImpulse = buffer.readFloat();
        float leftImpulse = buffer.readFloat();
        return new PlayerInputPacket(forwardImpulse, leftImpulse);
    }

    public static void handle(PlayerInputPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();

            if (player != null) {
                JetpackManager.getJetpackHandler(player).processPlayerInputPacket(packet.forwardImpulse(), packet.leftImpulse());
            }
        });
        context.get().setPacketHandled(true);
    }
}
