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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BackpackRecipe extends ShapedRecipe {

    private static final Map<Item, Integer> BACKPACK_MULTIPLIERS = Map.of(
            ModBlocks.ANDESITE_BACKPACK.asItem(), Util.ANDESITE_BACKPACK_STACK_MULTIPLIER,
            ModBlocks.COPPER_BACKPACK.asItem(), Util.COPPER_BACKPACK_STACK_MULTIPLIER,
            ModBlocks.BRASS_BACKPACK.asItem(), Util.BRASS_BACKPACK_STACK_MULTIPLIER,
            ModBlocks.HARDENED_BACKPACK.asItem(), Util.HARDENED_BACKPACK_STACK_MULTIPLIER
    );

    private final ShapedRecipe wrapped;

    public BackpackRecipe(ShapedRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(RegistryAccess.EMPTY));
        this.wrapped = recipe;
    }

    public ShapedRecipe getWrapped() {
        return wrapped;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);

        // Find the old backpack in the input
        Optional<ItemStack> oldBackpack = input.items().stream()
                .filter(stack -> !stack.isEmpty()
                        && stack.has(DataComponents.CONTAINER)
                        && stack.has(ModDataComponents.BACKPACK_STACK_MULTIPLIER))
                .findFirst();

        if (oldBackpack.isEmpty()) return result;

        // Copy container contents from old backpack
        result.copyFrom(oldBackpack.get(), DataComponents.CONTAINER);

        // Set the new multiplier
        Integer newStackMultiplier = BACKPACK_MULTIPLIERS.get(result.getItem());
        if (newStackMultiplier != null) {
            result.set(ModDataComponents.BACKPACK_STACK_MULTIPLIER, newStackMultiplier);
        }

        return result;
    }

    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements RecipeSerializer<BackpackRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        public static final MapCodec<BackpackRecipe> CODEC =
                RecipeSerializer.SHAPED_RECIPE.codec().xmap(
                        BackpackRecipe::new,
                        BackpackRecipe::getWrapped
                );

        public static final StreamCodec<RegistryFriendlyByteBuf, BackpackRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPED_RECIPE.streamCodec().map(
                        BackpackRecipe::new,
                        BackpackRecipe::getWrapped
                );

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
