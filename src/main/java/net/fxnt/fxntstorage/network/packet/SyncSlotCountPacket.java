package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncSlotCountPacket(int containerId, int stateId, int slot, ItemStack stack) {

    public static void encode(SyncSlotCountPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeVarInt(packet.stateId);
        buffer.writeVarInt(packet.slot);
        BackpackHelper.writeItemStack(packet.stack, buffer);
    }

    public static SyncSlotCountPacket decode(FriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        int stateId = buffer.readVarInt();
        int slot = buffer.readVarInt();
        ItemStack stack = BackpackHelper.readItemStack(buffer);
        return new SyncSlotCountPacket(containerId, stateId, slot, stack);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(SyncSlotCountPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection().getReceptionSide().isClient()) {
                Minecraft client = Minecraft.getInstance();
                Player player = client.player;
                if (player == null) return;
                if (player.containerMenu instanceof BackpackMenu menu && player.containerMenu.containerId == packet.containerId()) {
                    if (packet.slot() < 0 || packet.slot() >= menu.slots.size()) return;

                    ItemStackHandler itemHandler = menu.container.getItemHandler();
                    if (packet.slot() < itemHandler.getSlots()) {
                        itemHandler.setStackInSlot(packet.slot(), packet.stack());
                    } else {
                        menu.getSlot(packet.slot()).set(packet.stack());
                    }

                    menu.setStateId(packet.stateId());
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
