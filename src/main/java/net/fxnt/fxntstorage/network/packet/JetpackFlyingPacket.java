package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record JetpackFlyingPacket(boolean flying, boolean hovering) implements CustomPacketPayload {
    public static final Type<JetpackFlyingPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "jetpack_flying"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, JetpackFlyingPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, JetpackFlyingPacket::flying,
            ByteBufCodecs.BOOL, JetpackFlyingPacket::hovering,
            JetpackFlyingPacket::new
    );
}
