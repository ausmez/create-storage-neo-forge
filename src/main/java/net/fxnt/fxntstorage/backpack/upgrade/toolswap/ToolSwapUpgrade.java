package net.fxnt.fxntstorage.backpack.upgrade.toolswap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllItems;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.client.menu.button.SpriteButton;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ToolSwapUpgrade extends AbstractUpgrade {

    public ToolSwapUpgrade() {
        super(UpgradeType.TOOLSWAP);
    }

    @Override
    public List<UpgradeDataSync.Field> getSettings() {
        return List.of(
                UpgradeDataSync.Field.TOOLSWAP_PREFER_SWORD,
                UpgradeDataSync.Field.TOOLSWAP_PREFER_SILKTOUCH
        );
    }

    @Override
    public Map<UpgradeDataSync.Field, Boolean> getDefaultSettings() {
        return Map.of(
                UpgradeDataSync.Field.TOOLSWAP_PREFER_SWORD, false,
                UpgradeDataSync.Field.TOOLSWAP_PREFER_SILKTOUCH, false
        );
    }

    @Override
    public @Nullable UpgradePanel createPanel(UpgradeContext context) {
        return new ToolSwapPanel(context);
    }

    @Override
    public boolean onAttackEntity(UpgradeContext context, InteractionHand hand, LivingEntity target) {
        Player player = context.player();

        if (hand == InteractionHand.OFF_HAND) return false;
        if (player.isSpectator() || !player.isAlive() || player.isSleeping() || player.isDeadOrDying()) return false;
        if (player.getMainHandItem().is(AllItems.WRENCH)) return false;

        IBackpackContainer container = context.container();
        ToolSwapHandler toolSwapHandler = new ToolSwapHandler(player, container);
        toolSwapHandler.doToolSwap(null, target);

        return false;
    }

    @Override
    public boolean onLeftClickBlock(UpgradeContext context, InteractionHand hand, BlockPos blockPos) {
        Player player = context.player();

        if (hand == InteractionHand.OFF_HAND) return false;
        if (player.isSpectator() || !player.isAlive() || player.isSleeping() || player.isDeadOrDying()) return false;
        if (player.getMainHandItem().is(Tags.Items.TOOLS_WRENCH)) return false;
        if (!player.getMainHandItem().is(Tags.Items.TOOLS)) return false;

        IBackpackContainer container = context.container();
        ToolSwapHandler toolSwapHandler = new ToolSwapHandler(player, container);
        toolSwapHandler.doToolSwap(blockPos, null);

        return false;
    }

    public static class ToolSwapPanel implements UpgradePanel {
        private final List<SpriteButton<ToolSwapState>> stateButtons = new ArrayList<>();

        public record ToolSwapState(boolean preferSword, boolean preferSilkTouch) {
        }

        private static final WidgetSprites SWORD_ON = UpgradePanel.createWidgetSprites("sword_on");
        private static final WidgetSprites SWORD_OFF = UpgradePanel.createWidgetSprites("sword_off");
        private static final WidgetSprites SILK_ON = UpgradePanel.createWidgetSprites("silk_touch_on");
        private static final WidgetSprites SILK_OFF = UpgradePanel.createWidgetSprites("silk_touch_off");

        private int panelX;
        private int panelY;

        private final UpgradeContext context;
        private final List<AbstractWidget> widgets = new ArrayList<>();

        public ToolSwapPanel(UpgradeContext context) {
            this.context = context;
        }

        public void setPanelPosition(int leftPos, int imageWidth, int tabY) {
            int oldY = this.panelY;

            this.panelX = leftPos + imageWidth;
            this.panelY = tabY - 10;

            int deltaY = this.panelY - oldY;

            for (AbstractWidget widget : widgets) {
                widget.setPosition(widget.getX(), widget.getY() + deltaY);
            }
        }

        private ToolSwapState getState() {
            BackpackMenu menu = context.menu();
            return new ToolSwapState(
                    menu.isUpgradeSettingEnabled(UpgradeDataSync.Field.TOOLSWAP_PREFER_SWORD),
                    menu.isUpgradeSettingEnabled(UpgradeDataSync.Field.TOOLSWAP_PREFER_SILKTOUCH)
            );
        }

        @Override
        public void createWidgets(Consumer<AbstractWidget> widgetAdder) {
            BackpackMenu menu = context.menu();

            ToolSwapState initialState = getState();

            stateButtons.add(
                    new SpriteButton<>(
                            panelX + 4, panelY + 32, 18, 18,
                            initialState,
                            state -> state.preferSword() ? SWORD_ON : SWORD_OFF,
                            state -> state.preferSword()
                                    ? Component.translatable("tooltip.fxntstorage.backpack_toolswap_upgrade.panel.prefer_swords").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_toolswap_upgrade.panel.prefer_swords.description").withStyle(ChatFormatting.DARK_GRAY))
                                    : Component.translatable("tooltip.fxntstorage.backpack_toolswap_upgrade.panel.no_weapon_preference").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_toolswap_upgrade.panel.no_weapon_preference.description").withStyle(ChatFormatting.DARK_GRAY)),
                            button -> menu.toggleUpgradeSetting(UpgradeDataSync.Field.TOOLSWAP_PREFER_SWORD)

                    )
            );

            stateButtons.add(
                    new SpriteButton<>(
                            panelX + 26, panelY + 32, 18, 18,
                            initialState,
                            state -> state.preferSilkTouch() ? SILK_ON : SILK_OFF,
                            state -> state.preferSilkTouch()
                                    ? Component.translatable("tooltip.fxntstorage.backpack_toolswap_upgrade.panel.prefer_silk_touch").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_toolswap_upgrade.panel.prefer_silk_touch.description").withStyle(ChatFormatting.DARK_GRAY))
                                    : Component.translatable("tooltip.fxntstorage.backpack_toolswap_upgrade.panel.no_silk_touch_preference").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_toolswap_upgrade.panel.no_silk_touch_preference.description").withStyle(ChatFormatting.DARK_GRAY)),
                            button -> menu.toggleUpgradeSetting(UpgradeDataSync.Field.TOOLSWAP_PREFER_SILKTOUCH)

                    )
            );

            stateButtons.forEach(button -> {
                widgetAdder.accept(button);
                widgets.add(button);
            });
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(
                    PANEL_TEXTURE,
                    panelX - 3, panelY + 8,
                    0, 0,
                    PANEL_EXPANDED_WIDTH,
                    PANEL_EXPANDED_HEIGHT,
                    128, 128
            );
        }

        @Override
        public void renderTooltip(Font font, GuiGraphics graphics, int mouseX, int mouseY, Slot hoveredSlot) {
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false; // Handled by widgets
        }

        @Override
        public void tick() {
            stateButtons.forEach(button -> button.updateState(getState()));
        }

        public List<AbstractWidget> getWidgets() {
            return widgets;
        }

        public void clearWidgets() {
            widgets.clear();
            stateButtons.clear();
        }
    }
}
