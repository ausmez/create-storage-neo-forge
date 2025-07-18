package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record SyncMountedStoragePacket(int contraptionId, BlockPos localPos, EnumProperties.StorageUsed fillLevel,
                                       CompoundTag nbt) {

    public static void encode(@NotNull SyncMountedStoragePacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeInt(packet.contraptionId);
        buffer.writeBlockPos(packet.localPos);
        buffer.writeEnum(packet.fillLevel);
        buffer.writeNbt(packet.nbt);
    }

    public static @NotNull SyncMountedStoragePacket decode(@NotNull FriendlyByteBuf buffer) {
        int contraptionId = buffer.readInt();
        BlockPos localPos = buffer.readBlockPos();
        EnumProperties.StorageUsed fillLevel = buffer.readEnum(EnumProperties.StorageUsed.class);
        CompoundTag nbt = buffer.readNbt();
        return new SyncMountedStoragePacket(contraptionId, localPos, fillLevel, nbt);
    }

}
