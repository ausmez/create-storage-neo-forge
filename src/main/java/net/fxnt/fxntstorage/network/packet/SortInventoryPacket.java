package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SortInventoryPacket(byte invType, int slotStart, int slotEnd, SortOrder sortOrder) implements CustomPacketPayload {
    public static final Type<SortInventoryPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sort_inventory"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, SortInventoryPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, SortInventoryPacket::invType,
            ByteBufCodecs.INT, SortInventoryPacket::slotStart,
            ByteBufCodecs.INT, SortInventoryPacket::slotEnd,
            SortOrder.STREAM_CODEC, SortInventoryPacket::sortOrder,
            SortInventoryPacket::new
    );

}
