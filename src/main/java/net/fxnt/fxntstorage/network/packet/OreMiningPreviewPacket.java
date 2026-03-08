package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.upgrade.oremining.OreMiningRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record OreMiningPreviewPacket(List<BlockPos> positions) {

    public static void encode(OreMiningPreviewPacket packet, FriendlyByteBuf buffer) {
        buffer.writeCollection(packet.positions, FriendlyByteBuf::writeBlockPos);
    }

    public static OreMiningPreviewPacket decode(FriendlyByteBuf buffer) {
        List<BlockPos> positions = buffer.readCollection(i -> new ArrayList<>(), FriendlyByteBuf::readBlockPos);
        return new OreMiningPreviewPacket(positions);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(OreMiningPreviewPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection().getReceptionSide().isClient()) {
                if (!packet.positions().isEmpty()) {
                    OreMiningRenderer.setPreview(packet.positions());
                } else {
                    OreMiningRenderer.clear();
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
