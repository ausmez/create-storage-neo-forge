package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncDataComponentPacket(DataComponentPatch component) implements CustomPacketPayload {
    public static final Type<SyncDataComponentPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sync_data_component"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDataComponentPacket> STREAM_CODEC = CustomPacketPayload.codec(
            SyncDataComponentPacket::write,
            SyncDataComponentPacket::new);

    public SyncDataComponentPacket(RegistryFriendlyByteBuf buf) {
        this(DataComponentPatch.STREAM_CODEC.decode(buf));
    }

    public void write(RegistryFriendlyByteBuf buf) {
        if (!component().isEmpty())
            DataComponentPatch.STREAM_CODEC.encode(buf, component);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
