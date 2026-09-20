package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpgradeIntDataPacket(int setting, int value) implements CustomPacketPayload {

    public static final Type<UpgradeIntDataPacket> TYPE = new Type<>(FXNTStorage.modLoc("upgrade_int_data"));

    public static final StreamCodec<FriendlyByteBuf, UpgradeIntDataPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, UpgradeIntDataPacket::setting,
            ByteBufCodecs.INT, UpgradeIntDataPacket::value,
            UpgradeIntDataPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof BackpackMenu menu) {
                UpgradeDataSync.Field field = UpgradeDataSync.Field.fromIndex(setting);
                if (field != null && field.isInteger()) {
                    menu.setUpgradeIntSetting(field, value);
                } else {
                    FXNTStorage.LOGGER.debug("Invalid setting received in UpgradeIntDataPacket({}): {}", value, setting);
                }
            }
        });
    }
}
