package net.fxnt.fxntstorage.network.handler;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.main.BackpackMenu;
import net.fxnt.fxntstorage.backpack.upgrade.JetpackHandler;
import net.fxnt.fxntstorage.backpack.upgrade.JetpackManager;
import net.fxnt.fxntstorage.backpack.util.BackpackClientHelper;
import net.fxnt.fxntstorage.container.StorageBox;
import net.fxnt.fxntstorage.network.packet.*;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.simple_storage.mounted.SimpleStorageBoxMountedMenu;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
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
            context.player();
            context.player().containerMenu.setCarried(packet.stack());
        });
    }

    public void handleSyncNBTDataPacket(final SyncDataComponentPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            context.player();
            ItemStack selectedItem = context.player().getMainHandItem();
            if (selectedItem.getItem() instanceof BackpackItem) {
                selectedItem.applyComponents(packet.component());
            }
        });
    }

    public void handleSyncContainerPacket(final SyncContainerPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            context.player();
            if (context.player().containerMenu instanceof BackpackMenu && context.player().containerMenu.containerId == packet.containerId()) {
                IItemHandlerModifiable itemHandler = ((BackpackMenu) context.player().containerMenu).container.getItemHandler();
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    itemHandler.setStackInSlot(i, packet.items().get(i));
                }
            }

        });
    }

    public void handleSyncSlotCountPacket(final SyncSlotCountPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            context.player();
            if (context.player().containerMenu instanceof BackpackMenu && context.player().containerMenu.containerId == packet.containerId()) {
                IItemHandlerModifiable itemHandler = ((BackpackMenu) context.player().containerMenu).container.getItemHandler();
                itemHandler.setStackInSlot(packet.slot(), packet.stack());
            }
        });
    }

    public void handleVisualJetpackAirPacket(final VisualJetpackAirPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            context.player();
            if (packet.airRemaining() < 0) {
                context.player().getPersistentData().remove("VisualJetpackAir");
            } else {
                context.player().getPersistentData().putInt("VisualJetpackAir", packet.airRemaining());
            }
        });
    }

    public void handleSyncMountedStoragePacket(final SyncMountedStoragePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            context.player();
            var level = context.player().level();
            Entity entity = level.getEntity(packet.contraptionId());
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
                    if (nbt.contains("FilterItem", CompoundTag.TAG_COMPOUND)) {
                        newNbt.put("FilterItem", nbt.getCompound("FilterItem"));
                    }
                    newState = oldState.setValue(SimpleStorageBox.STORAGE_USED, packet.fillLevel());
                } else { // StorageBox
                    newNbt.putFloat("PercentageUsed", nbt.getFloat("PercentageUsed"));
                    newState = oldState
                            .setValue(StorageBox.STORAGE_USED, packet.fillLevel())
                            .setValue(StorageBox.VOID_UPGRADE, nbt.getBoolean("VoidUpgrade"));
                }

                // Update FilterItem icon if player has menu open
                if (context.player().containerMenu instanceof SimpleStorageBoxMountedMenu menu) {
                    if (menu.getLocalPos().equals(packet.localPos()))
                        menu.setFilterItem(ItemStack.parse(context.player().registryAccess(), newNbt.getCompound("FilterItem")).orElse(ItemStack.EMPTY));
                }

                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(blockInfo.pos(), newState, newNbt);
                contraptionEntity.getContraption().getBlocks().put(packet.localPos(), newInfo);
                contraptionEntity.getContraption().resetClientContraption();
            }
        });
    }

    public void handleSyncItemStackPacket(final SyncItemStackPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            context.player();
            ItemStack backpack = BackpackClientHelper.getEquippedBackpackStack((LocalPlayer) context.player());
            backpack.applyComponents(packet.dataMap());
        });
    }

    public void handleJetpackFuelSyncPacket(final JetpackFuelSyncPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            context.player();
            JetpackHandler handler = JetpackManager.getJetpackHandler(context.player());
            if (handler != null) {
                handler.onFuelSync(packet.fuelRemaining(), packet.serverTime());
            }
        });
    }

}
