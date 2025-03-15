package net.fxnt.fxntstorage.network;

import net.fxnt.fxntstorage.backpacks.main.BackpackItem;
import net.fxnt.fxntstorage.backpacks.main.BackpackItemMenu;
import net.fxnt.fxntstorage.backpacks.util.BackpackNetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncNBTDataPacket {
    private final ItemStack stack;

    public SyncNBTDataPacket(ItemStack stack) {
        this.stack = stack;
    }

    public static void encode(SyncNBTDataPacket packet, FriendlyByteBuf buf) {
        BackpackNetworkHelper.writeItemStack(packet.stack, buf);
    }

    public static SyncNBTDataPacket decode(FriendlyByteBuf buf) {
        ItemStack stack = BackpackNetworkHelper.readItemStack(buf);
        return new SyncNBTDataPacket(stack);
    }

    public static void handle(SyncNBTDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    ItemStack selectedItem = client.player.getMainHandItem();
                    if (selectedItem.getItem() instanceof BackpackItem) {
                        selectedItem.setTag(packet.stack.getTag());
                    }
                    if (client.player.containerMenu instanceof BackpackItemMenu backpackItemMenu) {
                        backpackItemMenu.setTag(packet.stack.getTag());
                    }
                }
            });
        });
        contextSupplier.get().setPacketHandled(true);
    }
}