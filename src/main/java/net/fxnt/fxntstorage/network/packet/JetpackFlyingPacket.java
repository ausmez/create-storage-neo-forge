package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record JetpackFlyingPacket(boolean flying) implements CustomPacketPayload {
    public static final Type<JetpackFlyingPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "jetpack_flying"));

    public static final StreamCodec<FriendlyByteBuf, JetpackFlyingPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, JetpackFlyingPacket::flying,
            JetpackFlyingPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                JetpackManager.getJetpackHandler(player).processPlayerFlyingPacket(flying());
            }
        });
    }
}
