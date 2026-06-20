package net.fxnt.fxntstorage.reserve_storage;

import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ReserveStorageBoxScrollInput extends ScrollInput {

    private ItemStack item = ItemStack.EMPTY;

    public ReserveStorageBoxScrollInput(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
        updateTooltip();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Reset soundPlayed here so the tick sound plays on every scroll, not just the first.
        soundPlayed = false;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void playDownSound(SoundManager handler) { }

    @Override
    protected void updateTooltip() {
        toolTip.clear();
        if (item.isEmpty()) return;

        String count = state > 1 ? " x " + state : "";
        toolTip.add(Component.literal(item.getHoverName().getString() + count)
                .withStyle(s -> s.withColor(HEADER_RGB.getRGB())));

        toolTip.add(Component.translatable("tooltip.fxntstorage.reserve_storage_scroll_input.line_1")
                .withStyle(ChatFormatting.GRAY));
        toolTip.add(Component.translatable("tooltip.fxntstorage.reserve_storage_scroll_input.line_2")
                .withStyle(ChatFormatting.GRAY));
        toolTip.add(scrollToModify.plainCopy()
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
        toolTip.add(Component.translatable("tooltip.fxntstorage.reserve_storage_scroll_input.line_4")
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
    }
}
