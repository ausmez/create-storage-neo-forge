package net.fxnt.fxntstorage.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.handler.CraftingRecipeHandler;
import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.fxnt.fxntstorage.backpack.main.BackpackContainer;
import net.fxnt.fxntstorage.backpack.main.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.TransferRecipePacket;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

public class EMICraftingRecipeHandler extends CraftingRecipeHandler {
    private final Player player = Minecraft.getInstance().player;

    @Override
    public List<Slot> getInputSources(CraftingMenu handler) {
        List<Slot> slots = new ArrayList<>(handler.slots.stream().filter(slot -> slot.mayPickup(player)).toList());

        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        if (!backpack.isEmpty()) {
            IBackpackContainer backpackContainer = new BackpackContainer(backpack, player);
            IItemHandlerModifiable itemHandler = backpackContainer.getItemHandler();

            for (int i = Util.ITEM_SLOT_START_RANGE; i < Util.ITEM_SLOT_END_RANGE; i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Slot fakeSlot = new SlotItemHandler(itemHandler, i, 0, 0);
                    fakeSlot.set(stack);
                    slots.add(fakeSlot);
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
            if (!EmiClient.onServer) {
                return EmiRecipeFiller.clientFill(this, recipe, context.getScreen(), stacks, context.getDestination());
            } else {
                List<Integer> recipeList = new ArrayList<>();
                for (int i = 0; i < stacks.size(); i++) {
                    if (!stacks.get(i).isEmpty())
                        recipeList.add(i + 1);
                }
                ModNetwork.sendToServer(new TransferRecipePacket(recipe.getId(), recipeList, context.getAmount() > 1, switch (context.getDestination()) {
                    case NONE -> (byte) 0;
                    case CURSOR -> (byte) 1;
                    case INVENTORY -> (byte) 2;
                }));
            }
            return true;
        }
        return false;
    }
}
