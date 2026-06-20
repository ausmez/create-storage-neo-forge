package net.fxnt.fxntstorage.backpack.upgrade;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.function.Consumer;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;

public interface UpgradePanel {

    void render(GuiGraphics graphics, int mouseX, int mouseY);

    void renderTooltip(Font font, GuiGraphics graphics, int mouseX, int mouseY, Slot hoveredSlot);

    void createWidgets(Consumer<AbstractWidget> widgetAdder);

    List<AbstractWidget> getWidgets();

    void clearWidgets();

    void setPanelPosition(int leftPos, int imageWidth, int tabY);

    boolean mouseClicked(double mouseX, double mouseY, int button);

    void tick();

    default int getExpandedWidth() {
        return 68;
    }

    default int getExpandedHeight() {
        return 47;
    }

    default int getTextureU() {
        return 0;
    }

    default int getTextureV() {
        return 0;
    }

    default void layoutSlots(List<Slot> slots, int imageWidth, int relativeTabY) {
        for (Slot slot : slots) {
            slot.y = relativeTabY + 22;
        }
    }

    static WidgetSprites createWidgetSprites(String name) {
        return new WidgetSprites(
                modLoc(name),
                modLoc(name + "_highlight")
        );
    }
}
