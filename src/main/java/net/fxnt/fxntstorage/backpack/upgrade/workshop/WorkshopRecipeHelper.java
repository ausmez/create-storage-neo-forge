package net.fxnt.fxntstorage.backpack.upgrade.workshop;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class WorkshopRecipeHelper {

    private WorkshopRecipeHelper() {
    }

    public record Result(ItemStack output, boolean consumeHeld, boolean damageHeld) {
    }

    public static boolean isMachine(ItemStack stack) {
        return isDeployer(stack) || isPress(stack);
    }

    public static boolean isDeployer(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == AllBlocks.DEPLOYER.asItem();
    }

    public static boolean isPress(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == AllBlocks.MECHANICAL_PRESS.asItem();
    }

    public static boolean isBacktank(ItemStack stack) {
        return !stack.isEmpty() && stack.is(AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.tag);
    }

    @Nullable
    public static Result resolve(Level level, ItemStack machine, ItemStack input, ItemStack held) {
        if (level == null || input.isEmpty()) return null;

        if (isPress(machine)) {
            return AllRecipeTypes.PRESSING.<SingleRecipeInput, PressingRecipe>find(new SingleRecipeInput(input), level)
                    .filter(AllRecipeTypes.CAN_BE_AUTOMATED)
                    .map(holder -> new Result(primaryOutput(holder), false, false))
                    .filter(r -> !r.output().isEmpty())
                    .orElse(null);
        }

        if (isDeployer(machine)) {
            if (held.isEmpty()) return null;

            if (held.getItem() instanceof SandPaperItem) {
                Optional<RecipeHolder<SandPaperPolishingRecipe>> polishing =
                        AllRecipeTypes.SANDPAPER_POLISHING.<SingleRecipeInput, SandPaperPolishingRecipe>find(
                                        new SingleRecipeInput(input), level)
                                .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
                if (polishing.isPresent()) {
                    ItemStack out = primaryOutput(polishing.get());
                    return out.isEmpty() ? null : new Result(out, false, true);
                }
            }

            ItemStackHandler tmp = new ItemStackHandler(2);
            tmp.setStackInSlot(0, input);
            tmp.setStackInSlot(1, held);
            RecipeWrapper wrapper = new RecipeWrapper(tmp);

            Optional<RecipeHolder<ItemApplicationRecipe>> found =
                    AllRecipeTypes.DEPLOYING.<RecipeWrapper, ItemApplicationRecipe>find(wrapper, level)
                            .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
            if (found.isEmpty()) {
                found = AllRecipeTypes.ITEM_APPLICATION.<RecipeWrapper, ItemApplicationRecipe>find(wrapper, level)
                        .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
            }
            return found
                    .map(holder -> new Result(primaryOutput(holder), !holder.value().shouldKeepHeldItem(), false))
                    .filter(r -> !r.output().isEmpty())
                    .orElse(null);
        }

        return null;
    }

    private static ItemStack primaryOutput(RecipeHolder<? extends com.simibubi.create.content.processing.recipe.ProcessingRecipe<?, ?>> holder) {
        List<ProcessingOutput> results = holder.value().getRollableResults();
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst().getStack().copy();
    }
}
