package net.fxnt.fxntstorage.network;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedStorage;
import net.fxnt.fxntstorage.simple_storage.mounted.SimpleStorageBoxMountedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SetMountedStorageDirtyPacket {
    private final int contraptionId;
    private final BlockPos localPos;

    public SetMountedStorageDirtyPacket(int contraptionId, BlockPos localPos) {
        this.contraptionId = contraptionId;
        this.localPos = localPos;
    }

    public static void encode(@NotNull SetMountedStorageDirtyPacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeInt(packet.contraptionId);
        buffer.writeBlockPos(packet.localPos);
    }

    public static @NotNull SetMountedStorageDirtyPacket decode(@NotNull FriendlyByteBuf buffer) {
        int contraptionId = buffer.readInt();
        BlockPos localPos = buffer.readBlockPos();
        return new SetMountedStorageDirtyPacket(contraptionId, localPos);
    }

    public static void handle(SetMountedStorageDirtyPacket packet, @NotNull Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender(); // Get the player who sent the packet (on server side)

        ctx.get().enqueueWork(() -> {
            Entity entity = player.level().getEntity(packet.contraptionId);
            if (entity instanceof AbstractContraptionEntity contraptionEntity) {
                MountedItemStorage storage = contraptionEntity.getContraption().getStorage().getAllItemStorages().get(packet.localPos);
                if (storage instanceof SimpleStorageBoxMountedStorage) {
                    ((SimpleStorageBoxMountedStorage) storage).markDirty();
                } else if (storage instanceof StorageBoxMountedStorage) {
                    ((StorageBoxMountedStorage) storage).markDirty();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

}
