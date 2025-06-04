package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetMountedStorageDirtyPacket(int entityId, BlockPos localPos) implements CustomPacketPayload {
    public static final Type<SetMountedStorageDirtyPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "set_mounted_storage_dirty"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SetMountedStorageDirtyPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SetMountedStorageDirtyPacket::entityId,
            BlockPos.STREAM_CODEC, SetMountedStorageDirtyPacket::localPos,
            SetMountedStorageDirtyPacket::new
            );

}
