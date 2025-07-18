package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.util.BackpackNetworkHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record SyncNBTDataPacket(ItemStack stack) {

    public static void encode(SyncNBTDataPacket packet, FriendlyByteBuf buf) {
        BackpackNetworkHelper.writeItemStack(packet.stack, buf);
    }

    public static SyncNBTDataPacket decode(FriendlyByteBuf buf) {
        ItemStack stack = BackpackNetworkHelper.readItemStack(buf);
        return new SyncNBTDataPacket(stack);
    }

}
