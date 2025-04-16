package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VisualJetpackAirPacket(int airRemaining) implements CustomPacketPayload {
    public static final Type<VisualJetpackAirPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "visual_jetpack_air"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, VisualJetpackAirPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, packet -> (packet.airRemaining),
            VisualJetpackAirPacket::new
    );
}
