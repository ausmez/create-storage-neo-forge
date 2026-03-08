package net.fxnt.fxntstorage.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.handler.InventoryRecipeHandler;
import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.network.packet.TransferRecipePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class EMIInventoryRecipeHandler extends InventoryRecipeHandler {
    private final Player player = Minecraft.getInstance().player;

    @Override
    public List<Slot> getInputSources(InventoryMenu handler) {
        if (player == null) return new ArrayList<>();

        List<Slot> slots = new ArrayList<>(handler.slots.stream().filter(slot -> slot.mayPickup(player)).toList());

        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        if (!backpack.isEmpty()) {
            IBackpackContainer backpackContainer = BackpackContainer.Cache.getOrCreateWornBackpack(player, backpack);
            IItemHandlerModifiable itemHandler = backpackContainer.getItemHandler();
            BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

            for (int i : layout.items().range()) {
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
    public boolean craft(EmiRecipe recipe, EmiCraftContext<InventoryMenu> context) {
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
                PacketDistributor.sendToServer(new TransferRecipePacket(recipe.getId(), recipeList, context.getAmount() > 1, switch (context.getDestination()) {
                    case NONE -> 0;
                    case CURSOR -> 1;
                    case INVENTORY -> 2;
                }));
            }
            return true;
        }
        return false;
    }
}
