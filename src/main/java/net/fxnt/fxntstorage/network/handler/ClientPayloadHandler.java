package net.fxnt.fxntstorage.network.handler;

import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.main.BackpackMenu;
import net.fxnt.fxntstorage.network.packet.*;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ClientPayloadHandler {
    private static final ClientPayloadHandler INSTANCE = new ClientPayloadHandler();

    public static ClientPayloadHandler getInstance() {
        return INSTANCE;
    }

    public void handleSetCarriedPacket(final SetCarriedPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    client.player.containerMenu.setCarried(packet.stack());
                }
            });
        });
    }

    public void handleSyncNBTDataPacket(final SyncDataComponentPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    ItemStack selectedItem = client.player.getMainHandItem();
                    if (selectedItem.getItem() instanceof BackpackItem) {
                        selectedItem.applyComponents(packet.component());
                    }
                }
            });
        });
    }

    public void handleSyncContainerPacket(final SyncContainerPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.player != null && client.player.containerMenu instanceof BackpackMenu && client.player.containerMenu.containerId == packet.containerId()) {
                    IItemHandlerModifiable itemHandler = ((BackpackMenu) client.player.containerMenu).container.getItemHandler();
                    for (int i = 0; i < itemHandler.getSlots(); i++) {
                        itemHandler.setStackInSlot(i, packet.items().get(i));
                    }
                }

            });
        });
    }

    public void handleSyncSlotCountPacket(final SyncSlotCountPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.player != null && client.player.containerMenu instanceof BackpackMenu && client.player.containerMenu.containerId == packet.containerId()) {
                    IItemHandlerModifiable itemHandler = ((BackpackMenu) client.player.containerMenu).container.getItemHandler();
                    itemHandler.setStackInSlot(packet.slot(), packet.stack());
                }
            });
        });
    }

    public void handleVisualJetpackAirPacket(final VisualJetpackAirPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    if (packet.airRemaining() < 0) {
                        client.player.getPersistentData().remove("VisualJetpackAir");
                    } else {
                        client.player.getPersistentData().putInt("VisualJetpackAir", packet.airRemaining());
                    }
                }
            });
        });
    }

}
