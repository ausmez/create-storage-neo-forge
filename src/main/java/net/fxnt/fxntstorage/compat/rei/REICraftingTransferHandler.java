package net.fxnt.fxntstorage.compat.rei;

import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.InputIngredient;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.TransferRecipePacket;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static me.shedaniel.rei.impl.client.transfer.SimpleTransferHandlerImpl.hasItemsIndexed;

@SuppressWarnings("UnstableApiUsage")
public class REICraftingTransferHandler implements SimpleTransferHandler {

    @Override
    public ApplicabilityResult checkApplicable(Context context) {
        return ((context.getMenu() instanceof CraftingMenu) && context.getDisplay().getCategoryIdentifier() == BuiltinPlugin.CRAFTING) && context.getContainerScreen() != null
                ? ApplicabilityResult.createApplicable()
                : ApplicabilityResult.createNotApplicable();
    }

    @Override
    public Iterable<SlotAccessor> getInputSlots(Context context) {
        return IntStream.range(1, 10).mapToObj(id -> SlotAccessor.fromSlot(context.getMenu().getSlot(id))).toList();
    }

    @Override
    public Iterable<SlotAccessor> getInventorySlots(Context context) {
        LocalPlayer player = context.getMinecraft().player;
        Inventory inventory = player.getInventory();

        // Add player inventory
        List<SlotAccessor> slotAccessors = new ArrayList<>(IntStream.range(0, inventory.items.size())
                .mapToObj(index -> SlotAccessor.fromPlayerInventory(player, index))
                .toList());

        // Add backpack inventory
        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);

        if (!backpack.isEmpty()) {
            IBackpackContainer backpackContainer = new BackpackContainer(player, backpack);
            IItemHandlerModifiable itemHandler = backpackContainer.getItemHandler();
            BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

            slotAccessors.addAll(
                    IntStream.range(layout.items().getStartIndex(), layout.items().getEndIndex())
                            .mapToObj(index -> new SlotItemHandler(itemHandler, index, 0, 0))
                            .map(SlotAccessor::fromSlot)
                            .toList()
            );
        }

        return slotAccessors;
    }

    @Override
    public Result handle(Context context) {
        List<InputIngredient<ItemStack>> missing = hasItemsIndexed(context, getInventorySlots(context), getInputsIndexed(context));

        if (missing.isEmpty()) {
            if (!context.isActuallyCrafting()) {
                return Result.createSuccessful();
            } else {
                AbstractContainerScreen<?> containerScreen = context.getContainerScreen();
                context.getMinecraft().setScreen(containerScreen);

                Display display = context.getDisplay();
                if (display.getDisplayLocation().isEmpty()) return Result.createNotApplicable();

                List<InputIngredient<EntryStack<?>>> stacks = context.getDisplay().getInputIngredients(null, null);
                List<Integer> recipeList = new ArrayList<>();
                for (int i = 0; i < stacks.size(); i++) {
                    if (!stacks.get(i).get().isEmpty())
                        recipeList.add(i + 1);
                }

                ModNetwork.sendToServer(new TransferRecipePacket(display.getDisplayLocation().get(), recipeList, context.isStackedCrafting(), (byte) 0));
                return Result.createSuccessful();
            }
        }

        return Result.createNotApplicable();
    }
}
