package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.util.EventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CrossbowChargedPacket() {

    public static void encode(CrossbowChargedPacket packet, FriendlyByteBuf buffer) {
    }

    public static CrossbowChargedPacket decode(FriendlyByteBuf buffer) {
        return new CrossbowChargedPacket();
    }

    public static void handle(CrossbowChargedPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            EventHandler.consumeArrowFromBackpack(context.get().getSender());
        });
        context.get().setPacketHandled(true);
    }
}
