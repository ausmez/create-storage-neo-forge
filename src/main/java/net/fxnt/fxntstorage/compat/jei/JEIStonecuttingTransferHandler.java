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
import net.fxnt.fxntstorage.backpack.main.BackpackContainer;
import net.fxnt.fxntstorage.backpack.main.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.TransferRecipePacket;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;
import java.util.*;

public class JEIStonecuttingTransferHandler implements IRecipeTransferHandler<StonecutterMenu, StonecutterRecipe> {
    private final IRecipeTransferHandlerHelper transferHelper;
    private final IStackHelper stackHelper;

    public JEIStonecuttingTransferHandler(IRecipeTransferHandlerHelper transferHelper, IStackHelper stackHelper) {
        this.transferHelper = transferHelper;
        this.stackHelper = stackHelper;
    }

    @Override
    public Class<? extends StonecutterMenu> getContainerClass() {
        return StonecutterMenu.class;
    }

    @Override
    public Optional<MenuType<StonecutterMenu>> getMenuType() {
        return Optional.of(MenuType.STONECUTTER);
    }

    @Override
    public RecipeType<StonecutterRecipe> getRecipeType() {
        return RecipeTypes.STONECUTTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(StonecutterMenu container, StonecutterRecipe recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        Inventory playerInventory = player.getInventory();
        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        Map<Slot, ItemStack> availableItemStacks = new LinkedHashMap<>();

        for (int i = 0; i < playerInventory.getContainerSize(); i++) {
            ItemStack stack = playerInventory.getItem(i);
            if (!stack.isEmpty())
                availableItemStacks.put(new Slot(playerInventory, i, 0, 0), stack);
        }

        if (!backpack.isEmpty()) {
            IBackpackContainer backpackContainer = new BackpackContainer(backpack, player);
            IItemHandlerModifiable itemHandler = backpackContainer.getItemHandler();

            for (int i = Util.ITEM_SLOT_START_RANGE; i < Util.ITEM_SLOT_END_RANGE; i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty())
                    availableItemStacks.put(new SlotItemHandler(itemHandler, i, 0, 0), stack);
            }
        }

        List<IRecipeSlotView> missingSlots = new ArrayList<>();
        Map<Slot, ItemStack> remainingStacks = new LinkedHashMap<>();
        availableItemStacks.forEach((slot, stack) -> remainingStacks.put(slot, stack.copy()));

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
            return transferHelper.createUserErrorForMissingSlots(
                    Component.translatable("jei.tooltip.error.recipe.transfer.missing"),
                    missingSlots
            );
        }

        if (!doTransfer) return null;

        List<Slot> inputSlotList = List.of(container.getSlot(1));

        RecipeTransferOperationsResult operations = RecipeTransferUtil.getRecipeTransferOperations(
                stackHelper,
                availableItemStacks,
                recipeSlots.getSlotViews(RecipeIngredientRole.INPUT),
                inputSlotList
        );

        if (!operations.missingItems.isEmpty()) {
            return transferHelper.createUserErrorForMissingSlots(Component.translatable("jei.tooltip.error.recipe.transfer.missing"), operations.missingItems);
        }

        List<Integer> recipeList = operations.results.stream().map(op -> op.craftingSlot(container).index).toList();

        ModNetwork.sendToServer(new TransferRecipePacket(recipe.getId(), recipeList, maxTransfer, (byte) 0));

        Minecraft mc = Minecraft.getInstance();

        Ingredient ingredient = recipe.getIngredients().get(0);
        ItemStack inputStack = ingredient.getItems().length > 0
                ? ingredient.getItems()[0]
                : ItemStack.EMPTY;

        Container inv = new SimpleContainer(inputStack);

        List<StonecutterRecipe> recipes = mc.level.getRecipeManager()
                .getRecipesFor(net.minecraft.world.item.crafting.RecipeType.STONECUTTING, inv, mc.level);

        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).getId().equals(recipe.getId())) {
                mc.gameMode.handleInventoryButtonClick(container.containerId, i);
                break;
            }
        }
        return null;
    }
}
