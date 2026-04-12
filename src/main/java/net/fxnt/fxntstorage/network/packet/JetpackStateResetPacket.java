package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record JetpackStateResetPacket() implements CustomPacketPayload {
    public static final Type<JetpackStateResetPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "jetpack_state_reset"));

    public static final StreamCodec<FriendlyByteBuf, JetpackStateResetPacket> STREAM_CODEC =
            StreamCodec.unit(new JetpackStateResetPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof Player player) {
                JetpackManager.getJetpackHandler(player).endHovering(false);
                JetpackManager.getJetpackHandler(player).flyingOnKeyRelease();
            }
        });
    }
}
