package net.fxnt.fxntstorage.compat.jei;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.fxnt.fxntstorage.network.packet.ReserveStorageBoxGhostPacket;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxGhostSlot;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxMenu;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxScreen;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class JEIReserveStorageGhostHandler implements IGhostIngredientHandler<ReserveStorageBoxScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(ReserveStorageBoxScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        ingredient.getItemStack().ifPresent(stack -> {
            for (Slot slot : gui.getMenu().slots) {
                if (slot instanceof ReserveStorageBoxGhostSlot) {
                    targets.add(new GhostTarget<>(gui, slot));
                }
            }
        });
        return targets;
    }

    @Override
    public void onComplete() {}

    private static class GhostTarget<I> implements Target<I> {
        private final Rect2i area;
        private final ReserveStorageBoxScreen gui;
        private final Slot slot;

        GhostTarget(ReserveStorageBoxScreen gui, Slot slot) {
            this.gui = gui;
            this.slot = slot;
            this.area = new Rect2i(gui.getGuiLeft() + slot.x, gui.getGuiTop() + slot.y, 16, 16);
        }

        @Override
        public Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(I ingredient) {
            ItemStack stack = ((ItemStack) ingredient).copyWithCount(1);
            ReserveStorageBoxMenu menu = gui.getMenu();
            if (menu.isGhostDuplicate(slot.getContainerSlot(), stack)) return;
            menu.setGhostItem(slot.getContainerSlot(), stack);
            if (menu.isMounted) {
                PacketDistributor.sendToServer(ReserveStorageBoxGhostPacket.forMounted(
                        stack, slot.getContainerSlot(), menu.contraptionId, menu.localPos));
            } else {
                PacketDistributor.sendToServer(ReserveStorageBoxGhostPacket.forBlock(
                        stack, slot.getContainerSlot()));
            }
            gui.onGhostSlotUpdated(slot.getContainerSlot());
        }
    }
}
