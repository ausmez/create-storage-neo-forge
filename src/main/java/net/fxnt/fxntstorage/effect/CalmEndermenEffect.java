package net.fxnt.fxntstorage.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class CalmEndermenEffect extends MobEffect {
    public CalmEndermenEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x341563);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
