package net.fxnt.fxntstorage.datagen;

import com.tterrag.registrate.util.entry.RegistryEntry;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.fxnt.fxntstorage.container.StorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        FXNTStorage.REGISTRATE
                .getAll(Registries.BLOCK)
                .stream()
                .map(RegistryEntry::get)
                .forEach(block -> {
                    if (block instanceof StorageBox || block instanceof SimpleStorageBox || block instanceof BackpackBlock) {
                        dropWithInventory(block);
                    } else {
                        dropSelf(block);
                    }
                });
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return FXNTStorage.REGISTRATE.getAll(Registries.BLOCK).stream().map(Holder::value)::iterator;
    }

    private void dropWithInventory(Block block) {
        this.add(block, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .when(ExplosionCondition.survivesExplosion())
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(block)
                                .apply(CopyNameFunction.copyName(CopyNameFunction.NameSource.BLOCK_ENTITY))
                                .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY))
                        )
                ));
    }

}
