package net.fxnt.fxntstorage.network.backpack.client;

import net.fxnt.fxntstorage.backpack.main.BackpackMenu;
import net.fxnt.fxntstorage.backpack.util.BackpackNetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SyncContainerPacket {
    private final int containerId;
    private final int stateId;
    private final NonNullList<ItemStack> items;
    private final ItemStack carriedItem;

    public SyncContainerPacket(int containerId, int stateId, NonNullList<ItemStack> items, ItemStack carriedItem) {
        this.containerId = containerId;
        this.stateId = stateId;
        this.items = items;
        this.carriedItem = carriedItem;
    }

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

    public static void handle(SyncContainerPacket packet, @NotNull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                Minecraft client = Minecraft.getInstance();
                if (client.player != null && client.player.containerMenu instanceof BackpackMenu && client.player.containerMenu.containerId == packet.containerId) {
                    ItemStackHandler itemHandler = ((BackpackMenu) client.player.containerMenu).container.getItemHandler();
                    for (int i = 0; i < packet.items.size(); i++) {
                        itemHandler.setStackInSlot(i, packet.items.get(i));
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
