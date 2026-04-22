package net.fxnt.fxntstorage.datagen.helper;

import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;

public class ModLootTableHelper {

    public static <T extends Block> NonNullBiConsumer<RegistrateBlockLootTables, T> copyBlockEntityTag() {
        return (tables, block) -> tables.add(block,
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .add(AlternativesEntry.alternatives(
                                        LootItem.lootTableItem(block)
                                                .when(ModBlockEntitySaveTag.builder())
                                                .apply(CopyNameFunction.copyName(CopyNameFunction.NameSource.BLOCK_ENTITY))
                                                .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                                                        .copy("{}", "BlockEntityTag")),
                                        LootItem.lootTableItem(block)
                                ))
                        ));
    }

    public static <T extends Block> NonNullBiConsumer<RegistrateBlockLootTables, T> copyBlockEntityTagBackpack() {
        return (tables, block) -> tables.add(block,
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .when(ExplosionCondition.survivesExplosion())
                                .add(LootItem.lootTableItem(block)
                                        .apply(CopyNameFunction.copyName(CopyNameFunction.NameSource.BLOCK_ENTITY))
                                        .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                                                .copy("{}", "BlockEntityTag", CopyNbtFunction.MergeStrategy.REPLACE)
                                        )
                                )
                        )
        );
    }
}
