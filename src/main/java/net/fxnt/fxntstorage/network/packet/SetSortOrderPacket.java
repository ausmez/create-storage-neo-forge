package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.container.StorageBoxMenu;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedMenu;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetSortOrderPacket(SortOrder sortOrder) implements CustomPacketPayload {
    public static final Type<SetSortOrderPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "set_sort_order"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetSortOrderPacket> STREAM_CODEC = StreamCodec.composite(
            SortOrder.STREAM_CODEC, SetSortOrderPacket::sortOrder,
            SetSortOrderPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {

                if (player.containerMenu instanceof BackpackMenu menu) {
                    menu.container.setSortOrder(sortOrder());
                    menu.container.setDataChanged();
                }

                if (player.containerMenu instanceof StorageBoxMenu menu) {
                    menu.setSortOrder(sortOrder());
                }

                if (player.containerMenu instanceof StorageBoxMountedMenu menu) {
                    menu.setSortOrder(sortOrder());
                }
            }
        });
    }
}
