package net.fxnt.fxntstorage.init;

import com.mojang.serialization.MapCodec;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeEnabledCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModConditions {

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, FXNTStorage.MOD_ID);

    public static final Supplier<MapCodec<UpgradeEnabledCondition>> BACKPACK_UPGRADE_ENABLED =
            CONDITION_CODECS.register("backpack_upgrade_enabled", () -> UpgradeEnabledCondition.CODEC);

    public static void register(IEventBus bus) {
        CONDITION_CODECS.register(bus);
    }
}
