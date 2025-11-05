package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.datagen.helper.ModBlockEntitySaveComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModLootConditionTypes {

    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, FXNTStorage.MOD_ID);

    public static final Supplier<LootItemConditionType> BLOCK_ENTITY_SAVE_COMPONENTS =
            LOOT_CONDITIONS.register("block_entity_save_components",
                    () -> new LootItemConditionType(ModBlockEntitySaveComponents.CODEC));

    public static void register(IEventBus bus) {
        LOOT_CONDITIONS.register(bus);
    }

}
