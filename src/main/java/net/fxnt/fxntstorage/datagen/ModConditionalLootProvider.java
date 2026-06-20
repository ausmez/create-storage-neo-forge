package net.fxnt.fxntstorage.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.fxnt.fxntstorage.datagen.helper.ModLootTableHelper;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModCompats;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;

public class ModConditionalLootProvider implements DataProvider {

    private final PackOutput.PathProvider pathProvider;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public ModConditionalLootProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table");
        this.registries = registries;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cache) {
        if (ModBlocks.SIMPLE_STORAGE_BOX_PALE_OAK == null || ModBlocks.STORAGE_TRIM_PALE_OAK == null)
            return CompletableFuture.allOf();

        return registries.thenCompose(provider -> {
            RegistryOps<JsonElement> ops = provider.createSerializationContext(JsonOps.INSTANCE);
            List<CompletableFuture<?>> futures = new ArrayList<>();
            futures.add(save(cache, ops, modLoc("blocks/pale_oak_simple_storage_box"),
                    ModLootTableHelper.copyComponentsTable(ModBlocks.SIMPLE_STORAGE_BOX_PALE_OAK.get())));
            futures.add(save(cache, ops, modLoc("blocks/pale_oak_storage_trim"),
                    dropSelf(ModBlocks.STORAGE_TRIM_PALE_OAK.get())));
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        });
    }

    private CompletableFuture<?> save(CachedOutput cache, RegistryOps<JsonElement> ops, ResourceLocation id, LootTable.Builder builder) {
        LootTable table = builder
                .setParamSet(LootContextParamSets.BLOCK)
                .setRandomSequence(id)
                .build();
        JsonObject json = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().getAsJsonObject();

        JsonObject modLoaded = new JsonObject();
        modLoaded.addProperty("type", "neoforge:mod_loaded");
        modLoaded.addProperty("modid", ModCompats.VANILLA_BACKPORT);
        JsonArray conditions = new JsonArray();
        conditions.add(modLoaded);
        json.add("neoforge:conditions", conditions);

        return DataProvider.saveStable(cache, json, pathProvider.json(id));
    }

    private static LootTable.Builder dropSelf(Block block) {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(block))
                        .when(ExplosionCondition.survivesExplosion()));
    }

    @Override
    public @NotNull String getName() {
        return "Create: Storage Conditional Loot Tables";
    }
}
