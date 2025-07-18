package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.network.handler.ClientPayloadHandler;
import net.fxnt.fxntstorage.network.handler.ServerPayloadHandler;
import net.fxnt.fxntstorage.network.packet.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int pkt = 0;

    public static void registerCommonPackets() {
        // ClientboundPacket
        registerMessage(SetCarriedPacket.class, SetCarriedPacket::encode, SetCarriedPacket::decode, ClientPayloadHandler::handleSetCarriedPacket);
        registerMessage(SyncContainerPacket.class, SyncContainerPacket::encode, SyncContainerPacket::decode, ClientPayloadHandler::handleSyncContainerPacket);
        registerMessage(SyncSlotCountPacket.class, SyncSlotCountPacket::encode, SyncSlotCountPacket::decode, ClientPayloadHandler::handleSyncSlotCountPacket);
        registerMessage(VisualJetpackAirPacket.class, VisualJetpackAirPacket::encode, VisualJetpackAirPacket::decode, ClientPayloadHandler::handleVisualJetpackAirPacket);
        registerMessage(SyncMountedStoragePacket.class, SyncMountedStoragePacket::encode, SyncMountedStoragePacket::decode, ClientPayloadHandler::handleSyncMountedStoragePacket);
        registerMessage(SyncNBTDataPacket.class, SyncNBTDataPacket::encode, SyncNBTDataPacket::decode, ClientPayloadHandler::handleSyncNBTDataPacket);

        // ServerboundPacket
        registerMessage(KeyPressedPacket.class, KeyPressedPacket::encode, KeyPressedPacket::decode, ServerPayloadHandler::handleKeyPressedPacket);
        registerMessage(PickBlockUpgradePacket.class, PickBlockUpgradePacket::encode, PickBlockUpgradePacket::decode, ServerPayloadHandler::handlePickBlockUpgradePacket);
        registerMessage(PlayerInputPacket.class, PlayerInputPacket::encode, PlayerInputPacket::decode, ServerPayloadHandler::handlePlayerInputPacket);
        registerMessage(SetMountedStorageDirtyPacket.class, SetMountedStorageDirtyPacket::encode, SetMountedStorageDirtyPacket::decode, ServerPayloadHandler::handleSetMountedStorageDirtyPacket);
        registerMessage(SetSortOrderPacket.class, SetSortOrderPacket::encode, SetSortOrderPacket::decode, ServerPayloadHandler::handleSetSortOrderPacket);
        registerMessage(SortInventoryPacket.class, SortInventoryPacket::encode, SortInventoryPacket::decode, ServerPayloadHandler::handleSortInventoryPacket);
        registerMessage(SyncClientSettingsPacket.class, SyncClientSettingsPacket::encode, SyncClientSettingsPacket::decode, ServerPayloadHandler::handleSyncClientSettingsPacket);
        registerMessage(TransferRecipePacket.class, TransferRecipePacket::encode, TransferRecipePacket::decode, ServerPayloadHandler::handleTransferRecipePacket);
    }

    public static <T> void sendToServer(T message) {
        INSTANCE.sendToServer(message);
    }

    public static <T> void sendToPlayer(ServerPlayer player, T message) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <T> void sendToAllTracking(Entity entity, T message) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
    }

    public static <M> void registerMessage(Class<M> messageType, BiConsumer<M, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, M> decoder, BiConsumer<M, Supplier<NetworkEvent.Context>> messageConsumer) {
        INSTANCE.registerMessage(pkt++, messageType, encoder, decoder, messageConsumer);
    }
}
