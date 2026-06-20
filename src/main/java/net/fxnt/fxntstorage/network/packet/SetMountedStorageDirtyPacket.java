package net.fxnt.fxntstorage.network.packet;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedStorage;
import net.fxnt.fxntstorage.reserve_storage.mounted.ReserveStorageBoxMountedStorage;
import net.fxnt.fxntstorage.simple_storage.mounted.SimpleStorageBoxMountedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetMountedStorageDirtyPacket(int entityId, BlockPos localPos) implements CustomPacketPayload {
    public static final Type<SetMountedStorageDirtyPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "set_mounted_storage_dirty"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetMountedStorageDirtyPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SetMountedStorageDirtyPacket::entityId,
            BlockPos.STREAM_CODEC, SetMountedStorageDirtyPacket::localPos,
            SetMountedStorageDirtyPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {

                Entity entity = player.level().getEntity(entityId());
                if (entity instanceof AbstractContraptionEntity contraptionEntity) {
                    MountedItemStorage storage = contraptionEntity.getContraption().getStorage().getAllItemStorages().get(localPos());
                    if (storage instanceof SimpleStorageBoxMountedStorage simpleStorageBox) {
                        simpleStorageBox.markDirty();
                    } else if (storage instanceof StorageBoxMountedStorage storageBox) {
                        storageBox.markDirty();
                    } else if (storage instanceof ReserveStorageBoxMountedStorage reserveStorageBox) {
                        reserveStorageBox.markDirty();
                    }
                }
            }
        });
    }
}
