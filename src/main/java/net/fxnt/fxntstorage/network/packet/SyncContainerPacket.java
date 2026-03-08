package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncContainerPacket(int containerId, int stateId, NonNullList<ItemStack> items, ItemStack carriedItem) {

    public static void encode(SyncContainerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.stateId);
        buffer.writeVarInt(packet.items.size());
        for (ItemStack stack : packet.items) {
            BackpackHelper.writeItemStack(stack, buffer);
        }
        buffer.writeItem(packet.carriedItem);
    }

    public static SyncContainerPacket decode(FriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        int stateId = buffer.readVarInt();
        int size = buffer.readVarInt();
        NonNullList<ItemStack> stacks = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < size; i++) {
            stacks.set(i, BackpackHelper.readItemStack(buffer));
        }
        ItemStack carriedItem = buffer.readItem();
        return new SyncContainerPacket(containerId, stateId, stacks, carriedItem);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(SyncContainerPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection().getReceptionSide().isClient()) {
                Minecraft client = Minecraft.getInstance();
                Player player = client.player;
                if (player == null) return;

                if (player.containerMenu instanceof BackpackMenu menu && menu.containerId == packet.containerId()) {
                    ItemStackHandler itemHandler = menu.container.getItemHandler();
                    for (int i = 0; i < packet.items().size(); i++) {
                        itemHandler.setStackInSlot(i, packet.items().get(i));
                    }

                    menu.setCarried(packet.carriedItem());
                    menu.setStateId(packet.stateId());
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
