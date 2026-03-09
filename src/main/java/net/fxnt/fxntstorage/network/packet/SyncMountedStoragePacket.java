package net.fxnt.fxntstorage.network.packet;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.container.StorageBox;
import net.fxnt.fxntstorage.container.util.EnumProperties;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.simple_storage.mounted.SimpleStorageBoxMountedMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncMountedStoragePacket(int contraptionId, BlockPos localPos, EnumProperties.StorageUsed fillLevel,
                                       CompoundTag nbt) implements CustomPacketPayload {
    public static final Type<SyncMountedStoragePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "sync_mounted_storage"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMountedStoragePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncMountedStoragePacket::contraptionId,
            BlockPos.STREAM_CODEC, SyncMountedStoragePacket::localPos,
            NeoForgeStreamCodecs.enumCodec(EnumProperties.StorageUsed.class), SyncMountedStoragePacket::fillLevel,
            ByteBufCodecs.COMPOUND_TAG, SyncMountedStoragePacket::nbt,
            SyncMountedStoragePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof Player player) {
                var level = player.level();
                Entity entity = level.getEntity(contraptionId());
                if (entity instanceof AbstractContraptionEntity contraptionEntity) {
                    StructureTemplate.StructureBlockInfo blockInfo = contraptionEntity.getContraption().getBlocks().get(localPos());
                    CompoundTag newNbt = blockInfo.nbt();
                    CompoundTag nbt = nbt();
                    BlockState oldState = blockInfo.state();
                    BlockState newState;

                    newNbt.putInt("StoredAmount", nbt.getInt("StoredAmount"));
                    newNbt.putBoolean("VoidUpgrade", nbt.getBoolean("VoidUpgrade"));
                    newNbt.putInt("MaxItemCapacity", nbt.getInt("MaxItemCapacity"));

                    if (oldState.getBlock() instanceof SimpleStorageBox) { // SimpleStorageBox
                        if (nbt.contains("FilterItem", CompoundTag.TAG_COMPOUND)) {
                            newNbt.put("FilterItem", nbt.getCompound("FilterItem"));
                        }
                        newState = oldState.setValue(SimpleStorageBox.STORAGE_USED, fillLevel());
                    } else { // StorageBox
                        newNbt.putFloat("PercentageUsed", nbt.getFloat("PercentageUsed"));
                        newState = oldState
                                .setValue(StorageBox.STORAGE_USED, fillLevel())
                                .setValue(StorageBox.VOID_UPGRADE, nbt.getBoolean("VoidUpgrade"));
                    }

                    // Update FilterItem icon if player has menu open
                    if (player.containerMenu instanceof SimpleStorageBoxMountedMenu menu) {
                        if (menu.getLocalPos().equals(localPos()))
                            menu.setFilterItem(ItemStack.parse(player.registryAccess(), newNbt.getCompound("FilterItem")).orElse(ItemStack.EMPTY));
                    }

                    StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(blockInfo.pos(), newState, newNbt);
                    contraptionEntity.getContraption().getBlocks().put(localPos(), newInfo);
                    contraptionEntity.getContraption().resetClientContraption();
                }
            }
        });
    }
}
