package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record JetpackFuelSyncPacket(float fuelRemaining, long serverTime) implements CustomPacketPayload {
    public static final Type<JetpackFuelSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "jetpack_fuel_sync"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, JetpackFuelSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, JetpackFuelSyncPacket::fuelRemaining,
            ByteBufCodecs.VAR_LONG, JetpackFuelSyncPacket::serverTime,
            JetpackFuelSyncPacket::new
    );
}
