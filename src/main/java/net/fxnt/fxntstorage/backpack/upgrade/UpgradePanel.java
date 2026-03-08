package net.fxnt.fxntstorage.backpack.upgrade;

import net.fxnt.fxntstorage.backpack.client.menu.button.WidgetSprites;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.function.Consumer;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;

public interface UpgradePanel {

    ResourceLocation PANEL_TEXTURE = modLoc("textures/gui/atlas.png");

    int PANEL_EXPANDED_WIDTH = 72;
    int PANEL_EXPANDED_HEIGHT = 49;

    void render(GuiGraphics graphics, int mouseX, int mouseY);

    void renderTooltip(Font font, GuiGraphics graphics, int mouseX, int mouseY, Slot hoveredSlot);

    void createWidgets(Consumer<AbstractWidget> widgetAdder);

    List<AbstractWidget> getWidgets();

    void clearWidgets();

    void setPanelPosition(int leftPos, int imageWidth, int tabY);

    boolean mouseClicked(double mouseX, double mouseY, int button);

    void tick();

    static WidgetSprites createWidgetSprites(String name) {
        return new WidgetSprites(
                modLoc("textures/gui/sprites/" + name + ".png"),
                modLoc("textures/gui/sprites/" + name + "_highlight.png")
        );
    }

    static WidgetSprites createWidgetSprites(String enabled, String disabled, String enabledFocused, String disabledFocus) {
        return new WidgetSprites(
                modLoc("textures/gui/sprites/" + enabled + ".png"),
                modLoc("textures/gui/sprites/" + disabled + ".png"),
                modLoc("textures/gui/sprites/" + enabledFocused + ".png"),
                modLoc("textures/gui/sprites/" + disabledFocus + ".png")
        );
    }
}
