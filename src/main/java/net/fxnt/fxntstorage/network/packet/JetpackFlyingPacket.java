package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record JetpackFlyingPacket(boolean flying, boolean hovering) {

    public static void encode(JetpackFlyingPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.flying);
        buffer.writeBoolean(packet.hovering);
    }

    public static JetpackFlyingPacket decode(FriendlyByteBuf buffer) {
        boolean flying = buffer.readBoolean();
        boolean hovering = buffer.readBoolean();
        return new JetpackFlyingPacket(flying, hovering);
    }

    public static void handle(JetpackFlyingPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();

            if (player != null) {
                JetpackManager.getJetpackHandler(player).processPlayerFlyingPacket(packet.flying(), packet.hovering());
            }
        });
        context.get().setPacketHandled(true);
    }
}
