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

public record UpgradeDataPacket(int setting, boolean value) implements CustomPacketPayload {

    public static final Type<UpgradeDataPacket> TYPE = new Type<>(FXNTStorage.modLoc("upgrade_data"));

    public static final StreamCodec<FriendlyByteBuf, UpgradeDataPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, UpgradeDataPacket::setting,
            ByteBufCodecs.BOOL, UpgradeDataPacket::value,
            UpgradeDataPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof BackpackMenu menu) {
                UpgradeDataSync.Field field = UpgradeDataSync.Field.fromIndex(setting);
                if (field != null) {
                    menu.setUpgradeSetting(field, value);
                } else {
                    FXNTStorage.LOGGER.debug("Invalid setting received in UpgradeDataPacket({}): {}", value, setting);
                }
            }
        });
    }
}