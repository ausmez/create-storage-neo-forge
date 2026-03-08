package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.container.StorageBoxMenu;
import net.fxnt.fxntstorage.container.mounted.StorageBoxMountedMenu;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SortInventoryPacket(byte invType, int slotStart, int slotEnd, SortOrder sortOrder) implements CustomPacketPayload {
    public static final Type<SortInventoryPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sort_inventory"));

    public static final StreamCodec<FriendlyByteBuf, SortInventoryPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, SortInventoryPacket::invType,
            ByteBufCodecs.INT, SortInventoryPacket::slotStart,
            ByteBufCodecs.INT, SortInventoryPacket::slotEnd,
            SortOrder.STREAM_CODEC, SortInventoryPacket::sortOrder,
            SortInventoryPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            // Backpack sorting
            if (invType() == Util.INV_TYPE_BACKPACK && player.containerMenu instanceof BackpackMenu menu)
                menu.sortBackpackItems(slotStart(), sortOrder());

            if (invType() == Util.INV_TYPE_STORAGE_BOX) {
                // StorageBox sorting
                if (player.containerMenu instanceof StorageBoxMenu menu)
                    menu.sortStorageItems(slotStart(), slotEnd(), sortOrder());

                // StorageBoxMounted sorting
                if (player.containerMenu instanceof StorageBoxMountedMenu menu)
                    menu.sortStorageItems(slotStart(), slotEnd(), sortOrder());
            }
        });
    }
}
