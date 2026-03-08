package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncNBTDataPacket(ItemStack stack) {

    public static void encode(SyncNBTDataPacket packet, FriendlyByteBuf buf) {
        BackpackHelper.writeItemStack(packet.stack, buf);
    }

    public static SyncNBTDataPacket decode(FriendlyByteBuf buf) {
        ItemStack stack = BackpackHelper.readItemStack(buf);
        return new SyncNBTDataPacket(stack);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(SyncNBTDataPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection().getReceptionSide().isClient()) {
                Minecraft client = Minecraft.getInstance();
                client.execute(() -> {
                    if (client.player != null) {
                        if (client.player.containerMenu instanceof BackpackMenu menu) {
                            switch (menu.type) {
                                case ITEM -> {
                                    ItemStack selectedItem = client.player.getMainHandItem();
                                    if (selectedItem.getItem() instanceof BackpackItem) {
                                        selectedItem.setTag(packet.stack().getTag());
                                    }
                                }
                                case WORN -> {
                                    ItemStack wornItem = BackpackHelper.getEquippedBackpackStack(client.player);
                                    if (wornItem.getItem() instanceof BackpackItem) {
                                        wornItem.setTag(packet.stack().getTag());
                                    }
                                }
                                default -> {}
                            }
                            menu.setTag(packet.stack().getTag());
                        }
                    }
                });
            }
        });
        context.get().setPacketHandled(true);
    }
}
