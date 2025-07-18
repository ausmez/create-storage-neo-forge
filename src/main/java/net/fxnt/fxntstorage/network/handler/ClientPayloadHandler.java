package net.fxnt.fxntstorage.network.handler;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.main.BackpackItemMenu;
import net.fxnt.fxntstorage.backpack.main.BackpackMenu;
import net.fxnt.fxntstorage.container.StorageBox;
import net.fxnt.fxntstorage.network.packet.*;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ClientPayloadHandler {
    private static final ClientPayloadHandler INSTANCE = new ClientPayloadHandler();

    public static ClientPayloadHandler getInstance() {
        return INSTANCE;
    }

    public static void handleSetCarriedPacket(SetCarriedPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    client.player.containerMenu.setCarried(packet.stack());
                }
            });
        });
        context.get().setPacketHandled(true);
    }

    public static void handleSyncNBTDataPacket(SyncNBTDataPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    ItemStack selectedItem = client.player.getMainHandItem();
                    if (selectedItem.getItem() instanceof BackpackItem) {
                        selectedItem.setTag(packet.stack().getTag());
                    }
                    if (client.player.containerMenu instanceof BackpackItemMenu backpackItemMenu) {
                        backpackItemMenu.setTag(packet.stack().getTag());
                    }
                }
            });
        });
        context.get().setPacketHandled(true);
    }

    public static void handleSyncContainerPacket(SyncContainerPacket packet, @NotNull Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection().getReceptionSide().isClient()) {
                Minecraft client = Minecraft.getInstance();
                if (client.player != null && client.player.containerMenu instanceof BackpackMenu && client.player.containerMenu.containerId == packet.containerId()) {
                    ItemStackHandler itemHandler = ((BackpackMenu) client.player.containerMenu).container.getItemHandler();
                    for (int i = 0; i < packet.items().size(); i++) {
                        itemHandler.setStackInSlot(i, packet.items().get(i));
                    }
                }
            }
        });
        context.get().setPacketHandled(true);
    }

    public static void handleSyncSlotCountPacket(SyncSlotCountPacket packet, @NotNull Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection().getReceptionSide().isClient()) {
                Minecraft client = Minecraft.getInstance();
                if (client.player != null && client.player.containerMenu instanceof BackpackMenu && client.player.containerMenu.containerId == packet.containerId()) {
                    ItemStackHandler itemHandler = ((BackpackMenu) client.player.containerMenu).container.getItemHandler();
                    itemHandler.setStackInSlot(packet.slot(), packet.stack());
                }

            }
        });
        context.get().setPacketHandled(true);
    }

    public static void handleVisualJetpackAirPacket(VisualJetpackAirPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
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
        context.get().setPacketHandled(true);
    }

    public static void handleSyncMountedStoragePacket(SyncMountedStoragePacket packet, @NotNull Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection().getReceptionSide().isClient()) {
                Minecraft client = Minecraft.getInstance();
                if (client.level == null) return;

                Entity entity = client.level.getEntity(packet.contraptionId());
                if (entity instanceof AbstractContraptionEntity contraptionEntity) {
                    StructureTemplate.StructureBlockInfo blockInfo = contraptionEntity.getContraption().getBlocks().get(packet.localPos());
                    CompoundTag newNbt = blockInfo.nbt();
                    CompoundTag nbt = packet.nbt();
                    BlockState oldState = blockInfo.state();
                    BlockState newState;

                    newNbt.putInt("StoredAmount", nbt.getInt("StoredAmount"));
                    newNbt.putBoolean("VoidUpgrade", nbt.getBoolean("VoidUpgrade"));
                    newNbt.putInt("MaxItemCapacity", nbt.getInt("MaxItemCapacity"));

                    if (oldState.getBlock() instanceof SimpleStorageBox) { // SimpleStorageBox
                        CompoundTag tag = blockInfo.nbt().getCompound("FilterItem");
                        if ("minecraft:air".equals(tag.getString("id"))) {
                            newNbt.put("FilterItem", nbt.getCompound("FilterItem"));
                        }
                        newState = oldState.setValue(SimpleStorageBox.STORAGE_USED, packet.fillLevel());
                    } else { // StorageBox
                        newNbt.putFloat("PercentageUsed", nbt.getFloat("PercentageUsed"));
                        newState = oldState
                                .setValue(StorageBox.STORAGE_USED, packet.fillLevel())
                                .setValue(StorageBox.VOID_UPGRADE, nbt.getBoolean("VoidUpgrade"));
                    }

                    StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(blockInfo.pos(), newState, newNbt);
                    contraptionEntity.getContraption().getBlocks().put(packet.localPos(), newInfo);
                    contraptionEntity.getContraption().deferInvalidate = true;
                }
            }
        });
        context.get().setPacketHandled(true);
    }

}
