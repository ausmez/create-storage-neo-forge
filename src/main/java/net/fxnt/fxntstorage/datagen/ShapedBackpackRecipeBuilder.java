package net.fxnt.fxntstorage.datagen;

import net.fxnt.fxntstorage.backpack.recipe.BackpackRecipe;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ShapedBackpackRecipeBuilder implements RecipeBuilder {
    private final RecipeCategory category;
    private final Item result;
    private final int count;
    private final Map<Character, Ingredient> key = new LinkedHashMap<>();
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    private final List<String> rows = new ArrayList<>();
    private String group = "";
    private boolean showNotification = true;

    private ShapedBackpackRecipeBuilder(RecipeCategory category, Item result, int count) {
        this.category = category;
        this.result = result;
        this.count = count;
    }

    public static ShapedBackpackRecipeBuilder shaped(RecipeCategory category, ItemLike result) {
        return shaped(category, result, 1);
    }

    public static ShapedBackpackRecipeBuilder shaped(RecipeCategory category, ItemLike result, int count) {
        return new ShapedBackpackRecipeBuilder(category, result.asItem(), count);
    }

    public ShapedBackpackRecipeBuilder define(Character symbol, TagKey<Item> tag) {
        return define(symbol, Ingredient.of(tag));
    }

    public ShapedBackpackRecipeBuilder define(Character symbol, ItemLike item) {
        return define(symbol, Ingredient.of(item));
    }

    public ShapedBackpackRecipeBuilder define(Character symbol, Ingredient ingredient) {
        if (this.key.containsKey(symbol)) {
            throw new IllegalArgumentException("Symbol '" + symbol + "' is already defined!");
        } else if (symbol == ' ') {
            throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
        } else {
            this.key.put(symbol, ingredient);
            return this;
        }
    }

    public ShapedBackpackRecipeBuilder pattern(String pattern) {
        if (!this.rows.isEmpty() && pattern.length() != this.rows.getFirst().length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        } else {
            this.rows.add(pattern);
            return this;
        }
    }

    @Override
    public RecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    public ShapedBackpackRecipeBuilder group(@Nullable String group) {
        this.group = group == null ? "" : group;
        return this;
    }

    public ShapedBackpackRecipeBuilder showNotification(boolean showNotification) {
        this.showNotification = showNotification;
        return this;
    }

    @Override
    public Item getResult() {
        return this.result;
    }

    @Override
    public void save(RecipeOutput output, ResourceLocation id) {
        ShapedRecipePattern pattern = this.ensureValid(id);
        Advancement.Builder advancement$builder = output.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                .rewards(AdvancementRewards.Builder.recipe(id))
                .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement$builder::addCriterion);
        ShapedRecipe shapedRecipe = new ShapedRecipe(
                this.group,
                CraftingBookCategory.MISC,
                pattern,
                new ItemStack(this.result, this.count),
                this.showNotification
        );
        BackpackRecipe backpackRecipe = new BackpackRecipe(shapedRecipe);
        output.accept(id, backpackRecipe, advancement$builder.build(id.withPrefix("recipes/" + this.category.getFolderName() + "/")));
    }

    private ShapedRecipePattern ensureValid(ResourceLocation id) {
        if (this.rows.isEmpty()) {
            throw new IllegalStateException("No pattern is defined for shaped recipe " + id + "!");
        } else {
            Set<Character> definedSymbols = this.key.keySet();
            Set<Character> usedSymbols = new HashSet<>();

            for (String row : this.rows) {
                for (int i = 0; i < row.length(); i++) {
                    char c = row.charAt(i);
                    if (!definedSymbols.contains(c) && c != ' ') {
                        throw new IllegalStateException(
                                "Pattern in recipe " + id + " uses undefined symbol '" + c + "'"
                        );
                    }
                    usedSymbols.add(c);
                }
            }

            for (Character symbol : definedSymbols) {
                if (!usedSymbols.contains(symbol)) {
                    throw new IllegalStateException(
                            "Ingredient '" + symbol + "' is defined but not used in pattern for recipe " + id
                    );
                }
            }

            if (this.rows.size() == 1 && this.rows.getFirst().length() == 1) {
                throw new IllegalStateException(
                        "Shaped recipe " + id + " only takes in a single item - should it be a shapeless recipe instead?"
                );
            } else {
                return ShapedRecipePattern.of(this.key, this.rows);
            }
        }
    }
}
