package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.effect.CalmEndermenEffect;
import net.fxnt.fxntstorage.effect.PacifyPiglinsEffect;
import net.fxnt.fxntstorage.effect.RepelCreepersEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, FXNTStorage.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> CALM_ENDERMEN = EFFECTS.register("calm_endermen", CalmEndermenEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> PACIFY_PIGLINS = EFFECTS.register("pacify_piglins", PacifyPiglinsEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> REPEL_CREEPERS = EFFECTS.register("repel_creepers", RepelCreepersEffect::new);

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
