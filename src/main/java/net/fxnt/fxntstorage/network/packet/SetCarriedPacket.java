package net.fxnt.fxntstorage.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record SetCarriedPacket(ItemStack stack) {

    public static void encode(SetCarriedPacket packet, FriendlyByteBuf buffer) {
        buffer.writeItem(packet.stack); // Can only be used for stacks >= 127
    }

    public static SetCarriedPacket decode(FriendlyByteBuf buffer) {
        return new SetCarriedPacket(buffer.readItem());
    }

}
