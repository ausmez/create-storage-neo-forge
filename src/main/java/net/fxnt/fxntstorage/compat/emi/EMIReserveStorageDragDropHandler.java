package net.fxnt.fxntstorage.compat.emi;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.fxnt.fxntstorage.network.packet.ReserveStorageBoxGhostPacket;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxGhostSlot;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxMenu;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class EMIReserveStorageDragDropHandler implements EmiDragDropHandler<ReserveStorageBoxScreen> {

    @Override
    public boolean dropStack(ReserveStorageBoxScreen screen, EmiIngredient stack, int x, int y) {
        Slot slot = screen.getSlotUnderMouse();
        if (!(slot instanceof ReserveStorageBoxGhostSlot)) return false;

        for (EmiStack emiStack : stack.getEmiStacks()) {
            ItemStack itemStack = emiStack.getItemStack().copyWithCount(1);
            ReserveStorageBoxMenu menu = screen.getMenu();
            if (menu.isGhostDuplicate(slot.getContainerSlot(), itemStack)) return false;
            menu.setGhostItem(slot.getContainerSlot(), itemStack);
            if (menu.isMounted) {
                PacketDistributor.sendToServer(ReserveStorageBoxGhostPacket.forMounted(
                        itemStack, slot.getContainerSlot(), menu.contraptionId, menu.localPos));
            } else {
                PacketDistributor.sendToServer(ReserveStorageBoxGhostPacket.forBlock(
                        itemStack, slot.getContainerSlot()));
            }
            screen.onGhostSlotUpdated(slot.getContainerSlot());
            return true;
        }
        return false;
    }

    @Override
    public void render(ReserveStorageBoxScreen screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta) {
        for (Slot slot : screen.getMenu().slots) {
            if (!(slot instanceof ReserveStorageBoxGhostSlot)) continue;
            draw.fill(
                screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y,
                screen.getGuiLeft() + slot.x + 16, screen.getGuiTop() + slot.y + 16,
                0x8822BB33
            );
        }
    }
}
