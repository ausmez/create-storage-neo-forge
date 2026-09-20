package net.fxnt.fxntstorage.simple_storage;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CompactingRecipeHelper {

    private static final int MAX_TIERS = 3;

    private static final Map<Item, Optional<CompactingChain>> CHAIN_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean built = false;

    private record Step(Item item, int ratio) {}

    public static void rebuild() {
        CHAIN_CACHE.clear();
        built = true;
    }

    public static boolean isEmpty() {
        return !built;
    }

    @Nullable
    public static CompactingChain buildChain(Level level, Item filterItem) {
        Optional<CompactingChain> cached = CHAIN_CACHE.get(filterItem);
        if (cached != null) return cached.orElse(null);

        CompactingChain chain = resolveChain(level, filterItem);
        CHAIN_CACHE.putIfAbsent(filterItem, Optional.ofNullable(chain));
        FXNTStorage.LOGGER.debug("Compacting recipe chain has been rebuilt!");
        return chain;
    }

    @Nullable
    private static CompactingChain resolveChain(Level level, Item filterItem) {
        List<Item> items = new ArrayList<>();
        List<Integer> ratios = new ArrayList<>();
        Set<Item> seen = new HashSet<>();
        items.add(filterItem);
        seen.add(filterItem);

        Item cursor = filterItem;
        while (items.size() < MAX_TIERS) {
            Step up = findUpperTier(level, cursor);
            if (up == null || !seen.add(up.item())) break;
            items.addLast(up.item());
            ratios.addLast(up.ratio());
            cursor = up.item();
        }

        cursor = filterItem;
        while (items.size() < MAX_TIERS) {
            Step down = findLowerTier(level, cursor);
            if (down == null || !seen.add(down.item())) break;
            items.addFirst(down.item());
            ratios.addFirst(down.ratio());
            cursor = down.item();
        }

        if (items.size() < 2) return null;

        Item t2 = items.size() > 2 ? items.get(2) : null;
        int t1ToT2 = items.size() > 2 ? ratios.get(1) : 1;
        return new CompactingChain(items.get(0), items.get(1), ratios.get(0), t2, t1ToT2);
    }

    // What a full grid of this item crafts into, 3x3 preferred over 2x2.
    @Nullable
    private static Step findUpperTier(Level level, Item item) {
        Step step = matchSquare(level, item, 3);
        return step != null ? step : matchSquare(level, item, 2);
    }

    @Nullable
    private static Step matchSquare(Level level, Item item, int size) {
        int ratio = size * size;
        CraftingInput input = filledGrid(item, size, size);

        List<Item> candidates = new ArrayList<>();
        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getRecipesFor(RecipeType.CRAFTING, input, level)) {
            ItemStack result = holder.value().assemble(input, level.registryAccess());
            if (result.isEmpty() || result.getCount() != 1 || result.getItem() == item) continue;

            // Every slot holds our item, so any ingredient accepts it; the first one stands for the rest.
            List<Ingredient> ingredients = holder.value().getIngredients();
            if (ingredients.isEmpty()) continue;
            if (!trustworthy(level, ingredients.getFirst(), result.getItem(), ratio)) continue;

            if (!candidates.contains(result.getItem())) candidates.add(result.getItem());
        }

        return pick(candidates, item, ratio);
    }

    // What this item is a packed form of, found by scanning for recipes that produce it
    @Nullable
    private static Step findLowerTier(Level level, Item item) {
        List<Item> candidates = new ArrayList<>();
        int bestRatio = 0;

        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = holder.value();
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (result.isEmpty() || result.getCount() != 1 || result.getItem() != item) continue;

            List<Ingredient> ingredients = recipe.getIngredients();
            int ratio = ingredients.size();
            // Mirrors the 3x3-before-2x2 preference on the way up
            if ((ratio != 4 && ratio != 9) || ratio < bestRatio) continue;

            List<Item> variants = uniformIngredientItems(ingredients);
            variants.remove(item);
            if (variants.isEmpty()) continue;
            if (!trustworthy(level, ingredients.getFirst(), item, ratio)) continue;

            if (ratio > bestRatio) {
                candidates.clear();
                bestRatio = ratio;
            }
            for (Item variant : variants) {
                if (!candidates.contains(variant)) candidates.add(variant);
            }
        }

        return pick(candidates, item, bestRatio);
    }

    private static List<Item> uniformIngredientItems(List<Ingredient> ingredients) {
        List<Item> result = new ArrayList<>();
        // Ingredient.EMPTY resolves to nothing, so shaped recipes with holes drop out here
        for (ItemStack reference : ingredients.getFirst().getItems()) {
            if (reference.isEmpty()) continue;

            boolean acceptedEverywhere = true;
            for (int i = 1; i < ingredients.size(); i++) {
                if (!ingredients.get(i).test(reference)) {
                    acceptedEverywhere = false;
                    break;
                }
            }

            if (acceptedEverywhere && !result.contains(reference.getItem())) result.add(reference.getItem());
        }
        return result;
    }

    private static boolean trustworthy(Level level, Ingredient ingredient, Item packed, int count) {
        if (ingredient.getItems().length <= 1) return true;

        CraftingInput input = filledGrid(packed, 1, 1);
        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getRecipesFor(RecipeType.CRAFTING, input, level)) {
            ItemStack result = holder.value().assemble(input, level.registryAccess());
            if (result.getCount() == count && ingredient.test(result)) return true;
        }
        return false;
    }

    // Prefer a candidate from the same mod as the item being resolved, then lowest id
    @Nullable
    private static Step pick(List<Item> candidates, Item reference, int ratio) {
        if (candidates.isEmpty() || ratio <= 0) return null;

        String preferred = idOf(reference).getNamespace();
        Item best = candidates.stream()
                .min(Comparator.comparingInt((Item candidate) -> idOf(candidate).getNamespace().equals(preferred) ? 0 : 1)
                        .thenComparing(candidate -> idOf(candidate).toString()))
                .orElseThrow();
        return new Step(best, ratio);
    }

    private static ResourceLocation idOf(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    private static CraftingInput filledGrid(Item item, int width, int height) {
        List<ItemStack> slots = new ArrayList<>(width * height);
        for (int i = 0; i < width * height; i++) slots.add(new ItemStack(item));
        return CraftingInput.of(width, height, slots);
    }
}
