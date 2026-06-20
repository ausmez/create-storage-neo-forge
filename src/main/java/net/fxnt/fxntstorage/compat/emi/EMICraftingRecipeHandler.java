package net.fxnt.fxntstorage.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.handler.CraftingRecipeHandler;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.network.packet.TransferRecipePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class EMICraftingRecipeHandler extends CraftingRecipeHandler {

    @Override
    public List<Slot> getInputSources(CraftingMenu handler) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return new ArrayList<>();

        List<Slot> slots = new ArrayList<>(handler.slots.stream().filter(slot -> slot.mayPickup(player)).toList());

        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        if (!backpack.isEmpty()) {
            BackpackContainer backpackContainer = BackpackContainer.Cache.getForCapability(backpack);
            IItemHandlerModifiable itemHandler = backpackContainer.getItemHandler();
            BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

            for (int i : layout.items().range()) {
                if (!itemHandler.getStackInSlot(i).isEmpty()) {
                    slots.add(new SlotItemHandler(itemHandler, i, 0, 0));
                }
            }
        }
        return slots;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<CraftingMenu> context) {
        List<ItemStack> stacks = EmiRecipeFiller.getStacks(this, recipe, context.getScreen(), context.getAmount());
        if (stacks != null) {
            Minecraft.getInstance().setScreen(context.getScreen());

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
        return false;
    }
}
