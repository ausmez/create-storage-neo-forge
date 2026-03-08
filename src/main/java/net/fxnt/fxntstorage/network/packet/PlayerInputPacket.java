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

public record PlayerInputPacket(float forwardImpulse, float leftImpulse) implements CustomPacketPayload {
    public static final Type<PlayerInputPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "player_input"));

    public static final StreamCodec<FriendlyByteBuf, PlayerInputPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, PlayerInputPacket::forwardImpulse,
            ByteBufCodecs.FLOAT, PlayerInputPacket::leftImpulse,
            PlayerInputPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                JetpackManager.getJetpackHandler(player).processPlayerInputPacket(forwardImpulse(), leftImpulse());
            }
        });
    }
}
