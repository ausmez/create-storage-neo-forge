package net.fxnt.fxntstorage.network.packet;

import io.netty.buffer.ByteBuf;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.controller.StorageControllerHighlight;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

public record StorageNetworkSyncPacket(BlockPos controller, Set<BlockPos> boxes) implements CustomPacketPayload {
    public static final Type<StorageNetworkSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "connected_storage_boxes"));

    public static final StreamCodec<ByteBuf, Set<BlockPos>> SET_STREAM_CODEC = BlockPos.STREAM_CODEC.apply(
            ByteBufCodecs.collection(HashSet::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageNetworkSyncPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StorageNetworkSyncPacket::controller,
            SET_STREAM_CODEC, StorageNetworkSyncPacket::boxes,
            StorageNetworkSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> StorageControllerHighlight.set(controller(), boxes()));
    }
}
