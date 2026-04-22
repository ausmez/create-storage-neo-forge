package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataManager;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataSync;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeHelper;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackHandler;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackManager;
import net.fxnt.fxntstorage.backpack.upgrade.oremining.OreMiningUpgrade;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.Tags;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record KeyPressedPacket(byte hotkey, boolean pressed, @Nullable BlockPos pos) {

    public static void encode(KeyPressedPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.hotkey);
        buffer.writeBoolean(packet.pressed);
        buffer.writeNullable(packet.pos, FriendlyByteBuf::writeBlockPos);
    }

    public static KeyPressedPacket decode(FriendlyByteBuf buffer) {
        byte hotkey = buffer.readByte();
        boolean pressed = buffer.readBoolean();
        BlockPos pos = buffer.readNullable(FriendlyByteBuf::readBlockPos);
        return new KeyPressedPacket(hotkey, pressed, pos);
    }

    public static void handle(KeyPressedPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();

            if (player != null) {
                switch (packet.hotkey()) {
                    case Util.JETPACK_KEY_PRESS -> JetpackManager.getJetpackHandler(player).flyingOnKeyPress();
                    case Util.JETPACK_KEY_RELEASE -> JetpackManager.getJetpackHandler(player).flyingOnKeyRelease();
                    case Util.OPEN_BACKPACK ->
                            BackpackHelper.openBackpackFromInventory(player, BackpackMenu.BackpackType.WORN);
                    case Util.CLOSE_BACKPACK -> {
                        if (player.containerMenu instanceof BackpackMenu) player.closeContainer();
                    }
                    case Util.BACKPACK_MENU_CTRL -> {
                        if (player.containerMenu instanceof BackpackMenu backpackMenu) {
                            backpackMenu.setCtrlKeyDown(packet.pressed());
                        }
                    }
                    case Util.TOGGLE_HOVER -> {
                        JetpackHandler jetpackHandler = JetpackManager.getJetpackHandler(player);
                        if (jetpackHandler.calculateJetPackFuel(player) > 0) {
                            jetpackHandler.toggleHover();
                        }
                    }
                    case Util.MINE_ALL_BLOCKS -> {
                        player.getPersistentData()
                                .getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG)
                                .putBoolean("MineAllBlocks", packet.pressed());

                        if (!packet.pressed()) {
                            ModNetwork.sendToPlayer(player, new OreMiningPreviewPacket(new ArrayList<>()));
                            return;
                        }

                        if (packet.pos() == null) return;

                        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
                        BackpackContainer container = new BackpackContainer(player, backpack);
                        UpgradeDataManager manager = UpgradeDataManager.loadFromItem(backpack);

                        boolean isUpgradeActive = UpgradeHelper.hasActiveUpgrade(container.getItemHandler(), UpgradeType.OREMINING);
                        boolean isMineOresOnly = manager.getSetting(UpgradeDataSync.Field.OREMINING_ORES_ONLY);
                        boolean isPreviewOreVeinsAllowed = manager.getSetting(UpgradeDataSync.Field.OREMINING_PREVIEW_ORE_VEIN);
                        boolean isServerPreviewAllowed = ConfigManager.ServerConfig.ORE_MINING_PREVIEW_ORE_VEIN.get();
                        boolean isStartBlockAnOre = player.level().getBlockState(packet.pos()).is(Tags.Blocks.ORES);

                        if (isUpgradeActive && isPreviewOreVeinsAllowed && isServerPreviewAllowed) {
                            List<BlockPos> vein = OreMiningUpgrade.findVein(
                                    player,
                                    player.level(),
                                    packet.pos(),
                                    player.level().getBlockState(packet.pos()),
                                    isStartBlockAnOre || !isMineOresOnly,
                                    64
                            );
                            ModNetwork.sendToPlayer(player, new OreMiningPreviewPacket(vein));
                        } else {
                            ModNetwork.sendToPlayer(player, new OreMiningPreviewPacket(new ArrayList<>()));
                        }
                    }
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
