package net.fxnt.fxntstorage.network;

import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.SyncContainerPacket;
import net.fxnt.fxntstorage.network.packet.SyncSlotCountPacket;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class HighStackCountSync implements ContainerSynchronizer {
    private final ServerPlayer player;

    public HighStackCountSync(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void sendInitialData(@NotNull AbstractContainerMenu container, @NotNull NonNullList<ItemStack> items, @NotNull ItemStack carriedItem, int @NotNull [] initialData) {
        ModNetwork.sendToPlayer(player, new SyncContainerPacket(container.containerId, container.incrementStateId(), items, carriedItem));
    }

    @Override
    public void sendSlotChange(@NotNull AbstractContainerMenu container, int slot, @NotNull ItemStack itemStack) {
        ModNetwork.sendToPlayer(player, new SyncSlotCountPacket(container.containerId, container.incrementStateId(), slot, itemStack));
    }

    @Override
    public void sendCarriedChange(@NotNull AbstractContainerMenu containerMenu, @NotNull ItemStack stack) {
        player.connection.send(new ClientboundContainerSetSlotPacket(-1, containerMenu.incrementStateId(), -1, stack));
    }

    @Override
    public void sendDataChange(@NotNull AbstractContainerMenu container, int id, int value) {
        player.connection.send(new ClientboundContainerSetDataPacket(container.containerId, id, value));
    }
}
