package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.upgrade.oremining.OreMiningRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record OreMiningPreviewPacket(List<BlockPos> positions) implements CustomPacketPayload {

    public static final Type<OreMiningPreviewPacket> TYPE = new Type<>(FXNTStorage.modLoc("ore_mining_preview_positions"));

    public static final StreamCodec<FriendlyByteBuf, OreMiningPreviewPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), OreMiningPreviewPacket::positions,
            OreMiningPreviewPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!positions().isEmpty()) {
                OreMiningRenderer.setPreview(positions());
            } else {
                OreMiningRenderer.clear();
            }
        });
    }
}
