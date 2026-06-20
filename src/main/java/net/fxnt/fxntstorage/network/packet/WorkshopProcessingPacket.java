package net.fxnt.fxntstorage.network.packet;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.fxnt.fxntstorage.backpack.upgrade.workshop.WorkshopClientState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record WorkshopProcessingPacket(int entityId, boolean processing, Optional<BlockPos> localPos) implements CustomPacketPayload {
    public static final Type<WorkshopProcessingPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "workshop_processing"));

    public static final StreamCodec<FriendlyByteBuf, WorkshopProcessingPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WorkshopProcessingPacket::entityId,
            ByteBufCodecs.BOOL, WorkshopProcessingPacket::processing,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), WorkshopProcessingPacket::localPos,
            WorkshopProcessingPacket::new
    );

    public static WorkshopProcessingPacket forEntity(int entityId, boolean processing) {
        return new WorkshopProcessingPacket(entityId, processing, Optional.empty());
    }

    public static WorkshopProcessingPacket forContraption(int contraptionId, boolean processing, BlockPos localPos) {
        return new WorkshopProcessingPacket(contraptionId, processing, Optional.of(localPos));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (localPos.isPresent()) {
                // Mounted contraption
                Player player = context.player();
                Entity entity = player.level().getEntity(entityId);
                if (!(entity instanceof AbstractContraptionEntity contraptionEntity)) return;

                // Keep the stored block-entity NBT in step so a later contraption rebuild reflects the
                // current state without a packet.
                StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos.get());
                if (info != null && info.nbt() != null) {
                    CompoundTag nbt = info.nbt();
                    nbt.putBoolean("WorkshopProcessing", processing());
                    contraptionEntity.getContraption().getBlocks().put(localPos.get(),
                            new StructureTemplate.StructureBlockInfo(info.pos(), info.state(), nbt));
                }

                // Update the already-rendered block entity in place so its flywheel inertia is preserved.
                BlockEntity be = contraptionEntity.getContraption().getBlockEntityClientSide(localPos.get());
                if (be instanceof BackpackEntity backpack) {
                    backpack.setWorkshopProcessing(processing());
                }

            } else {
                // Player entity
                WorkshopClientState.setProcessing(entityId, processing);
            }
        });
    }
}
