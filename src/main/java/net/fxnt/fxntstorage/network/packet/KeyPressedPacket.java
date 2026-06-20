package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
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
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record KeyPressedPacket(byte hotKey, boolean pressed, Optional<BlockPos> pos) implements CustomPacketPayload {
    public static final Type<KeyPressedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "key_pressed"));

    public static final StreamCodec<FriendlyByteBuf, KeyPressedPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, KeyPressedPacket::hotKey,
            ByteBufCodecs.BOOL, KeyPressedPacket::pressed,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), KeyPressedPacket::pos,
            KeyPressedPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                switch (hotKey()) {
                    case Util.JETPACK_KEY_PRESS -> JetpackManager.getJetpackHandler(player).flyingOnKeyPress();
                    case Util.JETPACK_KEY_RELEASE -> JetpackManager.getJetpackHandler(player).flyingOnKeyRelease();
                    case Util.OPEN_BACKPACK ->
                            BackpackHelper.openBackpackFromInventory(player, BackpackMenu.BackpackType.WORN);
                    case Util.CLOSE_BACKPACK -> {
                        if (player.containerMenu instanceof BackpackMenu) player.closeContainer();
                    }
                    case Util.BACKPACK_MENU_CTRL -> {
                        if (player.containerMenu instanceof BackpackMenu backpackMenu) {
                            backpackMenu.setCtrlKeyDown(pressed);
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
                                .putBoolean("MineAllBlocks", pressed());

                        pos().ifPresentOrElse(blockPos -> {
                            ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
                            BackpackContainer container = new BackpackContainer(player, backpack);
                            UpgradeDataManager manager = UpgradeDataManager.loadFromItem(backpack);

                            boolean isUpgradeActive = UpgradeHelper.hasActiveUpgrade(container.getItemHandler(), UpgradeType.OREMINING);
                            boolean isMineOresOnly = manager.getSetting(UpgradeDataSync.Field.OREMINING_ORES_ONLY);
                            boolean isPreviewOreVeinsAllowed = manager.getSetting(UpgradeDataSync.Field.OREMINING_PREVIEW_ORE_VEIN);
                            boolean isServerPreviewAllowed = ConfigManager.ServerConfig.ORE_MINING_PREVIEW_ORE_VEIN.get();
                            boolean isServerOresOnlyOverride = ConfigManager.ServerConfig.ORE_MINING_ORES_ONLY.get();
                            boolean isStartBlockAnOre = player.level().getBlockState(blockPos).is(Tags.Blocks.ORES);

                            if (isUpgradeActive && isPreviewOreVeinsAllowed && isServerPreviewAllowed && pressed()) {
                                List<BlockPos> vein = OreMiningUpgrade.findVein(player,
                                        player.level(),
                                        blockPos,
                                        player.level().getBlockState(blockPos),
                                        !isServerOresOnlyOverride && (isStartBlockAnOre || !isMineOresOnly),
                                        64
                                );
                                PacketDistributor.sendToPlayer(player, new OreMiningPreviewPacket(vein));
                            } else {
                                PacketDistributor.sendToPlayer(player, new OreMiningPreviewPacket(new ArrayList<>()));
                            }
                        }, () -> PacketDistributor.sendToPlayer(player, new OreMiningPreviewPacket(new ArrayList<>())));
                    }
                }
            }
        });
    }
}
