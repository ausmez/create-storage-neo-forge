package net.fxnt.fxntstorage.backpack.upgrade.jetpack;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.client.menu.button.GuiIcon;
import net.fxnt.fxntstorage.backpack.client.menu.button.GuiIconSprites;
import net.fxnt.fxntstorage.backpack.client.menu.button.SpriteButton;
import net.fxnt.fxntstorage.backpack.client.menu.slot.JetpackModifierSlot;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.network.packet.JetpackStateResetPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public List<Slot> createSlots(UpgradeContext context) {
        BackpackMenu menu = context.menu();
        return List.of(
                new JetpackModifierSlot(
                        menu.container,
                        menu.layout.jetpackModifier().getStartIndex(),
                        274, 34,
                        () -> menu.hasUpgrade(UpgradeType.FLIGHT),
                        () -> menu.isPanelExpanded(UpgradeType.FLIGHT)
                )
        );
    }

    @Override
    public void onUninstalled(UpgradeContext context) {
        if (context.isClientSide()) return;
        if (!(context.menu() instanceof BackpackMenu menu)) return;

        Slot modifierSlot = menu.slots.get(menu.layout.jetpackModifier().getStartIndex());
        ItemStack modifier = modifierSlot.getItem();
        if (modifier.isEmpty()) return;

        if (!menu.moveStackToPlayerInventory(modifier)) {
            context.player().drop(modifier.copy(), false);
        }

        modifierSlot.set(ItemStack.EMPTY);
        modifierSlot.setChanged();
    }

    @Override
    public Optional<ItemStack> onQuickMove(UpgradeContext context) {
        if (!(context.menu() instanceof BackpackMenu menu)
                || !menu.hasActiveUpgrade(UpgradeType.FLIGHT)
                || !menu.isPanelExpanded(UpgradeType.FLIGHT)
        ) return Optional.empty();

        int modifierSlotIndex = menu.layout.jetpackModifier().getStartIndex();
        int slotIndex = context.slotId();
        ItemStack slotItem = menu.slots.get(slotIndex).getItem();

        if (slotIndex == modifierSlotIndex) {
            if (!menu.moveStackToPlayerInventory(slotItem)) return Optional.empty();
            menu.getSlot(modifierSlotIndex).set(ItemStack.EMPTY);
            return Optional.of(ItemStack.EMPTY);
        }

        // Only pull modifiers in from the player's inventory, so backtanks stored as fuel stay put
        if (slotIndex >= menu.layout.getTotalSlots() && JetpackModifier.isModifier(slotItem)) {
            Slot modifierSlot = menu.getSlot(modifierSlotIndex);
            if (modifierSlot.getItem().isEmpty()) {
                modifierSlot.safeInsert(slotItem.split(1));
                if (slotItem.isEmpty()) menu.getSlot(slotIndex).set(ItemStack.EMPTY);
                menu.getSlot(slotIndex).setChanged();
                return Optional.of(ItemStack.EMPTY);
            }
        }

        return Optional.empty();
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
        private static final String MODIFIER_PREFIX = "tooltip.fxntstorage.backpack_flight_upgrade.panel.modifier.";
        private static final int DISABLED_SLOT_COLOR = 0x80AA0000;
        private final List<SpriteButton<JetpackState>> stateButtons = new ArrayList<>();

        public record JetpackState(boolean bobbing, boolean overlay) {
        }

        private static final GuiIconSprites BOBBING_ON = new GuiIconSprites(GuiIcon.JETPACK_BOBBING_ON);
        private static final GuiIconSprites BOBBING_OFF = new GuiIconSprites(GuiIcon.JETPACK_BOBBING_OFF);
        private static final GuiIconSprites OVERLAY_ON = new GuiIconSprites(GuiIcon.JETPACK_OVERLAY_ON);
        private static final GuiIconSprites OVERLAY_OFF = new GuiIconSprites(GuiIcon.JETPACK_OVERLAY_OFF);

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
                            panelX + 22, panelY + 31, 18, 18,
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
                            panelX + 41, panelY + 31, 18, 18,
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
            // Shade the slot red while modifiers are disabled, like the locked hotbar slot
            if (!JetpackModifier.isEnabled()) {
                graphics.fill(RenderType.guiOverlay(), panelX + 4, panelY + 32, panelX + 20, panelY + 48, DISABLED_SLOT_COLOR);
            }
        }

        @Override
        public void renderTooltip(Font font, GuiGraphics graphics, int mouseX, int mouseY, Slot hoveredSlot) {
            if (!(hoveredSlot instanceof JetpackModifierSlot)) return;

            List<Component> lines = new ArrayList<>();
            if (!JetpackModifier.isEnabled()) {
                Component disabled = Component.translatable(MODIFIER_PREFIX + "disabled").withStyle(ChatFormatting.RED);
                if (hoveredSlot.hasItem()) {
                    // A leftover modifier: shown above the item's own tooltip
                    lines.add(disabled);
                    graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY - 12 - font.lineHeight * lines.size());
                } else {
                    lines.add(Component.translatable(MODIFIER_PREFIX + "slot"));
                    lines.add(disabled);
                    graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
                }
                return;
            }
            if (hoveredSlot.hasItem()) {
                JetpackModifier modifier = JetpackModifier.fromStack(hoveredSlot.getItem());
                if (modifier == JetpackModifier.NONE) return;
                // Shown beside the item's own tooltip, so only the modifier effect is listed
                lines.addAll(TooltipHelper.cutTextComponent(Component.translatable(MODIFIER_PREFIX + "equipped"), FontHelper.Palette.GRAY_AND_WHITE));
                lines.addAll(modifierEffect(modifier));
                graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY - 12 - font.lineHeight * lines.size());
                return;
            }

            lines.add(Component.translatable(MODIFIER_PREFIX + "slot"));
            lines.add(holdShiftLine());
            if (Screen.hasShiftDown()) {
                lines.add(Component.empty());
                for (JetpackModifier modifier : JetpackModifier.values()) {
                    if (modifier == JetpackModifier.NONE) continue;
                    lines.addAll(TooltipHelper.cutTextComponent(modifier.getDisplayStack().getHoverName(), FontHelper.Palette.ALL_GRAY));
                    lines.addAll(modifierEffect(modifier));
                }
                lines.add(Component.empty());
                lines.addAll(TooltipHelper.cutTextComponent(Component.translatable(MODIFIER_PREFIX + "slot.subtext"),
                        FontHelper.Palette.GRAY_AND_GOLD.highlight(), FontHelper.Palette.GRAY_AND_GOLD.highlight()));
            }
            graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
        }

        private static Component holdShiftLine() {
            return Component.translatable("tooltip.fxntstorage.holdForDescription", Screen.hasShiftDown() ? "§fShift" : "§7Shift").withStyle(ChatFormatting.DARK_GRAY);
        }

        // Indented like an upgrade item's behavior line
        private static List<Component> modifierEffect(JetpackModifier modifier) {
            return TooltipHelper.cutTextComponent(Component.translatable(MODIFIER_PREFIX + modifier.getId(), modifier.getTooltipArgs()),
                    FontHelper.Palette.PURPLE.primary(), FontHelper.Palette.PURPLE.highlight(), 1);
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
