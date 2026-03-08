package net.fxnt.fxntstorage.backpack.upgrade.pickblock;

import net.fxnt.fxntstorage.backpack.upgrade.AbstractUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeContext;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.minecraft.world.item.ItemStack;

public class PickBlockUpgrade extends AbstractUpgrade {

    public PickBlockUpgrade() {
        super(UpgradeType.PICKBLOCK);
    }

    @Override
    public boolean onPickBlock(UpgradeContext context, ItemStack pickedItem) {
        PickBlockHandler.pickBlockHandler(context.player(), context.container(), pickedItem);
        return true;
    }
}
