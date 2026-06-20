package net.fxnt.fxntstorage.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.network.packet.TransferRecipePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class EMIBackpackCraftingRecipeHandler implements StandardRecipeHandler<BackpackMenu> {

    @Override
    public List<Slot> getInputSources(BackpackMenu handler) {
        List<Slot> slots = new ArrayList<>();

        // Player inventory slots already present in the menu.
        for (Slot slot : handler.slots) {
            if (slot.container instanceof Inventory) slots.add(slot);
        }

        // The backpack's own item section (not the crafting grid).
        IItemHandlerModifiable itemHandler = handler.container.getItemHandler();
        BackpackSlotLayout layout = handler.layout;
        for (int i : layout.items().range()) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                slots.add(new SlotItemHandler(itemHandler, i, 0, 0));
            }
        }
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(BackpackMenu handler) {
        return handler.isCraftingPanelReady() ? handler.getCraftingMatrixSlots() : List.of();
    }

    @Override
    public Slot getOutputSlot(BackpackMenu handler) {
        return handler.getCraftingResultSlot();
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        if (recipe.getCategory() != VanillaEmiRecipeCategories.CRAFTING) return false;

        LocalPlayer player = Minecraft.getInstance().player;
        return player != null
                && player.containerMenu instanceof BackpackMenu bm
                && bm.isCraftingPanelReady();
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<BackpackMenu> context) {
        List<ItemStack> stacks = EmiRecipeFiller.getStacks(this, recipe, context.getScreen(), context.getAmount());
        if (stacks == null) return false;

        List<Integer> recipeList = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i++) {
            if (!stacks.get(i).isEmpty())
                recipeList.add(i + 1);
        }
        PacketDistributor.sendToServer(new TransferRecipePacket(recipe.getId(), recipeList, context.getAmount() > 1, switch (context.getDestination()) {
            case NONE -> 0;
            case CURSOR -> 1;
            case INVENTORY -> 2;
        }));
        return true;
    }
}
