package net.fxnt.fxntstorage.compat.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.common.transfer.RecipeTransferOperationsResult;
import mezz.jei.common.transfer.RecipeTransferUtil;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.network.packet.TransferRecipePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class JEICraftingTransferHandler implements IRecipeTransferHandler<CraftingMenu, RecipeHolder<CraftingRecipe>> {
    private final IRecipeTransferHandlerHelper transferHelper;
    private final IStackHelper stackHelper;

    public JEICraftingTransferHandler(IRecipeTransferHandlerHelper transferHelper, IStackHelper stackHelper) {
        this.transferHelper = transferHelper;
        this.stackHelper = stackHelper;
    }

    @Override
    public Class<? extends CraftingMenu> getContainerClass() {
        return CraftingMenu.class;
    }

    @Override
    public Optional<MenuType<CraftingMenu>> getMenuType() {
        return Optional.of(MenuType.CRAFTING);
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(CraftingMenu container, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        Inventory playerInventory = player.getInventory();
        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        Map<Slot, ItemStack> availableItemStacks = new LinkedHashMap<>();

        for (int i = 0; i < playerInventory.getContainerSize(); i++) {
            ItemStack stack = playerInventory.getItem(i);
            if (!stack.isEmpty())
                availableItemStacks.put(new Slot(playerInventory, i, 0, 0), stack);
        }
        if (!backpack.isEmpty()) {
            IBackpackContainer backpackContainer = BackpackContainer.Cache.getOrCreateWornBackpack(player, backpack);
            IItemHandlerModifiable itemHandler = backpackContainer.getItemHandler();
            BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

            for (int i : layout.items().range()) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty())
                    availableItemStacks.put(new SlotItemHandler(itemHandler, i, 0, 0), stack);
            }
        }

        List<IRecipeSlotView> missingSlots = new ArrayList<>();
        Map<Slot, ItemStack> remainingStacks = new LinkedHashMap<>();
        availableItemStacks.forEach((slot, stack) -> remainingStacks.put(slot, stack.copy()));

        // Check if all ingredients are present
        for (IRecipeSlotView slotView : recipeSlots.getSlotViews()) {
            if (slotView.getRole() != RecipeIngredientRole.INPUT || slotView.isEmpty())
                continue;

            boolean matched = false;

            for (ItemStack displayed : slotView.getItemStacks().toList()) {
                if (displayed.isEmpty()) continue;

                Ingredient ingredient = Ingredient.of(displayed);

                for (Map.Entry<Slot, ItemStack> entry : remainingStacks.entrySet()) {
                    ItemStack testStack = entry.getValue();

                    if (!testStack.isEmpty() && ingredient.test(testStack)) {
                        testStack.shrink(1);
                        if (testStack.isEmpty()) {
                            entry.setValue(ItemStack.EMPTY);
                        }
                        matched = true;
                        break;
                    }
                }

                if (matched) break;
            }

            if (!matched) {
                missingSlots.add(slotView);
            }
        }

        if (!missingSlots.isEmpty()) {
            Component message = Component.translatable("jei.tooltip.error.recipe.transfer.missing");
            return transferHelper.createUserErrorForMissingSlots(message, missingSlots);
        }

        // All ingredients found: enable button
        if (!doTransfer) {
            return null;
        }

        List<Slot> craftingSlots = container.slots.subList(1, 10);

        RecipeTransferOperationsResult operations = RecipeTransferUtil.getRecipeTransferOperations(
                stackHelper,
                availableItemStacks,
                recipeSlots.getSlotViews(RecipeIngredientRole.INPUT),
                craftingSlots
        );

        if (!operations.missingItems.isEmpty()) {
            return transferHelper.createUserErrorForMissingSlots(Component.translatable("jei.tooltip.error.recipe.transfer.missing"), operations.missingItems);
        }

        List<Integer> recipeList = new ArrayList<>(operations.results.stream().map(op -> op.craftingSlot(container).index).toList());

        PacketDistributor.sendToServer(new TransferRecipePacket(recipe.id(), recipeList, maxTransfer, 0));
        return null;
    }
}
