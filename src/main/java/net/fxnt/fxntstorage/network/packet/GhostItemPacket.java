package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record GhostItemPacket(ItemStack item, int slot) {

    public static void encode(GhostItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeItem(packet.item);
        buffer.writeInt(packet.slot);
    }

    public static GhostItemPacket decode(FriendlyByteBuf buffer) {
        ItemStack item = buffer.readItem();
        int slot = buffer.readInt();
        return new GhostItemPacket(item, slot);
    }

    public static void handle(final GhostItemPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && player.containerMenu instanceof BackpackMenu menu) {
                menu.container.getItemHandler().setStackInSlot(packet.slot(), packet.item());
                menu.container.setDataChanged();
            }
        });
        context.get().setPacketHandled(true);
    }
}
