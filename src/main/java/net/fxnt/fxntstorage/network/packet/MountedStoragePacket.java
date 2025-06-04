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

public record MountedStoragePacket(int contraptionId, BlockPos localPos, EnumProperties.StorageUsed fillLevel,
                                   CompoundTag nbt) implements CustomPacketPayload {
    public static final Type<MountedStoragePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sync_mounted_storage"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, MountedStoragePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, MountedStoragePacket::contraptionId,
            BlockPos.STREAM_CODEC, MountedStoragePacket::localPos,
            NeoForgeStreamCodecs.enumCodec(EnumProperties.StorageUsed.class), MountedStoragePacket::fillLevel,
            ByteBufCodecs.COMPOUND_TAG, MountedStoragePacket::nbt,
            MountedStoragePacket::new
    );

}
