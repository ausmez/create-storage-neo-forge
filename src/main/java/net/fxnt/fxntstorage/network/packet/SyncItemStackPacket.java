package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncItemStackPacket(DataComponentMap dataMap) implements CustomPacketPayload {
    public static final Type<SyncItemStackPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sync_itemstack"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncItemStackPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodecWithRegistries(DataComponentMap.CODEC), SyncItemStackPacket::dataMap,
            SyncItemStackPacket::new
    );
}
