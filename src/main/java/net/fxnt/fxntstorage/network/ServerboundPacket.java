package net.fxnt.fxntstorage.network;

import net.fxnt.fxntstorage.backpacks.main.BackpackMenu;
import net.fxnt.fxntstorage.backpacks.upgrades.BackpackOnBackUpgradeHandler;
import net.fxnt.fxntstorage.backpacks.upgrades.JetpackHandler;
import net.fxnt.fxntstorage.backpacks.upgrades.JetpackManager;
import net.fxnt.fxntstorage.backpacks.util.BackpackHandler;
import net.fxnt.fxntstorage.backpacks.util.BackpackNetworkHelper;
import net.fxnt.fxntstorage.containers.StorageBoxMenu;
import net.fxnt.fxntstorage.containers.util.StorageBoxNetworkHelper;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ServerboundPacket {
    private final ResourceLocation packetId;
    private final FriendlyByteBuf data;

    public ServerboundPacket(ResourceLocation packetId, FriendlyByteBuf data) {
        this.packetId = packetId;
        this.data = data;
    }

    public static void encoder(@NotNull ServerboundPacket packet, @NotNull FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.packetId);
        buf.writeBytes(packet.data);
    }

    public static @NotNull ServerboundPacket decoder(@NotNull FriendlyByteBuf buf) {
        ResourceLocation packetId = buf.readResourceLocation();
        FriendlyByteBuf data = new FriendlyByteBuf(buf.copy()); // Copy data buffer
        return new ServerboundPacket(packetId, data);
    }

    public static void handler(ServerboundPacket packet, @NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender(); // Get the player who sent the packet (on server side)

        // Ensure this runs on the main thread
        context.enqueueWork(() -> {

            if (packet.packetId.equals(BackpackNetworkHelper.BACKPACK_MENU_CTRL_DOWN)) {
                if (player != null && player.containerMenu instanceof BackpackMenu backPackMenu) {
                    backPackMenu.ctrlKeyDown = true;
                }
            }

            if (packet.packetId.equals(BackpackNetworkHelper.BACKPACK_MENU_CTRL_UP)) {
                if (player != null && player.containerMenu instanceof BackpackMenu backPackMenu) {
                    backPackMenu.ctrlKeyDown = false;
                }
            }

            if (packet.packetId.equals(BackpackNetworkHelper.BACKPACK_KEY_OPEN_CLOSE)) {
                if (player != null) {
                    byte key = packet.data.readByte();
                    if (key == Util.OPEN_BACKPACK)
                        BackpackHandler.openBackpackFromInventory(player, Util.BACKPACK_ON_BACK);
                    if (key == Util.CLOSE_BACKPACK && player.containerMenu instanceof BackpackMenu)
                        player.closeContainer();
                    if (key == Util.TOGGLE_HOVER) {
                        JetpackHandler jetpackHandler = JetpackManager.getJetpackHandler(player);
                        jetpackHandler.toggleHover();

                    }
                }
            }

            if (packet.packetId.equals(BackpackNetworkHelper.JETPACK_FLY)) {
                if (player != null) {
                    byte type = packet.data.readByte();
                    if (type == Util.JETPACK_KEY_PRESS) {
                        JetpackHandler.flyingOnKeyPress(player);
                    }
                    if (type == Util.JETPACK_KEY_RELEASE) {
                        JetpackHandler.flyingOnKeyRelease(player);
                    }
                }
            }

            if (packet.packetId.equals(BackpackNetworkHelper.PLAYER_INPUT)) {
                if (player != null) {
                    // TODO: Have no clue if this is the best way??
                    JetpackHandler.processPlayerInputPacket(player, packet.data.readFloat(), packet.data.readFloat());
                }
            }

            if (packet.packetId.equals(BackpackNetworkHelper.SORT_BACKPACK_INVENTORY)) {
                if (player != null) {
                    if (player.containerMenu instanceof BackpackMenu menu) {
                        menu.sortBackpackItems(packet.data.readInt(), packet.data.readInt(), packet.data.readByte());
                    }
                }
            }

            if (packet.packetId.equals(StorageBoxNetworkHelper.SORT_STORAGE_BOX_INVENTORY)) {
                if (player != null) {
                    if (player.containerMenu instanceof StorageBoxMenu menu) {
                        menu.sortStorageItems(packet.data.readInt(), packet.data.readInt(), packet.data.readByte());
                    }
                }
            }

            if (packet.packetId.equals(BackpackNetworkHelper.TOGGLE_HOVER)) {
                if (player != null) {
                    JetpackHandler jetpackHandler = JetpackManager.getJetpackHandler(player);
                    if (JetpackHandler.calculateJetPackFuel(player) > 0.0) {
                        boolean hasFlightUpgrade = new BackpackOnBackUpgradeHandler(player).hasUpgrade(Util.FLIGHT_UPGRADE);
                        if (hasFlightUpgrade)
                            jetpackHandler.toggleHover();
                    }
                }
            }

            if (packet.packetId.equals(BackpackNetworkHelper.SYNC_CLIENT_SETTINGS)) {
                if (player != null) {
                    int listSize = packet.data.readInt();
                    ListTag prefersSilkTouchList = new ListTag();
                    for (int i = 0; i < listSize; i++) {
                        prefersSilkTouchList.add(StringTag.valueOf(packet.data.readUtf()));
                    }
                    boolean preferSilkTouch = packet.data.readBoolean();
                    boolean ignoreFanProcessing = packet.data.readBoolean();
                    boolean displayFeederMessage = packet.data.readBoolean();

                    player.getPersistentData().putBoolean("fxntDisplayFeederMessage", displayFeederMessage);
                    player.getPersistentData().putBoolean("fxntIgnoreFanProcessing", ignoreFanProcessing);
                    player.getPersistentData().putBoolean("fxntPreferSilkTouch", preferSilkTouch);
                    player.getPersistentData().put("fxntPrefersSilkTouchList", prefersSilkTouchList);
                }
            }

            if (packet.packetId.equals(BackpackNetworkHelper.UPGRADE_PICK_BLOCK)) {
                new BackpackOnBackUpgradeHandler(player).applyPickBlockUpgrade(packet.data.readItem());
            }

        });
        context.setPacketHandled(true); // Mark the packet as handled
    }

}
