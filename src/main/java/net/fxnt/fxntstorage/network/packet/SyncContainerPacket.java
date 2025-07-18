package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.util.BackpackNetworkHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record SyncContainerPacket(int containerId, int stateId, NonNullList<ItemStack> items, ItemStack carriedItem) {

    public static void encode(@NotNull SyncContainerPacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.stateId);
        buffer.writeVarInt(packet.items.size());
        for (ItemStack stack : packet.items) {
            BackpackNetworkHelper.writeItemStack(stack, buffer);
        }
        buffer.writeItem(packet.carriedItem);
    }

    public static @NotNull SyncContainerPacket decode(@NotNull FriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        int stateId = buffer.readVarInt();
        int size = buffer.readVarInt();
        NonNullList<ItemStack> stacks = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < size; i++) {
            stacks.set(i, BackpackNetworkHelper.readItemStack(buffer));
        }
        ItemStack carriedItem = buffer.readItem();
        return new SyncContainerPacket(containerId, stateId, stacks, carriedItem);
    }

}
