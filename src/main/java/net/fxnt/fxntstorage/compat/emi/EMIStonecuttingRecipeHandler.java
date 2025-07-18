package net.fxnt.fxntstorage.compat.emi;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.handler.StonecuttingRecipeHandler;
import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.fxnt.fxntstorage.backpack.main.BackpackContainer;
import net.fxnt.fxntstorage.backpack.main.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.network.packet.TransferRecipePacket;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class EMIStonecuttingRecipeHandler extends StonecuttingRecipeHandler {
    private final Player player = Minecraft.getInstance().player;

    @Override
    public List<Slot> getInputSources(StonecutterMenu handler) {
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
                PacketDistributor.sendToServer(new TransferRecipePacket(recipe.getId(), recipeList, context.getAmount() > 1, switch (context.getDestination()) {
                    case NONE -> 0;
                    case CURSOR -> 1;
                    case INVENTORY -> 2;
                }));
            }

            EmiIngredient emiInput = recipe.getInputs().getFirst();
            Ingredient ingredient = Ingredient.of(
                    emiInput.getEmiStacks().stream()
                            .map(emiStack -> emiStack.getItemStack().copy())
                            .toArray(ItemStack[]::new)
            );

            Set<RecipeHolder<StonecutterRecipe>> allMatching = new HashSet<>();

            for (ItemStack stack : ingredient.getItems()) {
                SingleRecipeInput inv = new SingleRecipeInput(stack);
                List<RecipeHolder<StonecutterRecipe>> matches = mc.level.getRecipeManager()
                        .getRecipesFor(RecipeType.STONECUTTING, inv, mc.level);
                allMatching.addAll(matches);
            }

            List<StonecutterRecipe> sorted = allMatching.stream()
                    .map(RecipeHolder::value)
                    .sorted(Comparator.comparing(r ->
                            r.getResultItem(mc.level.registryAccess()).getItem().getDescriptionId()))
                    .toList();

            for (int i = 0; i < sorted.size(); i++) {
                if (EmiPort.getId(sorted.get(i)) != null && EmiPort.getId(sorted.get(i)).equals(recipe.getId())) {
                    StonecutterMenu menu = context.getScreenHandler();
                    mc.gameMode.handleInventoryButtonClick(menu.containerId, i);

                    if (context.getDestination() == EmiCraftContext.Destination.CURSOR) {
                        mc.gameMode.handleInventoryMouseClick(menu.containerId, 1, 0, ClickType.PICKUP, mc.player);
                    } else if (context.getDestination() == EmiCraftContext.Destination.INVENTORY) {
                        mc.gameMode.handleInventoryMouseClick(menu.containerId, 1, 0, ClickType.QUICK_MOVE, mc.player);
                    }
                    break;
                }
            }

            return true;
        }

        return false;
    }
}
