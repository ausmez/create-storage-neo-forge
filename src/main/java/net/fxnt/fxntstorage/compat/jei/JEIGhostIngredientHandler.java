package net.fxnt.fxntstorage.compat.jei;

import com.simibubi.create.content.logistics.filter.FilterItem;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackScreen;
import net.fxnt.fxntstorage.backpack.client.menu.slot.FeederFilterSlot;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.GhostItemPacket;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class JEIGhostIngredientHandler implements IGhostIngredientHandler<BackpackScreen> {
    private final Predicate<ItemStack> createFilter =
            stack -> {
                Item item = stack.getItem();
                return item instanceof FilterItem;
            };

    @Override
    public <I> List<Target<I>> getTargetsTyped(BackpackScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

        ingredient.getItemStack().ifPresent(stack -> {
            for (int i : layout.getFiltersRange()) {
                Slot slot = gui.getMenu().getSlot(i);
                if (createFilter.test(slot.getItem()) || !slot.isActive())
                    continue;

                if (slot instanceof FeederFilterSlot) {
                    if (!Util.isEdible(stack, gui.getMenu().player) || Util.hasNegativeEffects(stack, gui.getMenu().player))
                        continue;
                }

                targets.add(new GhostTarget<>(gui, i));
            }
        });

        return targets;
    }

    @Override
    public void onComplete() {
    }

    private static class GhostTarget<I> implements Target<I> {
        private final Rect2i area;
        private final BackpackScreen gui;
        private final int slotIndex;

        public GhostTarget(BackpackScreen gui, int slotIndex) {
            this.gui = gui;
            this.slotIndex = slotIndex;
            Slot slot = gui.getMenu().slots.get(slotIndex);
            this.area = new Rect2i(gui.getGuiLeft() + slot.x, gui.getGuiTop() + slot.y, 16, 16);
        }

        @Override
        public Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(I ingredient) {
            ItemStack stack = ((ItemStack) ingredient).copyWithCount(1);
            gui.getMenu().container.getItemHandler().setStackInSlot(slotIndex, stack);

            ModNetwork.sendToServer(new GhostItemPacket(stack, slotIndex));
        }
    }
}