package net.fxnt.fxntstorage.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetCarriedPacket(ItemStack stack) {

    public static void encode(SetCarriedPacket packet, FriendlyByteBuf buffer) {
        buffer.writeItem(packet.stack); // Can only be used for stacks >= 127
    }

    public static SetCarriedPacket decode(FriendlyByteBuf buffer) {
        return new SetCarriedPacket(buffer.readItem());
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(SetCarriedPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection().getReceptionSide().isClient()) {
                Minecraft client = Minecraft.getInstance();
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.containerMenu.setCarried(packet.stack());
                    }
                });
            }
        });
        context.get().setPacketHandled(true);
    }
}
