package net.fxnt.fxntstorage.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record SetMountedStorageDirtyPacket(int contraptionId, BlockPos localPos) {

    public static void encode(@NotNull SetMountedStorageDirtyPacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeInt(packet.contraptionId);
        buffer.writeBlockPos(packet.localPos);
    }

    public static @NotNull SetMountedStorageDirtyPacket decode(@NotNull FriendlyByteBuf buffer) {
        int contraptionId = buffer.readInt();
        BlockPos localPos = buffer.readBlockPos();
        return new SetMountedStorageDirtyPacket(contraptionId, localPos);
    }

}
