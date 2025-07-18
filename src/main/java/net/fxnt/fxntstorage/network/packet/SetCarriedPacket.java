package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record SetCarriedPacket(ItemStack stack) implements CustomPacketPayload {
    public static final Type<SetCarriedPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "set_carried_packet"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SetCarriedPacket> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, SetCarriedPacket::stack,
            SetCarriedPacket::new
    );

}
