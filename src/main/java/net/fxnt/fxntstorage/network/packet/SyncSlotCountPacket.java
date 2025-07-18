package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.util.BackpackNetworkHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record SyncSlotCountPacket(int containerId, int stateId, int slot, ItemStack stack) {

    public static void encode(@NotNull SyncSlotCountPacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.stateId);
        buffer.writeVarInt(packet.slot);
        BackpackNetworkHelper.writeItemStack(packet.stack, buffer);
    }

    public static @NotNull SyncSlotCountPacket decode(@NotNull FriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        int stateId = buffer.readVarInt();
        int slot = buffer.readVarInt();
        ItemStack stack = BackpackNetworkHelper.readItemStack(buffer);
        return new SyncSlotCountPacket(containerId, stateId, slot, stack);
    }

}
