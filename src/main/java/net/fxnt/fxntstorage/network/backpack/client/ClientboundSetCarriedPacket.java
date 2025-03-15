package net.fxnt.fxntstorage.network.backpack.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSetCarriedPacket {
    private final ItemStack stack;

    public ClientboundSetCarriedPacket(ItemStack stack) {
        this.stack = stack;
    }

    public static void encode(ClientboundSetCarriedPacket packet, FriendlyByteBuf buffer) {
        buffer.writeItem(packet.stack); // Can only be used for stacks >= 127
    }

    public static ClientboundSetCarriedPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundSetCarriedPacket(buffer.readItem());
    }

    public static void handle(ClientboundSetCarriedPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    client.player.containerMenu.setCarried(packet.stack);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
