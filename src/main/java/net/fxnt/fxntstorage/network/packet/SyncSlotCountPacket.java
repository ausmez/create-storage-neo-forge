package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record SyncSlotCountPacket(int containerId, int stateId, int slot,
                                  ItemStack stack) implements CustomPacketPayload {
    public static final Type<SyncSlotCountPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sync_slot_count"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSlotCountPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncSlotCountPacket::containerId,
            ByteBufCodecs.INT, SyncSlotCountPacket::stateId,
            ByteBufCodecs.INT, SyncSlotCountPacket::slot,
            ItemStack.OPTIONAL_STREAM_CODEC, SyncSlotCountPacket::stack,
            SyncSlotCountPacket::new
    );
}
