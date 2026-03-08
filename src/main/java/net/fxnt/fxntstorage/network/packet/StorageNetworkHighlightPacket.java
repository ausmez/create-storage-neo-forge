package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.controller.StorageControllerHighlight;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StorageNetworkHighlightPacket(BlockPos controllerPos, boolean enable) implements CustomPacketPayload {
    public static final Type<StorageNetworkHighlightPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sync_network_highlight"));

    public static final StreamCodec<FriendlyByteBuf, StorageNetworkHighlightPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StorageNetworkHighlightPacket::controllerPos,
            ByteBufCodecs.BOOL, StorageNetworkHighlightPacket::enable,
            StorageNetworkHighlightPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!enable()) StorageControllerHighlight.remove(controllerPos());
        });
    }
}
