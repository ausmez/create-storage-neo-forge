package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record TransferRecipePacket(ResourceLocation recipeId, List<Integer> recipeList,
                                   boolean maxTransfer, int action) implements CustomPacketPayload {
    public static final Type<TransferRecipePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "transfer_recipe"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TransferRecipePacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, TransferRecipePacket::recipeId,
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), TransferRecipePacket::recipeList,
            ByteBufCodecs.BOOL, TransferRecipePacket::maxTransfer,
            ByteBufCodecs.INT, TransferRecipePacket::action,
            TransferRecipePacket::new
    );
}
