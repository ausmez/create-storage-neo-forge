package net.fxnt.fxntstorage.network;

import net.fxnt.fxntstorage.network.packet.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketHandler {

    @SubscribeEvent
    public void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // ClientboundPacket
        registrar.playToClient(JetpackFuelSyncPacket.TYPE, JetpackFuelSyncPacket.STREAM_CODEC, JetpackFuelSyncPacket::handle);
        registrar.playToClient(JukeboxClientPacket.TYPE, JukeboxClientPacket.STREAM_CODEC, JukeboxClientPacket::handle);
        registrar.playToClient(OreMiningPreviewPacket.TYPE, OreMiningPreviewPacket.STREAM_CODEC, OreMiningPreviewPacket::handle);
        registrar.playToClient(SetCarriedPacket.TYPE, SetCarriedPacket.STREAM_CODEC, SetCarriedPacket::handle);
        registrar.playToClient(StorageNetworkHighlightPacket.TYPE, StorageNetworkHighlightPacket.STREAM_CODEC, StorageNetworkHighlightPacket::handle);
        registrar.playToClient(StorageNetworkSyncPacket.TYPE, StorageNetworkSyncPacket.STREAM_CODEC, StorageNetworkSyncPacket::handle);
        registrar.playToClient(SyncContainerPacket.TYPE, SyncContainerPacket.STREAM_CODEC, SyncContainerPacket::handle);
        registrar.playToClient(SyncDataComponentPacket.TYPE, SyncDataComponentPacket.STREAM_CODEC, SyncDataComponentPacket::handle);
        registrar.playToClient(SyncItemStackPacket.TYPE, SyncItemStackPacket.STREAM_CODEC, SyncItemStackPacket::handle);
        registrar.playToClient(SyncMountedStoragePacket.TYPE, SyncMountedStoragePacket.STREAM_CODEC, SyncMountedStoragePacket::handle);
        registrar.playToClient(SyncSlotCountPacket.TYPE, SyncSlotCountPacket.STREAM_CODEC, SyncSlotCountPacket::handle);
        registrar.playToClient(VisualJetpackAirPacket.TYPE, VisualJetpackAirPacket.STREAM_CODEC, VisualJetpackAirPacket::handle);

        // ServerboundPacket
        registrar.playToServer(CrossbowChargedPacket.TYPE, CrossbowChargedPacket.STREAM_CODEC, CrossbowChargedPacket::handle);
        registrar.playToServer(GhostItemPacket.TYPE, GhostItemPacket.STREAM_CODEC, GhostItemPacket::handle);
        registrar.playToServer(JetpackFlyingPacket.TYPE, JetpackFlyingPacket.STREAM_CODEC, JetpackFlyingPacket::handle);
        registrar.playToServer(JukeboxServerPacket.TYPE, JukeboxServerPacket.STREAM_CODEC, JukeboxServerPacket::handle);
        registrar.playToServer(KeyPressedPacket.TYPE, KeyPressedPacket.STREAM_CODEC, KeyPressedPacket::handle);
        registrar.playToServer(PickBlockUpgradePacket.TYPE, PickBlockUpgradePacket.STREAM_CODEC, PickBlockUpgradePacket::handle);
        registrar.playToServer(PlayerInputPacket.TYPE, PlayerInputPacket.STREAM_CODEC, PlayerInputPacket::handle);
        registrar.playToServer(SetActivePanelPacket.TYPE, SetActivePanelPacket.STREAM_CODEC, SetActivePanelPacket::handle);
        registrar.playToServer(SetSortOrderPacket.TYPE, SetSortOrderPacket.STREAM_CODEC, SetSortOrderPacket::handle);
        registrar.playToServer(SetMountedStorageDirtyPacket.TYPE, SetMountedStorageDirtyPacket.STREAM_CODEC, SetMountedStorageDirtyPacket::handle);
        registrar.playToServer(SortInventoryPacket.TYPE, SortInventoryPacket.STREAM_CODEC, SortInventoryPacket::handle);
        registrar.playToServer(SyncClientSettingsPacket.TYPE, SyncClientSettingsPacket.STREAM_CODEC, SyncClientSettingsPacket::handle);
        registrar.playToServer(TransferRecipePacket.TYPE, TransferRecipePacket.STREAM_CODEC, TransferRecipePacket::handle);
        registrar.playToServer(UpgradeDataPacket.TYPE, UpgradeDataPacket.STREAM_CODEC, UpgradeDataPacket::handle);
    }
}
