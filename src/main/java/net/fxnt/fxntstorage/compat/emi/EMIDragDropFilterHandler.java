package net.fxnt.fxntstorage.compat.emi;

import com.simibubi.create.content.logistics.filter.AttributeFilterItem;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.PackageFilterItem;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackScreen;
import net.fxnt.fxntstorage.backpack.client.menu.slot.FeederFilterSlot;
import net.fxnt.fxntstorage.backpack.client.menu.slot.FilterSlot;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.network.packet.GhostItemPacket;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Predicate;

public class EMIDragDropFilterHandler implements EmiDragDropHandler<BackpackScreen> {
    private final BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
    private final Predicate<? super EmiStack> createFilter =
            stack -> {
                Item item = stack.getItemStack().getItem();
                return item instanceof FilterItem
                        || item instanceof AttributeFilterItem
                        || item instanceof PackageFilterItem;
            };

    @Override
    public boolean dropStack(BackpackScreen screen, EmiIngredient stack, int x, int y) {
        boolean isCreateFilter = stack.getEmiStacks().stream().anyMatch(createFilter);
        if (isCreateFilter) return false;

        Slot slot = screen.getSlotUnderMouse();
        if (slot == null) return false;
        if (!(slot instanceof FilterSlot)) return false;

        EmiStack slotItem = EmiStack.of(slot.getItem());
        if (createFilter.test(slotItem)) return false;

        if (slot instanceof FeederFilterSlot) {
            if (stack.getEmiStacks().isEmpty()) return false;
            ItemStack food = stack.getEmiStacks().getFirst().getItemStack();
            if (!Util.isEdible(food, screen.getMenu().player) || Util.hasNegativeEffects(food, screen.getMenu().player))
                return false;
        }

        for (EmiStack emiStack : stack.getEmiStacks()) {
            screen.getMenu().container.getItemHandler().setStackInSlot(slot.index, emiStack.getItemStack());
            PacketDistributor.sendToServer(new GhostItemPacket(emiStack.getItemStack(), slot.index));
            return true;
        }

        return false;
    }

    @Override
    public void render(BackpackScreen screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta) {
        boolean isCreateFilter = dragged.getEmiStacks().stream().anyMatch(createFilter);
        if (isCreateFilter) return;

        for (int i : layout.getFiltersRange()) {
            Slot slot = screen.getMenu().getSlot(i);
            EmiStack slotItem = EmiStack.of(slot.getItem());
            if (createFilter.test(slotItem) || !slot.isActive()) continue;

            if (slot instanceof FeederFilterSlot
                    && (!Util.isEdible(dragged.getEmiStacks().getFirst().getItemStack(), screen.getMenu().player) || Util.hasNegativeEffects(dragged.getEmiStacks().getFirst().getItemStack(), screen.getMenu().player)))
                continue;

            draw.fill(screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y, screen.getGuiLeft() + slot.x + 16, screen.getGuiTop() + slot.y + 16, 0x8822bb33);
        }
    }
}
