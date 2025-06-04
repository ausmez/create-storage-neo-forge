package net.fxnt.fxntstorage.network.backpack.client;

import net.fxnt.fxntstorage.backpack.main.BackpackMenu;
import net.fxnt.fxntstorage.backpack.util.BackpackNetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SyncSlotCountPacket {
    private final int containerId;
    private final int stateId;
    private final int slot;
    private final ItemStack stack;

    public SyncSlotCountPacket(int containerId, int stateId, int slot, ItemStack stack) {
        this.containerId = containerId;
        this.stateId = stateId;
        this.slot = slot;
        this.stack = stack;
    }

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

    public static void handle(SyncSlotCountPacket packet, @NotNull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                Minecraft client = Minecraft.getInstance();
                if (client.player != null && client.player.containerMenu instanceof BackpackMenu && client.player.containerMenu.containerId == packet.containerId) {
                    ItemStackHandler itemHandler = ((BackpackMenu) client.player.containerMenu).container.getItemHandler();
                    itemHandler.setStackInSlot(packet.slot, packet.stack);
                }

            }
        });
        ctx.get().setPacketHandled(true);
    }
}
