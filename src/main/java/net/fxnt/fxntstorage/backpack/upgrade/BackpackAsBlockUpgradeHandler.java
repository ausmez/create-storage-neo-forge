package net.fxnt.fxntstorage.backpack.upgrade;

import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.main.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModEntityTypes;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
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
            AABB boundingBox = new AABB(pos).inflate(ConfigManager.CommonConfig.BACKPACK_MAGNET_RANGE.get());
            // Retrieve all item entities within the range
            List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, boundingBox);

            if (!nearbyItems.isEmpty()) {
                MagnetPickupEntity stand = ModEntityTypes.MAGNET_PICKUP_ENTITY.create(level);

                if (stand != null) {
                    stand.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    level.addFreshEntity(stand);

                    for (ItemEntity itemEntity : nearbyItems) {
                        if (itemEntity.getItem().getItem() instanceof BackpackItem) continue;
                        boolean itemsAdded = new BackpackHelper().itemEntityToBackpack(this.container, itemEntity, Util.ITEM_SLOT_START_RANGE, Util.ITEM_SLOT_END_RANGE);

                        if (itemsAdded) stand.take(itemEntity, itemEntity.getItem().getCount());
                    }
                    stand.discard();
                }
            }
        }
    }

}
