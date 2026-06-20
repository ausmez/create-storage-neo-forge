package net.fxnt.fxntstorage.backpack.upgrade.jetpack;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.client.menu.button.SpriteButton;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.network.packet.JetpackStateResetPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class JetpackUpgrade extends AbstractUpgrade {

    public JetpackUpgrade() {
        super(UpgradeType.FLIGHT);
    }

    @Override
    public List<UpgradeDataSync.Field> getSettings() {
        return List.of(
                UpgradeDataSync.Field.JETPACK_BOBBING,
                UpgradeDataSync.Field.JETPACK_OVERLAY
        );
    }

    @Override
    public Map<UpgradeDataSync.Field, Boolean> getDefaultSettings() {
        return Map.of(
                UpgradeDataSync.Field.JETPACK_BOBBING, true,
                UpgradeDataSync.Field.JETPACK_OVERLAY, true
        );
    }

    @Override
    public void onRemoved(UpgradeContext context) {
        Player player = context.player();
        if (player.level().isClientSide) return;

        JetpackHandler handler = JetpackManager.getJetpackHandler(player);
        handler.endHovering(false);
        handler.flyingOnKeyRelease();
        player.setNoGravity(false);

        PacketDistributor.sendToPlayer((ServerPlayer) player, new JetpackStateResetPacket());
    }

    @Override
    public @Nullable UpgradePanel createPanel(UpgradeContext context) {
        return new JetpackPanel(context);
    }

    public static class JetpackPanel implements UpgradePanel {
        private final List<SpriteButton<JetpackState>> stateButtons = new ArrayList<>();

        public record JetpackState(boolean bobbing, boolean overlay) {
        }

        private static final WidgetSprites BOBBING_ON = UpgradePanel.createWidgetSprites("jetpack_bobbing_on");
        private static final WidgetSprites BOBBING_OFF = UpgradePanel.createWidgetSprites("jetpack_bobbing_off");
        private static final WidgetSprites OVERLAY_ON = UpgradePanel.createWidgetSprites("jetpack_overlay_on");
        private static final WidgetSprites OVERLAY_OFF = UpgradePanel.createWidgetSprites("jetpack_overlay_off");

        private int panelX;
        private int panelY;

        private final UpgradeContext context;
        private final List<AbstractWidget> widgets = new ArrayList<>();

        public JetpackPanel(UpgradeContext context) {
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

        private JetpackState getState() {
            BackpackMenu menu = context.menu();
            return new JetpackState(
                    menu.isUpgradeSettingEnabled(UpgradeDataSync.Field.JETPACK_BOBBING),
                    menu.isUpgradeSettingEnabled(UpgradeDataSync.Field.JETPACK_OVERLAY)
            );
        }

        @Override
        public void createWidgets(Consumer<AbstractWidget> widgetAdder) {
            BackpackMenu menu = context.menu();

            JetpackState initialState = getState();

            stateButtons.add(
                    new SpriteButton<>(
                            panelX + 3, panelY + 31, 18, 18,
                            initialState,
                            state -> state.bobbing() ? BOBBING_ON : BOBBING_OFF,
                            state -> state.bobbing()
                                    ? Component.translatable("tooltip.fxntstorage.backpack_flight_upgrade.panel.bobbing_enabled").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_flight_upgrade.panel.bobbing_enabled.description").withStyle(ChatFormatting.DARK_GRAY))
                                    : Component.translatable("tooltip.fxntstorage.backpack_flight_upgrade.panel.bobbing_disabled").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_flight_upgrade.panel.bobbing_disabled.description").withStyle(ChatFormatting.DARK_GRAY)),
                            button -> menu.toggleUpgradeSetting(UpgradeDataSync.Field.JETPACK_BOBBING)

                    )
            );

            stateButtons.add(
                    new SpriteButton<>(
                            panelX + 22, panelY + 31, 18, 18,
                            initialState,
                            state -> state.overlay() ? OVERLAY_ON : OVERLAY_OFF,
                            state -> state.overlay()
                                    ? Component.translatable("tooltip.fxntstorage.backpack_flight_upgrade.panel.show_air_time").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_flight_upgrade.panel.show_air_time.description").withStyle(ChatFormatting.DARK_GRAY))
                                    : Component.translatable("tooltip.fxntstorage.backpack_flight_upgrade.panel.hide_air_time").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_flight_upgrade.panel.hide_air_time.description").withStyle(ChatFormatting.DARK_GRAY)),
                            button -> menu.toggleUpgradeSetting(UpgradeDataSync.Field.JETPACK_OVERLAY)

                    )
            );

            stateButtons.forEach(button -> {
                widgetAdder.accept(button);
                widgets.add(button);
            });
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY) {
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
