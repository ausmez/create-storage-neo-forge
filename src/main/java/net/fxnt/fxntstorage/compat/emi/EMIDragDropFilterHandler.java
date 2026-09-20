package net.fxnt.fxntstorage.compat.emi;

import com.simibubi.create.content.logistics.filter.AttributeFilterItem;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.PackageFilterItem;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackScreen;
import net.fxnt.fxntstorage.backpack.client.menu.slot.FilterSlot;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.upgrade.GhostFilterHelper;
import net.fxnt.fxntstorage.network.packet.GhostItemPacket;
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

        for (EmiStack emiStack : stack.getEmiStacks()) {
            ItemStack dropped = emiStack.getItemStack();

            if (!GhostFilterHelper.accepts(screen.getMenu(), slot.index, dropped)) return false;

            screen.getMenu().container.getItemHandler().setStackInSlot(slot.index, dropped);
            PacketDistributor.sendToServer(new GhostItemPacket(dropped, slot.index));
            return true;
        }

        return false;
    }

    @Override
    public void render(BackpackScreen screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta) {
        boolean isCreateFilter = dragged.getEmiStacks().stream().anyMatch(createFilter);
        if (isCreateFilter) return;
        if (dragged.getEmiStacks().isEmpty()) return;

        ItemStack draggedStack = dragged.getEmiStacks().getFirst().getItemStack();

        for (int i : layout.getFilterSlotIndices()) {
            Slot slot = screen.getMenu().getSlot(i);
            EmiStack slotItem = EmiStack.of(slot.getItem());
            if (createFilter.test(slotItem) || !slot.isActive()) continue;

            if (!GhostFilterHelper.accepts(screen.getMenu(), i, draggedStack)) continue;

            draw.fill(screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y, screen.getGuiLeft() + slot.x + 16, screen.getGuiTop() + slot.y + 16, 0x8822bb33);
        }
    }
}
