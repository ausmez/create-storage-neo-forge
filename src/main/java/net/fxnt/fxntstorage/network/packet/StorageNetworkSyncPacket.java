package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.controller.StorageControllerHighlight;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public record StorageNetworkSyncPacket(BlockPos controller, Set<BlockPos> boxes) {

    public static void encode(StorageNetworkSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.controller);
        buffer.writeVarInt(packet.boxes.size());
        for (BlockPos pos : packet.boxes) {
            buffer.writeBlockPos(pos);
        }
    }

    public static StorageNetworkSyncPacket decode(FriendlyByteBuf buffer) {
        BlockPos controller = buffer.readBlockPos();
        int size = buffer.readVarInt();
        Set<BlockPos> boxes = new HashSet<>();
        for (int i = 0; i < size; i++) {
            boxes.add(buffer.readBlockPos());
        }
        return new StorageNetworkSyncPacket(controller, boxes);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(final StorageNetworkSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> StorageControllerHighlight.set(packet.controller(), packet.boxes()));
        context.get().setPacketHandled(true);
    }

}
