package net.fxnt.fxntstorage.network.packet;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.container.StorageBox;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.simple_storage.mounted.SimpleStorageBoxMountedMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncMountedStoragePacket(int contraptionId, BlockPos localPos, EnumProperties.StorageUsed fillLevel,
                                       CompoundTag nbt) {

    public static void encode(SyncMountedStoragePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.contraptionId);
        buffer.writeBlockPos(packet.localPos);
        buffer.writeEnum(packet.fillLevel);
        buffer.writeNbt(packet.nbt);
    }

    public static SyncMountedStoragePacket decode(FriendlyByteBuf buffer) {
        int contraptionId = buffer.readInt();
        BlockPos localPos = buffer.readBlockPos();
        EnumProperties.StorageUsed fillLevel = buffer.readEnum(EnumProperties.StorageUsed.class);
        CompoundTag nbt = buffer.readNbt();
        return new SyncMountedStoragePacket(contraptionId, localPos, fillLevel, nbt);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(SyncMountedStoragePacket packet, Supplier<NetworkEvent.Context> context) {
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
                    if (client.player.containerMenu instanceof SimpleStorageBoxMountedMenu menu) {
                        if (menu.getLocalPos().equals(packet.localPos()))
                            menu.setFilterItem(ItemStack.of(newNbt.getCompound("FilterItem")));
                    }

                    StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(blockInfo.pos(), newState, newNbt);
                    contraptionEntity.getContraption().getBlocks().put(packet.localPos(), newInfo);
                    contraptionEntity.getContraption().resetClientContraption();
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
