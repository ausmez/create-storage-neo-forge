package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackHandler;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record JetpackFuelSyncPacket(float fuelRemaining, long serverTime) implements CustomPacketPayload {
    public static final Type<JetpackFuelSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "jetpack_fuel_sync"));

    public static final StreamCodec<FriendlyByteBuf, JetpackFuelSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, JetpackFuelSyncPacket::fuelRemaining,
            ByteBufCodecs.VAR_LONG, JetpackFuelSyncPacket::serverTime,
            JetpackFuelSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof Player player) {
                JetpackHandler handler = JetpackManager.getJetpackHandler(player);
                if (handler != null) {
                    handler.onFuelSync(fuelRemaining(), serverTime());
                }
            }
        });
    }
}
