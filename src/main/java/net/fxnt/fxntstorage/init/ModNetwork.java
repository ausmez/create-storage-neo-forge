package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.network.ServerboundPacket;
import net.fxnt.fxntstorage.network.SyncNBTDataPacket;
import net.fxnt.fxntstorage.network.backpack.client.ClientboundSetCarriedPacket;
import net.fxnt.fxntstorage.network.backpack.client.SyncContainerPacket;
import net.fxnt.fxntstorage.network.backpack.client.SyncSlotCountPacket;
import net.fxnt.fxntstorage.network.backpack.client.VisualJetpackAirPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
            new ResourceLocation(FXNTStorage.MOD_ID, "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int pkt = 0;

    public static void registerCommonPackets() {
        registerMessage(ClientboundSetCarriedPacket.class, ClientboundSetCarriedPacket::encode, ClientboundSetCarriedPacket::decode, ClientboundSetCarriedPacket::handle);
        registerMessage(ServerboundPacket.class, ServerboundPacket::encoder, ServerboundPacket::decoder, ServerboundPacket::handler);
        registerMessage(SyncContainerPacket.class, SyncContainerPacket::encode, SyncContainerPacket::decode, SyncContainerPacket::handle);
        registerMessage(SyncSlotCountPacket.class, SyncSlotCountPacket::encode, SyncSlotCountPacket::decode, SyncSlotCountPacket::handle);
        registerMessage(VisualJetpackAirPacket.class, VisualJetpackAirPacket::encode, VisualJetpackAirPacket::decode, VisualJetpackAirPacket::handle);
        registerMessage(SyncNBTDataPacket.class, SyncNBTDataPacket::encode, SyncNBTDataPacket::decode, SyncNBTDataPacket::handle);
    }

    public static void registerClientPackets() {
        // NOOP
    }

    /**
     * Sends a packet to the Minecraft server.
     *
     * @param message The packet to send (must implement IMessage).
     * @param <T>     The packet type.
     */
    public static <T> void sendToServer(T message) {
        INSTANCE.sendToServer(message);
    }

    /**
     * Sends a packet to the specified player.
     *
     * @param player  The player to send the packet to.
     * @param message The packet to send (must implement IMessage).
     * @param <T>     The packet type.
     */
    public static <T> void sendToPlayer(ServerPlayer player, T message) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <M> void registerMessage(Class<M> messageType, BiConsumer<M, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, M> decoder, BiConsumer<M, Supplier<NetworkEvent.Context>> messageConsumer) {
        INSTANCE.registerMessage(pkt++, messageType, encoder, decoder, messageConsumer);
    }
}
