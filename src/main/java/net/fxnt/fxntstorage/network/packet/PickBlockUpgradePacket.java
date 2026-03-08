package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.upgrade.UpgradeEventDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PickBlockUpgradePacket(ItemStack stack) {

    public static void encode(PickBlockUpgradePacket packet, FriendlyByteBuf buffer) {
        buffer.writeItem(packet.stack);
    }

    public static PickBlockUpgradePacket decode(FriendlyByteBuf buffer) {
        ItemStack stack = buffer.readItem();
        return new PickBlockUpgradePacket(stack);
    }

    public static void handle(PickBlockUpgradePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                UpgradeEventDispatcher.dispatchPickBlock(player, packet.stack);
            }
        });
        ctx.setPacketHandled(true);
    }
}
