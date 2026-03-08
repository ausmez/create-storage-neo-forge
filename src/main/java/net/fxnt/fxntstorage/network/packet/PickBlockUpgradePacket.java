package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeEventDispatcher;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PickBlockUpgradePacket(ItemStack stack) implements CustomPacketPayload {
    public static final Type<PickBlockUpgradePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "pick_block_upgrade"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PickBlockUpgradePacket> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, PickBlockUpgradePacket::stack,
            PickBlockUpgradePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                UpgradeEventDispatcher.dispatchPickBlock(player, stack());
            }
        });
    }
}
