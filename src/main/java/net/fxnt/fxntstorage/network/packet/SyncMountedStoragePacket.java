package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

public record SyncMountedStoragePacket(int contraptionId, BlockPos localPos, EnumProperties.StorageUsed fillLevel,
                                       CompoundTag nbt) implements CustomPacketPayload {
    public static final Type<SyncMountedStoragePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sync_mounted_storage"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMountedStoragePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncMountedStoragePacket::contraptionId,
            BlockPos.STREAM_CODEC, SyncMountedStoragePacket::localPos,
            NeoForgeStreamCodecs.enumCodec(EnumProperties.StorageUsed.class), SyncMountedStoragePacket::fillLevel,
            ByteBufCodecs.COMPOUND_TAG, SyncMountedStoragePacket::nbt,
            SyncMountedStoragePacket::new
    );

}
