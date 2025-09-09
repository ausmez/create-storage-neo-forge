package net.fxnt.fxntstorage.network;

import net.fxnt.fxntstorage.network.handler.ClientPayloadHandler;
import net.fxnt.fxntstorage.network.handler.ServerPayloadHandler;
import net.fxnt.fxntstorage.network.packet.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketHandler {

    @SubscribeEvent
    public void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // ClientboundPacket
        registrar.playToClient(JetpackFuelSyncPacket.TYPE, JetpackFuelSyncPacket.STREAM_CODEC, ClientPayloadHandler.getInstance()::handleJetpackFuelSyncPacket);
        registrar.playToClient(SetCarriedPacket.TYPE, SetCarriedPacket.STREAM_CODEC, ClientPayloadHandler.getInstance()::handleSetCarriedPacket);
        registrar.playToClient(SyncContainerPacket.TYPE, SyncContainerPacket.STREAM_CODEC, ClientPayloadHandler.getInstance()::handleSyncContainerPacket);
        registrar.playToClient(SyncDataComponentPacket.TYPE, SyncDataComponentPacket.STREAM_CODEC, ClientPayloadHandler.getInstance()::handleSyncNBTDataPacket);
        registrar.playToClient(SyncItemStackPacket.TYPE, SyncItemStackPacket.STREAM_CODEC, ClientPayloadHandler.getInstance()::handleSyncItemStackPacket);
        registrar.playToClient(SyncMountedStoragePacket.TYPE, SyncMountedStoragePacket.STREAM_CODEC, ClientPayloadHandler.getInstance()::handleSyncMountedStoragePacket);
        registrar.playToClient(SyncSlotCountPacket.TYPE, SyncSlotCountPacket.STREAM_CODEC, ClientPayloadHandler.getInstance()::handleSyncSlotCountPacket);
        registrar.playToClient(VisualJetpackAirPacket.TYPE, VisualJetpackAirPacket.STREAM_CODEC, ClientPayloadHandler.getInstance()::handleVisualJetpackAirPacket);

        // ServerboundPacket
        registrar.playToServer(CrossbowChargedPacket.TYPE, CrossbowChargedPacket.STREAM_CODEC, ServerPayloadHandler.getInstance()::handleCrossbowChargedPacket);
        registrar.playToServer(JetpackFlyingPacket.TYPE, JetpackFlyingPacket.STREAM_CODEC, ServerPayloadHandler.getInstance()::handleJetpackFlyingPacket);
        registrar.playToServer(KeyPressedPacket.TYPE, KeyPressedPacket.STREAM_CODEC, ServerPayloadHandler.getInstance()::handleKeyPressedPacket);
        registrar.playToServer(PickBlockUpgradePacket.TYPE, PickBlockUpgradePacket.STREAM_CODEC, ServerPayloadHandler.getInstance()::handlePickBlockUpgradePacket);
        registrar.playToServer(PlayerInputPacket.TYPE, PlayerInputPacket.STREAM_CODEC, ServerPayloadHandler.getInstance()::handlePlayerInputPacket);
        registrar.playToServer(SetSortOrderPacket.TYPE, SetSortOrderPacket.STREAM_CODEC, ServerPayloadHandler.getInstance()::handleSetSortOrderPacket);
        registrar.playToServer(SetMountedStorageDirtyPacket.TYPE, SetMountedStorageDirtyPacket.STREAM_CODEC, ServerPayloadHandler.getInstance()::handleSetMountedStorageDirtyPacket);
        registrar.playToServer(SortInventoryPacket.TYPE, SortInventoryPacket.STREAM_CODEC, ServerPayloadHandler.getInstance()::handleSortInventoryPacket);
        registrar.playToServer(SyncClientSettingsPacket.TYPE, SyncClientSettingsPacket.STREAM_CODEC, ServerPayloadHandler.getInstance()::handleSyncClientSettingsPacket);
        registrar.playToServer(TransferRecipePacket.TYPE, TransferRecipePacket.STREAM_CODEC, ServerPayloadHandler.getInstance()::handleTransferRecipePacket);
    }

}
