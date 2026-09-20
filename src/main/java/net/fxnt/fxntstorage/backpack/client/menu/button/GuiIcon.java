package net.fxnt.fxntstorage.backpack.client.menu.button;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GuiIcon(int col, int row) {

    public static final ResourceLocation SHEET = FXNTStorage.modLoc("textures/gui/widgets.png");
    public static final int SHEET_SIZE = 128;
    public static final int ICON_SIZE = 18;

    public static final GuiIcon HIGHLIGHT = new GuiIcon(0, 0);
    public static final GuiIcon BACKGROUND = new GuiIcon(1, 0);

    public static final GuiIcon CHECK = new GuiIcon(0, 1);
    public static final GuiIcon CROSS = new GuiIcon(1, 1);
    public static final GuiIcon TILDE = new GuiIcon(2, 1);
    public static final GuiIcon ASTERISK = new GuiIcon(3, 1);
    public static final GuiIcon QUESTION_MARK = new GuiIcon(4, 1);

    public static final GuiIcon CHORUS_ON = new GuiIcon(0, 2);
    public static final GuiIcon CHORUS_OFF = new GuiIcon(1, 2);
    public static final GuiIcon MESSAGE_ON = new GuiIcon(2, 2);
    public static final GuiIcon MESSAGE_OFF = new GuiIcon(3, 2);

    public static final GuiIcon PURITY_3 = new GuiIcon(4, 2);
    public static final GuiIcon PURITY_2 = new GuiIcon(5, 2);
    public static final GuiIcon PURITY_1 = new GuiIcon(6, 2);
    public static final GuiIcon PURITY_0 = new GuiIcon(6, 3);

    public static final GuiIcon JETPACK_BOBBING_ON = new GuiIcon(0, 3);
    public static final GuiIcon JETPACK_BOBBING_OFF = new GuiIcon(1, 3);
    public static final GuiIcon JETPACK_OVERLAY_ON = new GuiIcon(2, 3);
    public static final GuiIcon JETPACK_OVERLAY_OFF = new GuiIcon(3, 3);

    public static final GuiIcon VOID_GUI_DENY = new GuiIcon(4, 3);
    public static final GuiIcon VOID_GUI_ALLOW = new GuiIcon(5, 3);

    public static final GuiIcon SILK_TOUCH_ON = new GuiIcon(0, 4);
    public static final GuiIcon SILK_TOUCH_OFF = new GuiIcon(1, 4);
    public static final GuiIcon SWORD_ON = new GuiIcon(2, 4);
    public static final GuiIcon SWORD_OFF = new GuiIcon(3, 4);

    public static final GuiIcon VOID_ALWAYS = new GuiIcon(4, 4);
    public static final GuiIcon VOID_SLOT = new GuiIcon(5, 4);
    public static final GuiIcon VOID_STORAGE = new GuiIcon(6, 4);

    public static final GuiIcon PLAY = new GuiIcon(0, 5);
    public static final GuiIcon PLAY_DISABLED = new GuiIcon(1, 5);
    public static final GuiIcon STOP = new GuiIcon(2, 5);
    public static final GuiIcon UNMUTED = new GuiIcon(3, 5);
    public static final GuiIcon UNMUTED_DISABLED = new GuiIcon(4, 5);
    public static final GuiIcon MUTED = new GuiIcon(5, 5);

    public int u() {
        return col * ICON_SIZE;
    }

    public int v() {
        return row * ICON_SIZE;
    }

    public void render(GuiGraphics graphics, int x, int y) {
        render(graphics, x, y, ICON_SIZE, ICON_SIZE);
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blit(SHEET, x, y, width, height, u(), v(), ICON_SIZE, ICON_SIZE, SHEET_SIZE, SHEET_SIZE);
    }
}
