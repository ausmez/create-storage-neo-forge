package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncClientSettingsPacket(CompoundTag settings) implements CustomPacketPayload {
    public static final Type<SyncClientSettingsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sync_client_settings"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, SyncClientSettingsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, SyncClientSettingsPacket::settings,
            SyncClientSettingsPacket::new
    );

}
