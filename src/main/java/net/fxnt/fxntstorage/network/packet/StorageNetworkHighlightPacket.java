package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.controller.StorageControllerHighlight;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StorageNetworkHighlightPacket(BlockPos controllerPos, boolean enable) {

    public static void encode(StorageNetworkHighlightPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.controllerPos);
        buffer.writeBoolean(packet.enable);
    }

    public static StorageNetworkHighlightPacket decode(FriendlyByteBuf buffer) {
        BlockPos controller = buffer.readBlockPos();
        boolean enable = buffer.readBoolean();
        return new StorageNetworkHighlightPacket(controller, enable);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(final StorageNetworkHighlightPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!packet.enable()) StorageControllerHighlight.remove(packet.controllerPos());
        });
        context.get().setPacketHandled(true);
    }
}
