package net.fxnt.fxntstorage.network.packet;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedStorage;
import net.fxnt.fxntstorage.simple_storage.mounted.SimpleStorageBoxMountedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetMountedStorageDirtyPacket(int contraptionId, BlockPos localPos) {

    public static void encode(SetMountedStorageDirtyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.contraptionId);
        buffer.writeBlockPos(packet.localPos);
    }

    public static SetMountedStorageDirtyPacket decode(FriendlyByteBuf buffer) {
        int contraptionId = buffer.readInt();
        BlockPos localPos = buffer.readBlockPos();
        return new SetMountedStorageDirtyPacket(contraptionId, localPos);
    }

    public static void handle(SetMountedStorageDirtyPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();

            if (player != null) {
                Entity entity = player.level().getEntity(packet.contraptionId());

                if (entity instanceof AbstractContraptionEntity contraptionEntity) {
                    MountedItemStorage storage = contraptionEntity.getContraption().getStorage().getAllItemStorages().get(packet.localPos());
                    if (storage instanceof SimpleStorageBoxMountedStorage) {
                        ((SimpleStorageBoxMountedStorage) storage).markDirty();
                    } else if (storage instanceof StorageBoxMountedStorage) {
                        ((StorageBoxMountedStorage) storage).markDirty();
                    }
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
