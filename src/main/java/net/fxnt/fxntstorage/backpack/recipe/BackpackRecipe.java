package net.fxnt.fxntstorage.backpack.recipe;

import com.mojang.serialization.MapCodec;
import net.fxnt.fxntstorage.init.ModBlocks;
import net.fxnt.fxntstorage.init.ModDataComponents;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BackpackRecipe extends ShapedRecipe {
    public BackpackRecipe(ShapedRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(RegistryAccess.EMPTY));
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        // Find the backpack in the input
        List<ItemStack> storageItems = input.items().stream().filter(stack -> !stack.isEmpty() && stack.get(DataComponents.CONTAINER) != null).toList();

        if (storageItems.isEmpty())
            return result;

        int newStackMultiplier = storageItems.getFirst().getOrDefault(ModDataComponents.BACKPACK_STACK_MULTIPLIER, 2);
        if (result.getItem().equals(ModBlocks.ANDESITE_BACKPACK.asItem())) {
            newStackMultiplier = Util.ANDESITE_BACKPACK_STACK_MULTIPLIER;
        } else if (result.getItem().equals(ModBlocks.COPPER_BACKPACK.asItem())) {
            newStackMultiplier = Util.COPPER_BACKPACK_STACK_MULTIPLIER;
        } else if (result.getItem().equals(ModBlocks.BRASS_BACKPACK.asItem())) {
            newStackMultiplier = Util.BRASS_BACKPACK_STACK_MULTIPLIER;
        } else if (result.getItem().equals(ModBlocks.HARDENED_BACKPACK.asItem())) {
            newStackMultiplier = Util.HARDENED_BACKPACK_STACK_MULTIPLIER;
        }

        result.set(ModDataComponents.BACKPACK_STACK_MULTIPLIER, newStackMultiplier);
        result.copyFrom(storageItems.getFirst(), DataComponents.CONTAINER);

        return result;
    }

    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements RecipeSerializer<BackpackRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        public static final MapCodec<BackpackRecipe> CODEC = RecipeSerializer.SHAPED_RECIPE.codec().xmap(BackpackRecipe::new, Function.identity());
        public static final StreamCodec<RegistryFriendlyByteBuf, BackpackRecipe> STREAM_CODEC = RecipeSerializer.SHAPED_RECIPE.streamCodec().map(BackpackRecipe::new, Function.identity());

        @Override
        public MapCodec<BackpackRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BackpackRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private Serializer() {
        }
    }

}
