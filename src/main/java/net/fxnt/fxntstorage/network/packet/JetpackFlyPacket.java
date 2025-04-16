package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record JetpackFlyPacket(byte keyPress) implements CustomPacketPayload {
    public static final Type<JetpackFlyPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "jetpack_fly"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, JetpackFlyPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, packet -> (packet.keyPress),
            JetpackFlyPacket::new
    );

}
