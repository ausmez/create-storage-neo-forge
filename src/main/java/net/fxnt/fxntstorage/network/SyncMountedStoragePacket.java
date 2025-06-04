package net.fxnt.fxntstorage.network;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.container.StorageBox;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SyncMountedStoragePacket {
    private final int contraptionId;
    private final BlockPos localPos;
    private final EnumProperties.StorageUsed fillLevel;
    private final CompoundTag nbt;

    public SyncMountedStoragePacket(int contraptionId, BlockPos localPos, EnumProperties.StorageUsed fillLevel, CompoundTag nbt) {
        this.contraptionId = contraptionId;
        this.localPos = localPos;
        this.fillLevel = fillLevel;
        this.nbt = nbt;
    }

    public static void encode(@NotNull SyncMountedStoragePacket packet, @NotNull FriendlyByteBuf buffer) {
        buffer.writeInt(packet.contraptionId);
        buffer.writeBlockPos(packet.localPos);
        buffer.writeEnum(packet.fillLevel);
        buffer.writeNbt(packet.nbt);
    }

    public static @NotNull SyncMountedStoragePacket decode(@NotNull FriendlyByteBuf buffer) {
        int contraptionId = buffer.readInt();
        BlockPos localPos = buffer.readBlockPos();
        EnumProperties.StorageUsed fillLevel = buffer.readEnum(EnumProperties.StorageUsed.class);
        CompoundTag nbt = buffer.readNbt();
        return new SyncMountedStoragePacket(contraptionId, localPos, fillLevel, nbt);
    }

    public static void handle(SyncMountedStoragePacket packet, @NotNull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                Minecraft client = Minecraft.getInstance();
                if (client.level == null) return;

                Entity entity = client.level.getEntity(packet.contraptionId);
                if (entity instanceof AbstractContraptionEntity contraptionEntity) {
                    StructureTemplate.StructureBlockInfo blockInfo = contraptionEntity.getContraption().getBlocks().get(packet.localPos);
                    CompoundTag newNbt = blockInfo.nbt();
                    CompoundTag nbt = packet.nbt;
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
                        newState = oldState.setValue(SimpleStorageBox.STORAGE_USED, packet.fillLevel);
                    } else { // StorageBox
                        newNbt.putFloat("PercentageUsed", nbt.getFloat("PercentageUsed"));
                        newState = oldState
                                .setValue(StorageBox.STORAGE_USED, packet.fillLevel)
                                .setValue(StorageBox.VOID_UPGRADE, nbt.getBoolean("VoidUpgrade"));
                    }

                    StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(blockInfo.pos(), newState, newNbt);
                    contraptionEntity.getContraption().getBlocks().put(packet.localPos, newInfo);
                    contraptionEntity.getContraption().deferInvalidate = true;
                }


//                Entity entity = client.player.level().getEntity(packet.contraptionId);
//                if (entity instanceof AbstractContraptionEntity contraptionEntity) {
//                    StructureTemplate.StructureBlockInfo blockInfo = contraptionEntity.getContraption().getBlocks().get(packet.localPos);
//                    CompoundTag newNbt = blockInfo.nbt();
//
//                    if (ItemStack.of(blockInfo.nbt().getCompound("FilterItem")).isEmpty()) {
//                        newNbt.put("FilterItem", packet.filter.save(new CompoundTag()));
//                    }
//
//                    int amount = packet.storedAmount;
//                    newNbt.putInt("StoredAmount", amount);
//
//                    BlockState newState = blockInfo.state().setValue(SimpleStorageBox.STORAGE_USED,
//                            amount == packet.maxCapacity
//                                    ? EnumProperties.StorageUsed.FULL
//                                    : EnumProperties.StorageUsed.HAS_ITEMS);
//
//                    StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(blockInfo.pos(), newState, newNbt);
//                    contraptionEntity.getContraption().getBlocks().put(packet.localPos, newInfo);
//                    contraptionEntity.getContraption().deferInvalidate = true;
//                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

}
