package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncDataComponentPacket(DataComponentPatch component) implements CustomPacketPayload {
    public static final Type<SyncDataComponentPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sync_data_component"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDataComponentPacket> STREAM_CODEC = StreamCodec.composite(
            DataComponentPatch.STREAM_CODEC, SyncDataComponentPacket::component,
            SyncDataComponentPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof Player player) {
                ItemStack selectedItem = player.getMainHandItem();
                if (selectedItem.getItem() instanceof BackpackItem) {
                    selectedItem.applyComponents(component());
                }
                ItemStack wornItem = BackpackHelper.getEquippedBackpackStack(player);
                if (wornItem.getItem() instanceof BackpackItem) {
                    wornItem.applyComponents(component());
                }
            }
        });
    }
}
