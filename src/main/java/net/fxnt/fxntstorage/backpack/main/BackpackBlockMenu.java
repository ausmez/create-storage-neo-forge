package net.fxnt.fxntstorage.backpack.main;

import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BackpackBlockMenu extends BackpackMenu {

    public BackpackBlockMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData.readBlockPos()));
    }

    public BackpackBlockMenu(int containerId, Inventory playerInventory, IBackpackContainer container) {
        super(ModMenuTypes.BACKPACK_BLOCK_MENU.get(), containerId, playerInventory, container, Util.BACKPACK_AS_BLOCK);
    }

    private static IBackpackContainer getBlockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntityAtPos = playerInventory.player.level().getBlockEntity(pos);

        if (blockEntityAtPos instanceof BackpackEntity backbackpackEntity) {
            return backbackpackEntity;
        } else {
            throw new IllegalStateException("Block entity is not correct or is null: " + blockEntityAtPos);
        }
    }

}
