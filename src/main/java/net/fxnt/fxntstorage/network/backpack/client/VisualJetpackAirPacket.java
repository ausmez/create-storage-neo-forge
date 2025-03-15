package net.fxnt.fxntstorage.network.backpack.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VisualJetpackAirPacket {
    private final int airRemaining;

    public VisualJetpackAirPacket(int airRemaining) {
        this.airRemaining = airRemaining;
    }

    public static void encode(VisualJetpackAirPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.airRemaining);
    }

    public static VisualJetpackAirPacket decode(FriendlyByteBuf buffer) {
        return new VisualJetpackAirPacket(buffer.readInt());
    }

    public static void handle(VisualJetpackAirPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    if (packet.airRemaining < 0) {
                        client.player.getPersistentData().remove("VisualJetpackAir");
                    } else {
                        client.player.getPersistentData().putInt("VisualJetpackAir", packet.airRemaining);
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
