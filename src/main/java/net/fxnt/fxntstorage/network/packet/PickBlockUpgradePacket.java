package net.fxnt.fxntstorage.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record PickBlockUpgradePacket(ItemStack stack) {

    public static void encode(@NotNull PickBlockUpgradePacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeItem(packet.stack);
    }

    public static @NotNull PickBlockUpgradePacket decode(@NotNull FriendlyByteBuf buffer) {
        ItemStack stack = buffer.readItem();
        return new PickBlockUpgradePacket(stack);
    }

}
