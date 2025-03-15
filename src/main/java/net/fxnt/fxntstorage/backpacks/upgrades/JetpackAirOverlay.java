package net.fxnt.fxntstorage.backpacks.upgrades;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Components;
import net.fxnt.fxntstorage.backpacks.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Objects;

public class JetpackAirOverlay implements IGuiOverlay {
    public static final JetpackAirOverlay INSTANCE = new JetpackAirOverlay();

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || Objects.requireNonNull(mc.gameMode).getPlayerMode() == GameType.SPECTATOR)
            return;
        if (!ConfigManager.ClientConfig.DISPLAY_JETPACK_AIR_OVERLAY.get())
            return;

        LocalPlayer player = mc.player;
        if (player == null)
            return;
        if (player.isCreative())
            return;
        if (!player.getPersistentData().contains("VisualJetpackAir"))
            return;

        int timeLeft = player.getPersistentData().getInt("VisualJetpackAir");

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        ItemStack backtank = BackpackHelper.getEquippedBackpackStack(player);
        poseStack.translate((double) width / 2 + 93.2, height - 29, 0);

        float scale = 0.60f;
        poseStack.scale(scale, scale, scale);

        GuiGameElement.of(backtank)
                .at(0, 0)
                .render(graphics);

        // Reset position
        poseStack.popPose();
        poseStack.pushPose();

        poseStack.translate((float) width / 2 + 90, height - 33, 0);
        Component text = Components.literal(StringUtil.formatTickDuration(Math.max(0, timeLeft - 1) * 20));

        int color = 0xFF_FFFFFF;
        if (timeLeft < 60 && timeLeft % 2 == 0) {
            color = Color.mixColors(0xFF_FF0000, color, Math.max(timeLeft / 60f, .25f));
        }
        graphics.drawString(mc.font, text, 16, 5, color);

        poseStack.popPose();
    }

}
