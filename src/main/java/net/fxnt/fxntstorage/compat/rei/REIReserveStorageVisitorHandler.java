package net.fxnt.fxntstorage.compat.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.*;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.fxnt.fxntstorage.network.packet.ReserveStorageBoxGhostPacket;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxGhostSlot;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxMenu;
import net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class REIReserveStorageVisitorHandler implements DraggableStackVisitor<ReserveStorageBoxScreen> {

    @Override
    public DraggedAcceptorResult acceptDraggedStack(DraggingContext<ReserveStorageBoxScreen> context, DraggableStack stack) {
        Point pos = context.getCurrentPosition();
        if (pos == null) return DraggedAcceptorResult.PASS;

        EntryStack<?> entryStack = stack.getStack();
        if (!(entryStack.getValue() instanceof ItemStack itemStack)) return DraggedAcceptorResult.PASS;

        ReserveStorageBoxScreen screen = context.getScreen();
        ReserveStorageBoxMenu menu = screen.getMenu();
        for (Slot slot : menu.slots) {
            if (!(slot instanceof ReserveStorageBoxGhostSlot)) continue;
            int sx = screen.getGuiLeft() + slot.x;
            int sy = screen.getGuiTop() + slot.y;
            if (pos.getX() >= sx && pos.getX() < sx + 16 && pos.getY() >= sy && pos.getY() < sy + 16) {
                ItemStack copy = itemStack.copyWithCount(1);
                if (menu.isGhostDuplicate(slot.getContainerSlot(), copy)) return DraggedAcceptorResult.PASS;
                menu.setGhostItem(slot.getContainerSlot(), copy);
                if (menu.isMounted) {
                    PacketDistributor.sendToServer(ReserveStorageBoxGhostPacket.forMounted(
                            copy, slot.getContainerSlot(), menu.contraptionId, menu.localPos));
                } else {
                    PacketDistributor.sendToServer(ReserveStorageBoxGhostPacket.forBlock(
                            copy, slot.getContainerSlot()));
                }
                screen.onGhostSlotUpdated(slot.getContainerSlot());
                return DraggedAcceptorResult.CONSUMED;
            }
        }
        return DraggedAcceptorResult.PASS;
    }

    @Override
    public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<ReserveStorageBoxScreen> context, DraggableStack stack) {
        if (!(stack.getStack().getValue() instanceof ItemStack)) return Stream.empty();

        List<BoundsProvider> targets = new ArrayList<>();
        ReserveStorageBoxScreen screen = context.getScreen();
        for (Slot slot : screen.getMenu().slots) {
            if (!(slot instanceof ReserveStorageBoxGhostSlot)) continue;
            int sx = screen.getGuiLeft() + slot.x;
            int sy = screen.getGuiTop() + slot.y;
            targets.add(() -> DraggableBoundsProvider.fromRectangle(new Rectangle(sx, sy, 16, 16)));
        }
        return targets.stream();
    }

    @Override
    public <R extends Screen> boolean isHandingScreen(R screen) {
        return screen instanceof ReserveStorageBoxScreen;
    }
}
