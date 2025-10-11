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

import java.util.HashMap;
import java.util.Map;
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

    private static final Map<String, BiConsumer<Object, Supplier<NetworkEvent.Context>>> HANDLERS = new HashMap<>();

    static {
        HANDLERS.put("handleJetpackFuelSyncPacket", (msg, ctx) -> ClientPayloadHandler.handleJetpackFuelSyncPacket((JetpackFuelSyncPacket) msg, ctx));
        HANDLERS.put("handleSetCarriedPacket", (msg, ctx) -> ClientPayloadHandler.handleSetCarriedPacket((SetCarriedPacket) msg, ctx));
        HANDLERS.put("handleSyncContainerPacket", (msg, ctx) -> ClientPayloadHandler.handleSyncContainerPacket((SyncContainerPacket) msg, ctx));
        HANDLERS.put("handleSyncSlotCountPacket", (msg, ctx) -> ClientPayloadHandler.handleSyncSlotCountPacket((SyncSlotCountPacket) msg, ctx));
        HANDLERS.put("handleVisualJetpackAirPacket", (msg, ctx) -> ClientPayloadHandler.handleVisualJetpackAirPacket((VisualJetpackAirPacket) msg, ctx));
        HANDLERS.put("handleSyncMountedStoragePacket", (msg, ctx) -> ClientPayloadHandler.handleSyncMountedStoragePacket((SyncMountedStoragePacket) msg, ctx));
        HANDLERS.put("handleSyncNBTDataPacket", (msg, ctx) -> ClientPayloadHandler.handleSyncNBTDataPacket((SyncNBTDataPacket) msg, ctx));
    }

    public static void registerCommonPackets() {
        // ClientboundPacket
        registerClientboundMessage(JetpackFuelSyncPacket.class, JetpackFuelSyncPacket::encode, JetpackFuelSyncPacket::decode, "handleJetpackFuelSyncPacket");
        registerClientboundMessage(SetCarriedPacket.class, SetCarriedPacket::encode, SetCarriedPacket::decode, "handleSetCarriedPacket");
        registerClientboundMessage(SyncContainerPacket.class, SyncContainerPacket::encode, SyncContainerPacket::decode, "handleSyncContainerPacket");
        registerClientboundMessage(SyncSlotCountPacket.class, SyncSlotCountPacket::encode, SyncSlotCountPacket::decode, "handleSyncSlotCountPacket");
        registerClientboundMessage(VisualJetpackAirPacket.class, VisualJetpackAirPacket::encode, VisualJetpackAirPacket::decode, "handleVisualJetpackAirPacket");
        registerClientboundMessage(SyncMountedStoragePacket.class, SyncMountedStoragePacket::encode, SyncMountedStoragePacket::decode, "handleSyncMountedStoragePacket");
        registerClientboundMessage(SyncNBTDataPacket.class, SyncNBTDataPacket::encode, SyncNBTDataPacket::decode, "handleSyncNBTDataPacket");

        // ServerboundPacket
        registerMessage(CrossbowChargedPacket.class, CrossbowChargedPacket::encode, CrossbowChargedPacket::decode, ServerPayloadHandler::handleCrossbowChargedPacket);
        registerMessage(JetpackFlyingPacket.class, JetpackFlyingPacket::encode, JetpackFlyingPacket::decode, ServerPayloadHandler::handleJetpackFlyingPacket);
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

    private static void invokeClientHandler(String name, Object message, Supplier<NetworkEvent.Context> ctx) {
        BiConsumer<Object, Supplier<NetworkEvent.Context>> handler = HANDLERS.get(name);
        if (handler != null) {
            handler.accept(message, ctx);
        } else {
            FXNTStorage.LOGGER.error("No handler for message type: {}", name);
        }
    }

    private static <M> void registerClientboundMessage(Class<M> messageType, BiConsumer<M, FriendlyByteBuf> encoder,
                                                       Function<FriendlyByteBuf, M> decoder, String handler) {
        INSTANCE.registerMessage(pkt++, messageType, encoder, decoder, (msg, ctx) -> {
            ctx.get().enqueueWork(() -> invokeClientHandler(handler, msg, ctx));
            ctx.get().setPacketHandled(true);
        });
    }

    private static <M> void registerMessage(Class<M> messageType, BiConsumer<M, FriendlyByteBuf> encoder,
                                            Function<FriendlyByteBuf, M> decoder, BiConsumer<M, Supplier<NetworkEvent.Context>> messageConsumer) {
        INSTANCE.registerMessage(pkt++, messageType, encoder, decoder, messageConsumer);
    }

}
