package net.fxnt.fxntstorage.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class RepelCreepersEffect extends MobEffect {
    public RepelCreepersEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x13A90F);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
