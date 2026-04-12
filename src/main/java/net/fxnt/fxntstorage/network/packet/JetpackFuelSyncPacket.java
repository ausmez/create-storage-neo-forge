package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackHandler;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.function.Supplier;

public record JetpackFuelSyncPacket(float fuelRemaining, long serverTime) {

    public static void encode(JetpackFuelSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.fuelRemaining);
        buffer.writeLong(packet.serverTime);
    }

    public static JetpackFuelSyncPacket decode(FriendlyByteBuf buffer) {
        float fuelRemaining = buffer.readFloat();
        long serverTime = buffer.readLong();
        return new JetpackFuelSyncPacket(fuelRemaining, serverTime);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(final JetpackFuelSyncPacket packet, @NonNull Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection().getReceptionSide().isClient()) {
                Minecraft client = Minecraft.getInstance();
                if (client.player != null) {
                    JetpackHandler handler = JetpackManager.getJetpackHandler(client.player);
                    if (handler != null) {
                        handler.onFuelSync(packet.fuelRemaining(), packet.serverTime());
                    }
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
