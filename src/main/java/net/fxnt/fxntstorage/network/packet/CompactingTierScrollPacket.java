package net.fxnt.fxntstorage.network.packet;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.fxnt.fxntstorage.simple_storage.mounted.SimpleStorageBoxMountedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record CompactingTierScrollPacket(BlockPos pos, int tier,
                                         Optional<Integer> entityId) implements CustomPacketPayload {
    public static final Type<CompactingTierScrollPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "compacting_tier_scroll"));

    public static final StreamCodec<FriendlyByteBuf, CompactingTierScrollPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CompactingTierScrollPacket::pos,
            ByteBufCodecs.INT, CompactingTierScrollPacket::tier,
            ByteBufCodecs.optional(ByteBufCodecs.INT), CompactingTierScrollPacket::entityId,
            CompactingTierScrollPacket::new
    );

    public static CompactingTierScrollPacket forBlock(BlockPos pos, int tier) {
        return new CompactingTierScrollPacket(pos, tier, Optional.empty());
    }

    public static CompactingTierScrollPacket forMounted(BlockPos pos, int tier, int contraptionId) {
        return new CompactingTierScrollPacket(pos, tier, Optional.of(contraptionId));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (entityId.isPresent()) {
                    // Mounted contraption
                    Entity entity = player.level().getEntity(entityId().get());
                    if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) return;

                    MountedItemStorage storage = contraptionEntity.getContraption()
                            .getStorage().getAllItemStorages().get(pos());
                    if (!(storage instanceof SimpleStorageBoxMountedStorage ssb)) return;
                    if (ssb.compactingChain == null) return;

                    ssb.compactingSelectedTier = Mth.clamp(tier(), 0, ssb.compactingChain.tiers() - 1);
                    ssb.markDirty();
                } else {
                    // Block entity
                    BlockEntity blockEntity = player.level().getBlockEntity(pos());
                    if (blockEntity instanceof SimpleStorageBoxEntity ssb && ssb.compactingChain != null) {
                        ssb.compactingSelectedTier = Mth.clamp(tier(), 0, ssb.compactingChain.tiers() - 1);
                        ssb.setChanged();
                    }
                }
            }
        });
    }
}
