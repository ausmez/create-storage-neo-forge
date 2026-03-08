package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.recipe.BackpackRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

public class ModRecipes {
    public static final ResourceLocation BACKPACK_RECIPE_KEY = ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "backpack_crafting");

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BackpackRecipe>> BACKPACK_RECIPE =
            DeferredHolder.create(BuiltInRegistries.RECIPE_SERIALIZER.key(), BACKPACK_RECIPE_KEY);

    public static void register(RegisterEvent.RegisterHelper<RecipeSerializer<?>> helper) {
        helper.register(BACKPACK_RECIPE_KEY, BackpackRecipe.Serializer.INSTANCE);
    }
}
