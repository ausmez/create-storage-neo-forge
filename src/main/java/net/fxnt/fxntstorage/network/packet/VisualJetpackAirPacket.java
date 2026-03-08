package net.fxnt.fxntstorage.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record VisualJetpackAirPacket(int airRemaining) {

    public static void encode(VisualJetpackAirPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.airRemaining);
    }

    public static VisualJetpackAirPacket decode(FriendlyByteBuf buffer) {
        return new VisualJetpackAirPacket(buffer.readInt());
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(VisualJetpackAirPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection().getReceptionSide().isClient()) {
                Minecraft client = Minecraft.getInstance();
                client.execute(() -> {
                    if (client.player != null) {
                        if (packet.airRemaining() < 0) {
                            client.player.getPersistentData().remove("VisualJetpackAir");
                        } else {
                            client.player.getPersistentData().putInt("VisualJetpackAir", packet.airRemaining());
                        }
                    }
                });
            }
        });
        context.get().setPacketHandled(true);
    }
}
