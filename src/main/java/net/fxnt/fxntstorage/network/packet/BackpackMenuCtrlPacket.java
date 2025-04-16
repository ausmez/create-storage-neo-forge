package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BackpackMenuCtrlPacket(boolean ctrlKeyDown) implements CustomPacketPayload {
    public static final Type<BackpackMenuCtrlPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "backpack_ctrl_key_down"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, BackpackMenuCtrlPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, packet -> (packet.ctrlKeyDown),
            BackpackMenuCtrlPacket::new
    );

}
