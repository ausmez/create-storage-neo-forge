package net.fxnt.fxntstorage.network.packet;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record SyncContainerPacket(int containerId, int stateId, List<ItemStack> items,
                                  ItemStack carriedItem) implements CustomPacketPayload {
    public static final Type<SyncContainerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sync_container"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncContainerPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncContainerPacket::containerId,
            ByteBufCodecs.INT, SyncContainerPacket::stateId,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, SyncContainerPacket::items,
            ItemStack.OPTIONAL_STREAM_CODEC, SyncContainerPacket::carriedItem,
            SyncContainerPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof Player player && player.containerMenu instanceof BackpackMenu menu && menu.containerId == containerId()) {
                IItemHandlerModifiable itemHandler = menu.container.getItemHandler();
                if (items().size() == itemHandler.getSlots()) {
                    for (int i = 0; i < itemHandler.getSlots(); i++) {
                        itemHandler.setStackInSlot(i, items().get(i));
                    }
                }
                menu.setCarried(carriedItem());
                menu.setStateId(stateId());
            }
        });
    }
}
