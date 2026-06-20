package net.fxnt.fxntstorage.simple_storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompactingRecipeHelper {
    private static final Map<Item, Map.Entry<Item, Integer>> N_TO_ONE = new HashMap<>();
    private static final Map<Item, Map.Entry<Item, Integer>> ONE_TO_N = new HashMap<>();

    public static void rebuild(RecipeManager recipeManager, HolderLookup.Provider registries) {
        N_TO_ONE.clear();
        ONE_TO_N.clear();

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (!(recipe instanceof CraftingRecipe craftingRecipe)) continue;

            ItemStack output = craftingRecipe.getResultItem(registries);
            if (output.isEmpty() || output.getCount() != 1) continue;

            List<Ingredient> ingredients = craftingRecipe.getIngredients();
            if (ingredients.isEmpty()) continue;

            ItemStack first = singleItem(ingredients.getFirst());
            if (first == null || first.isEmpty()) continue;

            // All ingredients must be the same single item
            boolean allSame = true;
            for (int i = 1; i < ingredients.size(); i++) {
                ItemStack next = singleItem(ingredients.get(i));
                if (next == null || next.getItem() != first.getItem()) {
                    allSame = false;
                    break;
                }
            }

            if (allSame && isCompactingRatio(ingredients.size())) {
                int ratio = ingredients.size();
                Item input = first.getItem();
                Item out = output.getItem();
                // When two valid recipes exist for the same input (e.g. 4 iron ingots -> trapdoor
                // and 9 iron ingots -> iron block), prefer the higher ratio - the block recipe
                // is the intended compacting target
                Map.Entry<Item, Integer> existing = N_TO_ONE.get(input);
                if (existing == null || ratio > existing.getValue()) {
                    if (existing != null) ONE_TO_N.remove(existing.getKey());
                    N_TO_ONE.put(input, Map.entry(out, ratio));
                    ONE_TO_N.put(out, Map.entry(input, ratio));
                }
            }
        }
    }

    @Nullable
    private static ItemStack singleItem(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return (items.length == 1) ? items[0] : null;
    }

    @Nullable
    public static CompactingChain buildChain(Item filterItem) {
        // Walk down to find T0
        Item t0 = filterItem;
        for (int safety = 0; safety < 4; safety++) {
            Map.Entry<Item, Integer> prev = ONE_TO_N.get(t0);
            if (prev == null) break;
            t0 = prev.getKey();
        }

        // T1 is what T0 packs into, with the ratio
        Map.Entry<Item, Integer> t1Entry = N_TO_ONE.get(t0);
        if (t1Entry == null) return null;
        Item t1 = t1Entry.getKey();
        int t0ToT1 = t1Entry.getValue();

        // T2 is what T1 packs into (may not exist)
        Map.Entry<Item, Integer> t2Entry = N_TO_ONE.get(t1);
        Item t2 = t2Entry != null ? t2Entry.getKey() : null;
        int t1ToT2 = t2Entry != null ? t2Entry.getValue() : 1;

        return new CompactingChain(t0, t1, t0ToT1, t2, t1ToT2);
    }

    // Only treat recipes that fill a complete square grid as compacting (2×2=4, 3×3=9, etc.)
    private static boolean isCompactingRatio(int n) {
        if (n < 4) return false;
        int sqrt = (int) Math.round(Math.sqrt(n));
        return sqrt * sqrt == n;
    }

    public static boolean isEmpty() {
        return N_TO_ONE.isEmpty();
    }
}
