package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KeyPressedPacket(byte hotKey, boolean pressed) implements CustomPacketPayload {
    public static final Type<KeyPressedPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "key_pressed"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, KeyPressedPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, KeyPressedPacket::hotKey,
            ByteBufCodecs.BOOL, KeyPressedPacket::pressed,
            KeyPressedPacket::new
    );
}
