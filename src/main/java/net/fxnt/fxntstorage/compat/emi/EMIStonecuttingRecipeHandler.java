package net.fxnt.fxntstorage.compat.emi;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.handler.StonecuttingRecipeHandler;
import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.TransferRecipePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

public class EMIStonecuttingRecipeHandler extends StonecuttingRecipeHandler {
    private final Player player = Minecraft.getInstance().player;

    @Override
    public List<Slot> getInputSources(StonecutterMenu handler) {
        if (player == null) return new ArrayList<>();

        List<Slot> slots = new ArrayList<>(handler.slots.stream().filter(slot -> slot.mayPickup(player)).toList());

        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        if (!backpack.isEmpty()) {
            IBackpackContainer backpackContainer = new BackpackContainer(player, backpack);
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
    public boolean craft(EmiRecipe recipe, EmiCraftContext<StonecutterMenu> context) {
        List<ItemStack> stacks = EmiRecipeFiller.getStacks(this, recipe, context.getScreen(), context.getAmount());
        if (stacks != null) {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(context.getScreen());
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

            Minecraft client = Minecraft.getInstance();

            Container inv = new SimpleContainer(recipe.getInputs().get(0).getEmiStacks().get(0).getItemStack());
            List<StonecutterRecipe> recipes = client.level.getRecipeManager().getRecipesFor(RecipeType.STONECUTTING, inv, client.level);

            for (int i = 0; i < recipes.size(); i++) {
                if (EmiPort.getId(recipes.get(i)) != null && EmiPort.getId(recipes.get(i)).equals((recipe.getId()))) {
                    StonecutterMenu sh = context.getScreenHandler();
                    mc.gameMode.handleInventoryButtonClick(sh.containerId, i);
                    if (context.getDestination() == EmiCraftContext.Destination.CURSOR) {
                        mc.gameMode.handleInventoryMouseClick(sh.containerId, 1, 0, ClickType.PICKUP, mc.player);
                    } else if (context.getDestination() == EmiCraftContext.Destination.INVENTORY) {
                        mc.gameMode.handleInventoryMouseClick(sh.containerId, 1, 0, ClickType.QUICK_MOVE, mc.player);
                    }
                    break;
                }
            }
            return true;
        }
        return false;
    }
}
