package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.datagen.helper.ModBlockEntitySaveTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModLootConditionTypes {

    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, FXNTStorage.MOD_ID);

    public static final RegistryObject<LootItemConditionType> BLOCK_ENTITY_SAVE_TAG =
            LOOT_CONDITIONS.register("block_entity_save_tag",
                    () -> new LootItemConditionType(new ModBlockEntitySaveTag.Serializer()));

    public static void register(IEventBus bus) {
        LOOT_CONDITIONS.register(bus);
    }
}
