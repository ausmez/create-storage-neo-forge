package net.fxnt.fxntstorage.backpack.recipe;

import com.google.gson.JsonObject;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModRecipes;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BackpackRecipe extends ShapedRecipe {
    private ItemStack craftingStack;

    public BackpackRecipe(ResourceLocation id, String group, int width, int height, NonNullList<Ingredient> recipeItems, ItemStack result) {
        super(id, group, CraftingBookCategory.MISC, width, height, recipeItems, result);
    }

    @Override
    public boolean matches(CraftingContainer inv, @NotNull Level level) {
        ItemStack[] items = inv.getItems().toArray(new ItemStack[0]);
        for (ItemStack itemStack : items) {
            if (itemStack.getItem() instanceof BackpackItem) {
                this.craftingStack = itemStack;
                break;
            }
        }
        return super.matches(inv, level);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.BACKPACK_RECIPE_SERIALIZER.get();
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        ItemStack craftedStack = super.getResultItem(registryAccess);
        if (craftingStack == null) return craftedStack;

        if (craftingStack.hasTag()) {
            craftedStack.setTag(craftingStack.getTag());
            CompoundTag entityTag = craftedStack.getOrCreateTagElement("BlockEntityTag");
            if (entityTag.contains("StackMultiplier")) {
                int newMaxStackSize = entityTag.getInt("StackMultiplier");
                // Handle different backpack types
                if (craftedStack.getItem().equals(ModBlocks.ANDESITE_BACKPACK.asItem())) {
                    newMaxStackSize = Util.ANDESITE_BACKPACK_STACK_MULTIPLIER;
                } else if (craftedStack.getItem().equals(ModBlocks.COPPER_BACKPACK.asItem())) {
                    newMaxStackSize = Util.COPPER_BACKPACK_STACK_MULTIPLIER;
                } else if (craftedStack.getItem().equals(ModBlocks.BRASS_BACKPACK.asItem())) {
                    newMaxStackSize = Util.BRASS_BACKPACK_STACK_MULTIPLIER;
                } else if (craftedStack.getItem().equals(ModBlocks.HARDENED_BACKPACK.asItem())) {
                    newMaxStackSize = Util.HARDENED_BACKPACK_STACK_MULTIPLIER;
                }
                entityTag.putInt("StackMultiplier", newMaxStackSize);
            }
        }
        return craftedStack;
    }

    private static BackpackRecipe fromShaped(ShapedRecipe recipe) {
        return new BackpackRecipe(recipe.getId(), recipe.getGroup(), recipe.getWidth(), recipe.getHeight(),
                recipe.getIngredients(), recipe.getResultItem(null));
    }

    public static class Serializer extends ShapedRecipe.Serializer {
        @Override
        public @NotNull BackpackRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            return fromShaped(super.fromJson(recipeId, json));
        }

        @Override
        public @NotNull BackpackRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
            return fromShaped(Objects.requireNonNull(super.fromNetwork(recipeId, buffer)));
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf pBuffer, @NotNull ShapedRecipe pRecipe) {
            super.toNetwork(pBuffer, pRecipe);
        }
    }

}
