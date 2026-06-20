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
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.network.packet.TransferRecipePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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

public class JEIBackpackCraftingTransferHandler implements IRecipeTransferHandler<BackpackMenu, RecipeHolder<CraftingRecipe>> {
    private final IRecipeTransferHandlerHelper transferHelper;
    private final IStackHelper stackHelper;

    public JEIBackpackCraftingTransferHandler(IRecipeTransferHandlerHelper transferHelper, IStackHelper stackHelper) {
        this.transferHelper = transferHelper;
        this.stackHelper = stackHelper;
    }

    @Override
    public Class<? extends BackpackMenu> getContainerClass() {
        return BackpackMenu.class;
    }

    @Override
    public Optional<MenuType<BackpackMenu>> getMenuType() {
        return Optional.empty(); // match by container class
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(BackpackMenu menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        // The grid only exists while the crafting panel is open; hide the button otherwise.
        if (!menu.isCraftingPanelReady()) {
            return transferHelper.createInternalError();
        }

        Inventory playerInventory = player.getInventory();
        Map<Slot, ItemStack> availableItemStacks = new LinkedHashMap<>();

        for (int i = 0; i < playerInventory.getContainerSize(); i++) {
            ItemStack stack = playerInventory.getItem(i);
            if (!stack.isEmpty())
                availableItemStacks.put(new Slot(playerInventory, i, 0, 0), stack);
        }

        IItemHandlerModifiable itemHandler = menu.container.getItemHandler();
        BackpackSlotLayout layout = menu.layout;
        for (int i : layout.items().range()) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty())
                availableItemStacks.put(new SlotItemHandler(itemHandler, i, 0, 0), stack);
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

        List<Slot> craftingSlots = menu.getCraftingMatrixSlots();

        RecipeTransferOperationsResult operations = RecipeTransferUtil.getRecipeTransferOperations(
                stackHelper,
                availableItemStacks,
                recipeSlots.getSlotViews(RecipeIngredientRole.INPUT),
                craftingSlots
        );

        if (!operations.missingItems.isEmpty()) {
            return transferHelper.createUserErrorForMissingSlots(Component.translatable("jei.tooltip.error.recipe.transfer.missing"), operations.missingItems);
        }

        List<Integer> recipeList = new ArrayList<>(operations.results.stream()
                .map(op -> craftingSlots.indexOf(op.craftingSlot(menu)) + 1)
                .toList());

        PacketDistributor.sendToServer(new TransferRecipePacket(recipe.id(), recipeList, maxTransfer, 0));
        return null;
    }
}
