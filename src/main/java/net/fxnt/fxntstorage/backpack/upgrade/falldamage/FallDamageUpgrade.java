package net.fxnt.fxntstorage.backpack.upgrade.falldamage;

import net.fxnt.fxntstorage.backpack.upgrade.AbstractUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeContext;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.minecraftforge.event.entity.living.LivingFallEvent;

public class FallDamageUpgrade extends AbstractUpgrade {

    public FallDamageUpgrade() {
        super(UpgradeType.FALLDAMAGE);
    }

    @Override
    public boolean onLivingFall(UpgradeContext context, LivingFallEvent event) {
        return true;
    }
}
