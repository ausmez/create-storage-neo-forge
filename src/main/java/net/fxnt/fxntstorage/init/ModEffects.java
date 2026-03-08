package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.effect.CalmEndermenEffect;
import net.fxnt.fxntstorage.effect.PacifyPiglinsEffect;
import net.fxnt.fxntstorage.effect.RepelCreepersEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FXNTStorage.MOD_ID);

    public static final RegistryObject<MobEffect> CALM_ENDERMEN = EFFECTS.register("calm_endermen", CalmEndermenEffect::new);
    public static final RegistryObject<MobEffect> PACIFY_PIGLINS = EFFECTS.register("pacify_piglins", PacifyPiglinsEffect::new);
    public static final RegistryObject<MobEffect> REPEL_CREEPERS = EFFECTS.register("repel_creepers", RepelCreepersEffect::new);

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
