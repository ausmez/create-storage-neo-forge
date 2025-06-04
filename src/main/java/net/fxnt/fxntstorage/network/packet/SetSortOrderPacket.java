package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetSortOrderPacket(SortOrder sortOrder) implements CustomPacketPayload {
    public static final Type<SetSortOrderPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "set_sort_order"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SetSortOrderPacket> STREAM_CODEC = StreamCodec.composite(
            SortOrder.STREAM_CODEC, SetSortOrderPacket::sortOrder,
            SetSortOrderPacket::new
    );

}
