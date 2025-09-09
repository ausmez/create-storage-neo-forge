package net.fxnt.fxntstorage.backpack.upgrade;

import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.main.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class BackpackAsBlockUpgradeHandler {
    private final IBackpackContainer container;
    private final BlockEntity blockEntity;
    private final Level level;
    private final BlockPos pos;
    private static final int magnetUpgradeRange = ConfigManager.CommonConfig.BACKPACK_MAGNET_RANGE.get();

    public BackpackAsBlockUpgradeHandler(BackpackEntity backpackBlockEntity) {
        this.container = backpackBlockEntity;
        this.blockEntity = backpackBlockEntity;
        this.level = blockEntity.getLevel();
        this.pos = blockEntity.getBlockPos();
    }

    // Magnet Upgrade
    public void applyMagnetUpgrade() {
        if (blockEntity != null && !level.isClientSide) {
            // Define the bounding box around the center position
            AABB boundingBox = new AABB(pos).inflate(magnetUpgradeRange);
            // Retrieve all item entities within the range
            List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, boundingBox);

            if (!nearbyItems.isEmpty()) {
                ArmorStand stand = EntityType.ARMOR_STAND.create(level);

                for (ItemEntity itemEntity : nearbyItems) {
                    if (itemEntity.getItem().getItem() instanceof BackpackItem) continue;
                    new BackpackHelper().itemEntityToBackpack(this.container, itemEntity, Util.ITEM_SLOT_START_RANGE, Util.ITEM_SLOT_END_RANGE);

                    if (stand != null) {
                        stand.setPos(pos.getX() + 0.5, pos.getY() - 0.75, pos.getZ() + 0.5);
                        stand.noPhysics = true;
                        stand.setInvisible(true);
                        level.addFreshEntity(stand);

                        ((ServerLevel) level).getChunkSource().broadcast(
                                itemEntity,
                                new ClientboundTakeItemEntityPacket(itemEntity.getId(), stand.getId(), itemEntity.getItem().getCount())
                        );
                    }
                }
            }
        }
    }
}
