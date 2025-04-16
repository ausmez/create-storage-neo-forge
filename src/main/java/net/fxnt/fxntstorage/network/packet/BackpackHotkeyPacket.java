package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BackpackHotkeyPacket(byte hotKey) implements CustomPacketPayload {
    public static final Type<BackpackHotkeyPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "backpack_hotkey"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, BackpackHotkeyPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, BackpackHotkeyPacket::hotKey,
            BackpackHotkeyPacket::new
    );
}
