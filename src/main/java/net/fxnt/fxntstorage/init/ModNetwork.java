package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.network.*;
import net.fxnt.fxntstorage.network.backpack.client.ClientboundSetCarriedPacket;
import net.fxnt.fxntstorage.network.backpack.client.SyncContainerPacket;
import net.fxnt.fxntstorage.network.backpack.client.SyncSlotCountPacket;
import net.fxnt.fxntstorage.network.backpack.client.VisualJetpackAirPacket;
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
        registerMessage(ClientboundSetCarriedPacket.class, ClientboundSetCarriedPacket::encode, ClientboundSetCarriedPacket::decode, ClientboundSetCarriedPacket::handle);
        registerMessage(ServerboundPacket.class, ServerboundPacket::encoder, ServerboundPacket::decoder, ServerboundPacket::handler);
        registerMessage(SetMountedStorageDirtyPacket.class, SetMountedStorageDirtyPacket::encode, SetMountedStorageDirtyPacket::decode, SetMountedStorageDirtyPacket::handle);
        registerMessage(SetSortOrderPacket.class, SetSortOrderPacket::encode, SetSortOrderPacket::decode, SetSortOrderPacket::handle);
        registerMessage(SyncContainerPacket.class, SyncContainerPacket::encode, SyncContainerPacket::decode, SyncContainerPacket::handle);
        registerMessage(SyncMountedStoragePacket.class, SyncMountedStoragePacket::encode, SyncMountedStoragePacket::decode, SyncMountedStoragePacket::handle);
        registerMessage(SyncSlotCountPacket.class, SyncSlotCountPacket::encode, SyncSlotCountPacket::decode, SyncSlotCountPacket::handle);
        registerMessage(VisualJetpackAirPacket.class, VisualJetpackAirPacket::encode, VisualJetpackAirPacket::decode, VisualJetpackAirPacket::handle);
        registerMessage(SyncNBTDataPacket.class, SyncNBTDataPacket::encode, SyncNBTDataPacket::decode, SyncNBTDataPacket::handle);
    }

    public static void registerClientPackets() {
        // NOOP
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
