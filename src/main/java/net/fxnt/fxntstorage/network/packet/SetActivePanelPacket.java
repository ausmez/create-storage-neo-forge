package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetActivePanelPacket(UpgradeType upgradeType) {

    public static void encode(SetActivePanelPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.upgradeType);
    }

    public static SetActivePanelPacket decode(FriendlyByteBuf buffer) {
        UpgradeType upgradeType = buffer.readEnum(UpgradeType.class);
        return new SetActivePanelPacket(upgradeType);
    }

    public static void handle(final SetActivePanelPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer serverPlayer = context.get().getSender();
            if (serverPlayer == null) return;

            if (serverPlayer.containerMenu instanceof BackpackMenu menu
                    && packet.upgradeType.hasPanel()) {
                menu.togglePanelExpanded(packet.upgradeType);
                menu.updateBackpackDataFromContainer();
            }
        });
        context.get().setPacketHandled(true);
    }
}
