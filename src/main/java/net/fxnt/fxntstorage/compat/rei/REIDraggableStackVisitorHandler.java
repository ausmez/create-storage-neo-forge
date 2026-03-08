package net.fxnt.fxntstorage.compat.rei;

import com.simibubi.create.content.logistics.filter.AttributeFilterItem;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.PackageFilterItem;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.*;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackScreen;
import net.fxnt.fxntstorage.backpack.client.menu.slot.FeederFilterSlot;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.network.packet.GhostItemPacket;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class REIDraggableStackVisitorHandler implements DraggableStackVisitor<BackpackScreen> {

    private static final BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
    private final Predicate<ItemStack> createFilter =
            stack -> {

                Item item = stack.getItem();
                return item instanceof FilterItem
                        || item instanceof AttributeFilterItem
                        || item instanceof PackageFilterItem;
            };

    @Override
    public DraggedAcceptorResult acceptDraggedStack(DraggingContext<BackpackScreen> context, DraggableStack stack) {
        Point pos = context.getCurrentPosition();
        if (pos == null) return DraggedAcceptorResult.PASS;

        EntryStack<?> entryStack = stack.getStack();
        if (!(entryStack.getValue() instanceof ItemStack itemStack)
                || createFilter.test(itemStack))
            return DraggedAcceptorResult.PASS;


        BackpackScreen screen = context.getScreen();

        for (int i : layout.getFiltersRange()) {
            Slot slot = screen.getMenu().getSlot(i);
            if (!slot.isActive())
                continue;

            ItemStack existingStack = slot.getItem();
            if (createFilter.test(existingStack))
                continue;

            if (slot instanceof FeederFilterSlot) {
                if (!Util.isEdible(itemStack, context.getScreen().getMenu().player) || Util.hasNegativeEffects(itemStack, context.getScreen().getMenu().player))
                    continue;
            }

            int slotX = screen.getGuiLeft() + slot.x;
            int slotY = screen.getGuiTop() + slot.y;

            if (isWithinBounds(pos.getX(), pos.getY(), slotX, slotY)) {
                PacketDistributor.sendToServer(new GhostItemPacket(itemStack, slot.index));
                return DraggedAcceptorResult.CONSUMED;
            }
        }

        return DraggedAcceptorResult.PASS;
    }

    @Override
    public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<BackpackScreen> context, DraggableStack stack) {
        if (!(stack.getStack().getValue() instanceof ItemStack itemStack)
                || createFilter.test(itemStack))
            return Stream.empty();

        List<BoundsProvider> targets = new ArrayList<>();
        BackpackScreen screen = context.getScreen();

        for (int i : layout.getFiltersRange()) {
            Slot slot = screen.getMenu().getSlot(i);
            if (createFilter.test(slot.getItem()) || !slot.isActive())
                continue;

            if (slot instanceof FeederFilterSlot) {
                if (!Util.isEdible(itemStack, context.getScreen().getMenu().player)
                        || Util.hasNegativeEffects(itemStack, context.getScreen().getMenu().player))
                    continue;
            }

            targets.add(new BackpackBoundsProvider(
                    screen.getGuiLeft() + slot.x,
                    screen.getGuiTop() + slot.y
            ));
        }

        return targets.stream();
    }

    @Override
    public <R extends Screen> boolean isHandingScreen(R screen) {
        return screen instanceof BackpackScreen;
    }

    private static boolean isWithinBounds(int x, int y, int boundsX, int boundsY) {
        return x >= boundsX && x < boundsX + 16
                && y >= boundsY && y < boundsY + 16;
    }

    private static class BackpackBoundsProvider implements BoundsProvider {
        private final int x;
        private final int y;
        private VoxelShape cachedBounds;

        public BackpackBoundsProvider(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public VoxelShape bounds() {
            if (cachedBounds == null) {
                cachedBounds = DraggableBoundsProvider.fromRectangle(
                        new Rectangle(x, y, 16, 16)
                );
            }
            return cachedBounds;
        }
    }
}
