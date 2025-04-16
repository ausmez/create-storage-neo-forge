package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SyncClientSettingsPacket(List<String> prefersSilkTouchList, boolean preferSilkTouch, boolean ignoreFanProcessing, boolean displayFeederMessage) implements CustomPacketPayload {
    public static final Type<SyncClientSettingsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sync_client_settings"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, SyncClientSettingsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncClientSettingsPacket::prefersSilkTouchList,
            ByteBufCodecs.BOOL, SyncClientSettingsPacket::preferSilkTouch,
            ByteBufCodecs.BOOL, SyncClientSettingsPacket::ignoreFanProcessing,
            ByteBufCodecs.BOOL, SyncClientSettingsPacket::displayFeederMessage,
            SyncClientSettingsPacket::new
    );

}
