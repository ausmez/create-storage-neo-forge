package net.fxnt.fxntstorage.network.packet;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxMenu;
import net.fxnt.fxntstorage.reserve_storage.mounted.ReserveStorageBoxMountedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record ReserveStorageBoxGhostPacket(ItemStack item, int slot, Optional<Integer> contraptionId,
                                           Optional<BlockPos> localPos) implements CustomPacketPayload {

    public static final Type<ReserveStorageBoxGhostPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "reserve_storage_ghost"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReserveStorageBoxGhostPacket> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, ReserveStorageBoxGhostPacket::item,
            ByteBufCodecs.INT, ReserveStorageBoxGhostPacket::slot,
            ByteBufCodecs.optional(ByteBufCodecs.INT), ReserveStorageBoxGhostPacket::contraptionId,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), ReserveStorageBoxGhostPacket::localPos,
            ReserveStorageBoxGhostPacket::new
    );

    public static ReserveStorageBoxGhostPacket forBlock(ItemStack item, int slot) {
        return new ReserveStorageBoxGhostPacket(item, slot, Optional.empty(), Optional.empty());
    }

    public static ReserveStorageBoxGhostPacket forMounted(ItemStack item, int slot, int contraptionId, BlockPos localPos) {
        return new ReserveStorageBoxGhostPacket(item, slot, Optional.of(contraptionId), Optional.of(localPos));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof ReserveStorageBoxMenu menu)) return;

            if (contraptionId.isPresent() && localPos.isPresent()) {
                // Mounted contraption
                Entity entity = player.level().getEntity(contraptionId.get());
                if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) return;

                MountedItemStorage storage = contraptionEntity.getContraption().getStorage()
                        .getAllItemStorages().get(localPos.get());
                if (!(storage instanceof ReserveStorageBoxMountedStorage mountedStorage)) return;
                if (mountedStorage.isGhostDuplicate(slot, item)) return;

                mountedStorage.setGhostSlot(slot, item);
            } else {
                // Block entity
                if (menu.isGhostDuplicate(slot, item)) return;
                menu.blockEntity.getItemHandler().setStackInSlot(slot, item);
            }
        });
    }
}
