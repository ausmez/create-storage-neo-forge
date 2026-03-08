package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpgradeDataPacket(int setting, boolean value) {

    public static void encode(UpgradeDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.setting);
        buffer.writeBoolean(packet.value);
    }

    public static UpgradeDataPacket decode(FriendlyByteBuf buffer) {
        int setting = buffer.readInt();
        boolean value = buffer.readBoolean();
        return new UpgradeDataPacket(setting, value);
    }

    public static void handle(final UpgradeDataPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;

            if (player.containerMenu instanceof BackpackMenu menu) {
                UpgradeDataSync.Field field = UpgradeDataSync.Field.fromIndex(packet.setting);
                if (field != null) {
                    menu.setUpgradeSetting(field, packet.value);
                } else {
                    FXNTStorage.LOGGER.debug("Invalid setting received in UpgradeDataPacket({}): {}", packet.value, packet.setting);
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}