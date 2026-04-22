package net.fxnt.fxntstorage.datagen.helper;

import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;

public class ModLootTableHelper {

    public static <T extends Block> NonNullBiConsumer<RegistrateBlockLootTables, T> copyComponents() {
        return (tables, block) -> tables.add(block,
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .add(AlternativesEntry.alternatives(
                                        LootItem.lootTableItem(block)
                                                .when(ModBlockEntitySaveComponents.builder())
                                                .apply(ModCopyBlockEntityData.builder()),
                                        LootItem.lootTableItem(block)
                                ))
                        )
        );
    }
}
