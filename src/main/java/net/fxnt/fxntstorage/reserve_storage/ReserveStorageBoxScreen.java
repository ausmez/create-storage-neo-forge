package net.fxnt.fxntstorage.reserve_storage;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.network.packet.ReserveStorageBoxGhostPacket;
import net.fxnt.fxntstorage.util.SortOrder;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxEntity.GHOST_SLOTS;
import static net.fxnt.fxntstorage.reserve_storage.ReserveStorageBoxEntity.STORAGE_SLOTS;

public class ReserveStorageBoxScreen extends AbstractContainerScreen<ReserveStorageBoxMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            FXNTStorage.MOD_ID, "textures/gui/container/reserve_storage_box_screen.png");

    protected static final int GUI_WIDTH = 176;
    protected static final int GUI_HEIGHT = 211;
    protected static final int SLOT_START_X = 8;
    protected static final int ROW1_Y = 22;
    protected static final int ROWS_START_Y = 63;
    protected static final int INV_START_Y = 129;
    protected static final int HOTBAR_Y = 187;

    private static final Component RESERVED_ITEMS = Component.translatable("container.fxntstorage.reserve_storage_box.reserved_items");
    private static final int RESERVE_MET_COLOR = 0x66000000 + DyeColor.GREEN.getTextureDiffuseColor();
    private static final int RESERVE_UNMET_COLOR = 0x66000000 + DyeColor.YELLOW.getTextureDiffuseColor();
    private static final int RESERVE_MISSING_COLOR = 0x66000000 + DyeColor.RED.getTextureDiffuseColor();

    private final List<ReserveStorageBoxScrollInput> ghostScrollInputs = new ArrayList<>();

    public ReserveStorageBoxScreen(ReserveStorageBoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        initSlots();
        initScrollInputs();
        initSortButton();
    }

    private void initSortButton() {
        Button sortButton = Button.builder(menu.getSortOrder().getDisplayName(), button -> {
                    SortOrder next = menu.getSortOrder().next();
                    menu.setSortOrder(next);
                    button.setMessage(next.getDisplayName());
                    button.setTooltip(createSortTooltip(next));
                })
                .tooltip(createSortTooltip(menu.getSortOrder()))
                .size(16, 12)
                .pos(leftPos + imageWidth - 24, topPos + 49)
                .build();
        addRenderableWidget(sortButton);
    }

    private Tooltip createSortTooltip(SortOrder order) {
        return Tooltip.create(
                Component.translatable("tooltip.fxntstorage.sortBy")
                        .append(Component.literal(" "))
                        .append(order.name().toUpperCase(Locale.ROOT))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("tooltip.fxntstorage.sortBy.text").withStyle(ChatFormatting.DARK_GRAY))
        );
    }

    protected void initSlots() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                Slot slot = menu.getSlot(row * 9 + col);
                slot.x = SLOT_START_X + col * Util.SLOT_SIZE;
                slot.y = ROWS_START_Y + row * Util.SLOT_SIZE;
            }
        }
        for (int col = 0; col < GHOST_SLOTS; col++) {
            Slot slot = menu.getSlot(STORAGE_SLOTS + col);
            slot.x = SLOT_START_X + col * Util.SLOT_SIZE;
            slot.y = ROW1_Y;
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                Slot slot = menu.getSlot(ReserveStorageBoxMenu.TOTAL_CONTAINER_SLOTS + row * 9 + col);
                slot.x = SLOT_START_X + col * Util.SLOT_SIZE;
                slot.y = INV_START_Y + row * Util.SLOT_SIZE;
            }
        }
        for (int col = 0; col < 9; col++) {
            Slot slot = menu.getSlot(ReserveStorageBoxMenu.TOTAL_CONTAINER_SLOTS + 27 + col);
            slot.x = SLOT_START_X + col * Util.SLOT_SIZE;
            slot.y = HOTBAR_Y;
        }
    }

    private void initScrollInputs() {
        ghostScrollInputs.clear();
        for (int col = 0; col < GHOST_SLOTS; col++) {
            final int slotIndex = STORAGE_SLOTS + col;
            final int listIndex = col;
            final Slot ghostSlot = menu.getSlot(slotIndex);

            ReserveStorageBoxScrollInput input = new ReserveStorageBoxScrollInput(
                    leftPos + ghostSlot.x, topPos + ghostSlot.y, 16, 16);
            input.withRange(1, STORAGE_SLOTS * 64 + 1)
                    .withStepFunction(ctx -> {
                        ItemStack item = ghostSlot.getItem();
                        if (ctx.control && !item.isEmpty()) {
                            int max = item.getMaxStackSize();
                            int cur = ctx.currentValue;
                            if (ctx.forward) {
                                int next = ((cur + max - 1) / max) * max;
                                if (next == cur) next += max;
                                return next - cur;
                            } else {
                                return cur - ((cur - 1) / max) * max;
                            }
                        }
                        return ctx.shift ? 10 : 1;
                    })
                    .calling(count -> {
                        ItemStack current = ghostSlot.getItem();
                        if (current.isEmpty()) return;
                        int cap = STORAGE_SLOTS * current.getMaxStackSize();
                        int clamped = Math.min(count, cap);
                        if (clamped != count) ghostScrollInputs.get(listIndex).setState(clamped);
                        ItemStack updated = current.copyWithCount(clamped);
                        ghostSlot.set(updated);
                        sendGhostPacket(updated, slotIndex);
                    });

            syncScrollInput(input, ghostSlot);
            ghostScrollInputs.add(input);
            addRenderableWidget(input);
        }
    }

    private void syncScrollInput(ReserveStorageBoxScrollInput input, Slot ghostSlot) {
        ItemStack item = ghostSlot.getItem();
        if (item.isEmpty()) {
            input.active = false;
            input.setState(1);
            input.setItem(ItemStack.EMPTY);
        } else {
            input.active = true;
            input.setState(item.getCount());
            input.setItem(item);
        }
    }

    public void onGhostSlotUpdated(int containerSlot) {
        int col = containerSlot - STORAGE_SLOTS;
        if (col < 0 || col >= ghostScrollInputs.size()) return;
        ReserveStorageBoxScrollInput input = ghostScrollInputs.get(col);
        if (input != null) syncScrollInput(input, menu.getSlot(containerSlot));
    }

    protected boolean isGhostSlot(Slot slot) {
        return slot instanceof ReserveStorageBoxGhostSlot;
    }

    protected void sendGhostPacket(ItemStack item, int containerSlot) {
        if (menu.isMounted) {
            PacketDistributor.sendToServer(
                    ReserveStorageBoxGhostPacket.forMounted(item, containerSlot, menu.contraptionId, menu.localPos));
        } else {
            PacketDistributor.sendToServer(
                    ReserveStorageBoxGhostPacket.forBlock(item, containerSlot));
        }
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType type) {
        if (isGhostSlot(slot)) {
            ItemStack carried = getMenu().getCarried();
            ItemStack toSet = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
            if (menu.isGhostDuplicate(slot.getContainerSlot(), toSet)) return;
            slot.set(toSet);
            sendGhostPacket(toSet, slot.getContainerSlot());
            onGhostSlotUpdated(slot.getContainerSlot());
            return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isGhostSlot(hoveredSlot)) return;
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(GUI_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int cx = GUI_WIDTH / 2 - font.width(RESERVED_ITEMS.getString()) / 2;
        guiGraphics.drawString(font, RESERVED_ITEMS, cx, 4, 0x202F20, false);
        guiGraphics.drawString(font, title, SLOT_START_X, ROWS_START_Y - 12, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, SLOT_START_X, INV_START_Y - 11, 0x404040, false);
    }

    @Override
    protected void renderSlotContents(GuiGraphics guiGraphics, ItemStack itemstack, Slot slot, @Nullable String countString) {
        if (isGhostSlot(slot) && !itemstack.isEmpty()) {
            guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, reserveStatusColor(itemstack));
            guiGraphics.renderFakeItem(itemstack, slot.x, slot.y);
            renderGhostCount(guiGraphics, itemstack.getCount(), slot.x, slot.y);
            return;
        }
        super.renderSlotContents(guiGraphics, itemstack, slot, countString);
    }

    private int reserveStatusColor(ItemStack ghostItem) {
        int count = 0;
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            ItemStack stored = menu.getSlot(i).getItem();
            if (ItemStack.isSameItemSameComponents(stored, ghostItem)) count += stored.getCount();
        }
        return count == 0 ? RESERVE_MISSING_COLOR : count >= ghostItem.getCount() ? RESERVE_MET_COLOR : RESERVE_UNMET_COLOR;
    }

    protected void renderGhostCount(GuiGraphics guiGraphics, int count, int x, int y) {
        if (count <= 1) return;

        String text = String.valueOf(count);
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 200);
        float scale = Math.min(1.0f, 16.0f / font.width(text));
        if (scale < 1.0f) pose.scale(scale, scale, 1);
        float tx = (x + 17 - font.width(text) * scale) / scale;
        float ty = (y + 9 + (1 / (scale * scale) - 1)) / scale;
        guiGraphics.drawString(font, text, (int) tx, (int) ty, 0xFFFFFF, true);
        pose.popPose();
    }
}
