package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.network.packet.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.Nullable;
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
        HANDLERS.put("handleJetpackFuelSyncPacket", (msg, ctx) -> JetpackFuelSyncPacket.handle((JetpackFuelSyncPacket) msg, ctx));
        HANDLERS.put("handleJukeboxClientPacket", (msg, ctx) -> JukeboxClientPacket.handle((JukeboxClientPacket) msg, ctx));
        HANDLERS.put("handleSetCarriedPacket", (msg, ctx) -> SetCarriedPacket.handle((SetCarriedPacket) msg, ctx));
        HANDLERS.put("handleSyncContainerPacket", (msg, ctx) -> SyncContainerPacket.handle((SyncContainerPacket) msg, ctx));
        HANDLERS.put("handleSyncSlotCountPacket", (msg, ctx) -> SyncSlotCountPacket.handle((SyncSlotCountPacket) msg, ctx));
        HANDLERS.put("handleVisualJetpackAirPacket", (msg, ctx) -> VisualJetpackAirPacket.handle((VisualJetpackAirPacket) msg, ctx));
        HANDLERS.put("handleSyncMountedStoragePacket", (msg, ctx) -> SyncMountedStoragePacket.handle((SyncMountedStoragePacket) msg, ctx));
        HANDLERS.put("handleSyncNBTDataPacket", (msg, ctx) -> SyncNBTDataPacket.handle((SyncNBTDataPacket) msg, ctx));
        HANDLERS.put("handleSyncStorageNetworkPacket", (msg, ctx) -> StorageNetworkSyncPacket.handle((StorageNetworkSyncPacket) msg, ctx));
        HANDLERS.put("handleStorageNetworkHighlightPacket", (msg, ctx) -> StorageNetworkHighlightPacket.handle((StorageNetworkHighlightPacket) msg, ctx));
        HANDLERS.put("handleVeinPreviewPacket", (msg, ctx) -> OreMiningPreviewPacket.handle((OreMiningPreviewPacket) msg, ctx));
    }

    public static void registerCommonPackets() {
        // ClientboundPacket
//        registerMessage(JetpackFuelSyncPacket.class, JetpackFuelSyncPacket::encode, JetpackFuelSyncPacket::decode, JetpackFuelSyncPacket::handle);
//        registerMessage(JukeboxClientPacket.class, JukeboxClientPacket::encode, JukeboxClientPacket::decode, JukeboxClientPacket::handle);
//        registerMessage(SetCarriedPacket.class, SetCarriedPacket::encode, SetCarriedPacket::decode, SetCarriedPacket::handle);
//        registerMessage(SyncContainerPacket.class, SyncContainerPacket::encode, SyncContainerPacket::decode, SyncContainerPacket::handle);
//        registerMessage(SyncSlotCountPacket.class, SyncSlotCountPacket::encode, SyncSlotCountPacket::decode, SyncSlotCountPacket::handle);
//        registerMessage(VisualJetpackAirPacket.class, VisualJetpackAirPacket::encode, VisualJetpackAirPacket::decode, VisualJetpackAirPacket::handle);
//        registerMessage(SyncMountedStoragePacket.class, SyncMountedStoragePacket::encode, SyncMountedStoragePacket::decode, SyncMountedStoragePacket::handle);
//        registerMessage(SyncNBTDataPacket.class, SyncNBTDataPacket::encode, SyncNBTDataPacket::decode, SyncNBTDataPacket::handle);
//        registerMessage(StorageNetworkSyncPacket.class, StorageNetworkSyncPacket::encode, StorageNetworkSyncPacket::decode, StorageNetworkSyncPacket::handle);
//        registerMessage(StorageNetworkHighlightPacket.class, StorageNetworkHighlightPacket::encode, StorageNetworkHighlightPacket::decode, StorageNetworkHighlightPacket::handle);


        registerClientboundMessage(JetpackFuelSyncPacket.class, JetpackFuelSyncPacket::encode, JetpackFuelSyncPacket::decode, "handleJetpackFuelSyncPacket");
        registerClientboundMessage(JukeboxClientPacket.class, JukeboxClientPacket::encode, JukeboxClientPacket::decode, "handleJukeboxClientPacket");
        registerClientboundMessage(SetCarriedPacket.class, SetCarriedPacket::encode, SetCarriedPacket::decode, "handleSetCarriedPacket");
        registerClientboundMessage(SyncContainerPacket.class, SyncContainerPacket::encode, SyncContainerPacket::decode, "handleSyncContainerPacket");
        registerClientboundMessage(SyncSlotCountPacket.class, SyncSlotCountPacket::encode, SyncSlotCountPacket::decode, "handleSyncSlotCountPacket");
        registerClientboundMessage(VisualJetpackAirPacket.class, VisualJetpackAirPacket::encode, VisualJetpackAirPacket::decode, "handleVisualJetpackAirPacket");
        registerClientboundMessage(SyncMountedStoragePacket.class, SyncMountedStoragePacket::encode, SyncMountedStoragePacket::decode, "handleSyncMountedStoragePacket");
        registerClientboundMessage(SyncNBTDataPacket.class, SyncNBTDataPacket::encode, SyncNBTDataPacket::decode, "handleSyncNBTDataPacket");
        registerClientboundMessage(StorageNetworkSyncPacket.class, StorageNetworkSyncPacket::encode, StorageNetworkSyncPacket::decode, "handleSyncStorageNetworkPacket");
        registerClientboundMessage(StorageNetworkHighlightPacket.class, StorageNetworkHighlightPacket::encode, StorageNetworkHighlightPacket::decode, "handleStorageNetworkHighlightPacket");
        registerClientboundMessage(OreMiningPreviewPacket.class, OreMiningPreviewPacket::encode, OreMiningPreviewPacket::decode, "handleVeinPreviewPacket");

        // ServerboundPacket
        registerMessage(CrossbowChargedPacket.class, CrossbowChargedPacket::encode, CrossbowChargedPacket::decode, CrossbowChargedPacket::handle);
        registerMessage(GhostItemPacket.class, GhostItemPacket::encode, GhostItemPacket::decode, GhostItemPacket::handle);
        registerMessage(JetpackFlyingPacket.class, JetpackFlyingPacket::encode, JetpackFlyingPacket::decode, JetpackFlyingPacket::handle);
        registerMessage(JukeboxServerPacket.class, JukeboxServerPacket::encode, JukeboxServerPacket::decode, JukeboxServerPacket::handle);
        registerMessage(KeyPressedPacket.class, KeyPressedPacket::encode, KeyPressedPacket::decode, KeyPressedPacket::handle);
        registerMessage(PickBlockUpgradePacket.class, PickBlockUpgradePacket::encode, PickBlockUpgradePacket::decode, PickBlockUpgradePacket::handle);
        registerMessage(PlayerInputPacket.class, PlayerInputPacket::encode, PlayerInputPacket::decode, PlayerInputPacket::handle);
        registerMessage(SetActivePanelPacket.class, SetActivePanelPacket::encode, SetActivePanelPacket::decode, SetActivePanelPacket::handle);
        registerMessage(SetMountedStorageDirtyPacket.class, SetMountedStorageDirtyPacket::encode, SetMountedStorageDirtyPacket::decode, SetMountedStorageDirtyPacket::handle);
        registerMessage(SetSortOrderPacket.class, SetSortOrderPacket::encode, SetSortOrderPacket::decode, SetSortOrderPacket::handle);
        registerMessage(SortInventoryPacket.class, SortInventoryPacket::encode, SortInventoryPacket::decode, SortInventoryPacket::handle);
        registerMessage(SyncClientSettingsPacket.class, SyncClientSettingsPacket::encode, SyncClientSettingsPacket::decode, SyncClientSettingsPacket::handle);
        registerMessage(TransferRecipePacket.class, TransferRecipePacket::encode, TransferRecipePacket::decode, TransferRecipePacket::handle);
        registerMessage(UpgradeDataPacket.class, UpgradeDataPacket::encode, UpgradeDataPacket::decode, UpgradeDataPacket::handle);
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

    public static <T> void sendToAllPlayers(T message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }

    public static <T> void sendToPlayersNear(ServerLevel level, @Nullable ServerPlayer excluded, double x, double y, double z, double radius, T message) {
        INSTANCE.send(
                PacketDistributor.NEAR.with(() ->
                        excluded == null
                                ? new PacketDistributor.TargetPoint(x, y, z, radius, level.dimension())
                                : new PacketDistributor.TargetPoint(excluded, x, y, z, radius, level.dimension())
                ), message
        );
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
