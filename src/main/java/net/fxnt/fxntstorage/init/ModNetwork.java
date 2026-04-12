package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.network.packet.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.Nullable;
import java.util.Optional;
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
        // Clientbound packets
        registerClientboundMessage(JetpackFuelSyncPacket.class, JetpackFuelSyncPacket::encode, JetpackFuelSyncPacket::decode, () -> JetpackFuelSyncPacket::handle);
        registerClientboundMessage(JetpackStateResetPacket.class, JetpackStateResetPacket::encode, JetpackStateResetPacket::decode, () -> JetpackStateResetPacket::handle);
        registerClientboundMessage(JukeboxClientPacket.class, JukeboxClientPacket::encode, JukeboxClientPacket::decode, () -> JukeboxClientPacket::handle);
        registerClientboundMessage(SetCarriedPacket.class, SetCarriedPacket::encode, SetCarriedPacket::decode, () -> SetCarriedPacket::handle);
        registerClientboundMessage(SyncContainerPacket.class, SyncContainerPacket::encode, SyncContainerPacket::decode, () -> SyncContainerPacket::handle);
        registerClientboundMessage(SyncSlotCountPacket.class, SyncSlotCountPacket::encode, SyncSlotCountPacket::decode, () -> SyncSlotCountPacket::handle);
        registerClientboundMessage(VisualJetpackAirPacket.class, VisualJetpackAirPacket::encode, VisualJetpackAirPacket::decode, () -> VisualJetpackAirPacket::handle);
        registerClientboundMessage(SyncMountedStoragePacket.class, SyncMountedStoragePacket::encode, SyncMountedStoragePacket::decode, () -> SyncMountedStoragePacket::handle);
        registerClientboundMessage(SyncNBTDataPacket.class, SyncNBTDataPacket::encode, SyncNBTDataPacket::decode, () -> SyncNBTDataPacket::handle);
        registerClientboundMessage(StorageNetworkSyncPacket.class, StorageNetworkSyncPacket::encode, StorageNetworkSyncPacket::decode, () -> StorageNetworkSyncPacket::handle);
        registerClientboundMessage(StorageNetworkHighlightPacket.class, StorageNetworkHighlightPacket::encode, StorageNetworkHighlightPacket::decode, () -> StorageNetworkHighlightPacket::handle);
        registerClientboundMessage(OreMiningPreviewPacket.class, OreMiningPreviewPacket::encode, OreMiningPreviewPacket::decode, () -> OreMiningPreviewPacket::handle);

        // Serverbound packets
        registerServerboundMessage(CrossbowChargedPacket.class, CrossbowChargedPacket::encode, CrossbowChargedPacket::decode, CrossbowChargedPacket::handle);
        registerServerboundMessage(GhostItemPacket.class, GhostItemPacket::encode, GhostItemPacket::decode, GhostItemPacket::handle);
        registerServerboundMessage(JetpackFlyingPacket.class, JetpackFlyingPacket::encode, JetpackFlyingPacket::decode, JetpackFlyingPacket::handle);
        registerServerboundMessage(JukeboxServerPacket.class, JukeboxServerPacket::encode, JukeboxServerPacket::decode, JukeboxServerPacket::handle);
        registerServerboundMessage(KeyPressedPacket.class, KeyPressedPacket::encode, KeyPressedPacket::decode, KeyPressedPacket::handle);
        registerServerboundMessage(PickBlockUpgradePacket.class, PickBlockUpgradePacket::encode, PickBlockUpgradePacket::decode, PickBlockUpgradePacket::handle);
        registerServerboundMessage(PlayerInputPacket.class, PlayerInputPacket::encode, PlayerInputPacket::decode, PlayerInputPacket::handle);
        registerServerboundMessage(SetActivePanelPacket.class, SetActivePanelPacket::encode, SetActivePanelPacket::decode, SetActivePanelPacket::handle);
        registerServerboundMessage(SetMountedStorageDirtyPacket.class, SetMountedStorageDirtyPacket::encode, SetMountedStorageDirtyPacket::decode, SetMountedStorageDirtyPacket::handle);
        registerServerboundMessage(SetSortOrderPacket.class, SetSortOrderPacket::encode, SetSortOrderPacket::decode, SetSortOrderPacket::handle);
        registerServerboundMessage(SortInventoryPacket.class, SortInventoryPacket::encode, SortInventoryPacket::decode, SortInventoryPacket::handle);
        registerServerboundMessage(SyncClientSettingsPacket.class, SyncClientSettingsPacket::encode, SyncClientSettingsPacket::decode, SyncClientSettingsPacket::handle);
        registerServerboundMessage(TransferRecipePacket.class, TransferRecipePacket::encode, TransferRecipePacket::decode, TransferRecipePacket::handle);
        registerServerboundMessage(UpgradeDataPacket.class, UpgradeDataPacket::encode, UpgradeDataPacket::decode, UpgradeDataPacket::handle);
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

    private static <M> void registerClientboundMessage(Class<M> messageType, BiConsumer<M, FriendlyByteBuf> encoder,
                                                       Function<FriendlyByteBuf, M> decoder, Supplier<BiConsumer<M, Supplier<NetworkEvent.Context>>> clientHandlerSupplier) {
        INSTANCE.registerMessage(pkt++, messageType, encoder, decoder,
                (msg, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> clientHandlerSupplier.get().accept(msg, ctx)),
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static <M> void registerServerboundMessage(Class<M> messageType, BiConsumer<M, FriendlyByteBuf> encoder,
                                                       Function<FriendlyByteBuf, M> decoder, BiConsumer<M, Supplier<NetworkEvent.Context>> handler) {
        INSTANCE.registerMessage(pkt++, messageType, encoder, decoder, handler, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
