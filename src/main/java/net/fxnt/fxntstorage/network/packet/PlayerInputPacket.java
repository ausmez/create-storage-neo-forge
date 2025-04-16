package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayerInputPacket(float forwardImpulse, float leftImpulse) implements CustomPacketPayload {
    public static final Type<PlayerInputPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "player_input"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, PlayerInputPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, PlayerInputPacket::forwardImpulse,
            ByteBufCodecs.FLOAT, PlayerInputPacket::leftImpulse,
            PlayerInputPacket::new
    );
}
