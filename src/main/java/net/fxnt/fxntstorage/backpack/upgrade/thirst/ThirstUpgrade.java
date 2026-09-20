package net.fxnt.fxntstorage.backpack.upgrade.thirst;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.client.menu.button.GuiIcon;
import net.fxnt.fxntstorage.backpack.client.menu.button.GuiIconSprites;
import net.fxnt.fxntstorage.backpack.client.menu.button.PurityButton;
import net.fxnt.fxntstorage.backpack.client.menu.button.SpriteButton;
import net.fxnt.fxntstorage.backpack.client.menu.slot.ThirstFilterSlot;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.compat.thirst.ThirstCompat;
import net.fxnt.fxntstorage.config.ClientSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ThirstUpgrade extends AbstractUpgrade {

    public ThirstUpgrade() {
        super(UpgradeType.THIRST);
    }

    @Override
    public List<UpgradeDataSync.Field> getSettings() {
        return List.of(UpgradeDataSync.Field.THIRST_DISPLAY_MESSAGE);
    }

    @Override
    public Map<UpgradeDataSync.Field, Boolean> getDefaultSettings() {
        return Map.of(UpgradeDataSync.Field.THIRST_DISPLAY_MESSAGE, true);
    }

    @Override
    public List<UpgradeDataSync.Field> getIntSettings() {
        return List.of(UpgradeDataSync.Field.THIRST_MIN_PURITY);
    }

    @Override
    public Map<UpgradeDataSync.Field, Integer> getDefaultIntSettings() {
        return Map.of(UpgradeDataSync.Field.THIRST_MIN_PURITY, 0); // 0 = accept any purity
    }

    @Override
    public List<Slot> createSlots(UpgradeContext context) {
        BackpackMenu menu = context.menu();
        return List.of(new ThirstFilterSlot(
                menu.container,
                menu.layout.thirstFilter().getStartIndex(),
                274, 58,
                () -> menu.hasUpgrade(UpgradeType.THIRST),
                () -> menu.isPanelExpanded(UpgradeType.THIRST)
        ));
    }

    @Override
    public int getFilterSlotIndex(BackpackSlotLayout layout) {
        return layout.thirstFilter().getStartIndex();
    }

    @Override
    public void onUninstalled(UpgradeContext context) {
        GhostFilterHelper.returnFilterItem(context, getFilterSlotIndex(BackpackSlotLayout.createLayout()));
    }

    @Override
    public boolean filterAccepts(UpgradeContext context, ItemStack stack) {
        return FXNTStorage.THIRST_LOADED && ThirstCompat.isDrinkable(stack, context.player());
    }

    @Override
    public boolean clicked(UpgradeContext context) {
        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
        if (context.slotId() != getFilterSlotIndex(layout)) return false;

        return GhostFilterHelper.handleClick(context, stack -> filterAccepts(context, stack));
    }

    @Override
    public @Nullable UpgradePanel createPanel(UpgradeContext context) {
        if (!FXNTStorage.THIRST_LOADED) return null;
        return new ThirstPanel(context);
    }

    @Override
    protected void tickActive(UpgradeContext context) {
        if (!FXNTStorage.THIRST_LOADED) return;
        if (context.level().getGameTime() % 15 != 0) return;
        if (context.backpack().isEmpty()) return;

        Player player = context.player();
        if (!shouldDrink(player)) return;

        Level level = context.level();
        IBackpackContainer container = context.container();
        IItemHandlerModifiable itemHandler = container.getItemHandler();
        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

        FilterItemStack filter = FilterItemStack.of(itemHandler.getStackInSlot(layout.thirstFilter().getStartIndex()));
        int minPurity = container.getUpgradeIntSetting(UpgradeDataSync.Field.THIRST_MIN_PURITY);

        for (int i : layout.items().range()) {
            ItemStack drink = itemHandler.getStackInSlot(i);
            if (!filter.isEmpty() && !filter.test(level, drink)) continue;
            if (!ThirstCompat.isDrinkable(drink, player)) continue;
            if (ThirstCompat.isPurityEnabled() && ThirstCompat.getPurity(drink) < minPurity) continue;

            if (consume(context, itemHandler, layout, i, drink)) return;
        }
    }

    private boolean consume(UpgradeContext context, IItemHandlerModifiable itemHandler, BackpackSlotLayout layout, int slot, ItemStack drink) {
        Player player = context.player();
        Level level = context.level();
        IBackpackContainer container = context.container();

        String drinkName = drink.getItem().getName(drink).getString();

        // Stash the main hand item and hold the drink instead, so use()/finishUsingItem() see it
        ItemStack mainHandItem = player.getMainHandItem();
        player.getInventory().items.set(player.getInventory().selected, drink);

        ItemStack singleItem = drink.copyWithCount(1);

        if (singleItem.use(level, player, InteractionHand.MAIN_HAND).getResult() != InteractionResult.CONSUME) {
            // Not consumed for some reason, put the held item back and leave the stack alone
            player.getInventory().items.set(player.getInventory().selected, mainHandItem);
            return false;
        }

        player.getInventory().items.set(player.getInventory().selected, mainHandItem);
        drink.shrink(1);
        itemHandler.setStackInSlot(slot, drink);

        if (drink.getUseAnimation() == UseAnim.DRINK)
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    drink.getDrinkingSound(), SoundSource.NEUTRAL,
                    0.5F, 1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.4F);

        ItemStack remainder = EventHooks.onItemUseFinish(player, singleItem.copy(), 0,
                singleItem.getItem().finishUsingItem(singleItem, level, player));
        returnRemainder(context, itemHandler, layout, remainder);

        container.setDataChanged();

        if (container.getUpgradeSetting(UpgradeDataSync.Field.THIRST_DISPLAY_MESSAGE)) {
            player.displayClientMessage(Component.translatable(
                    "item.fxntstorage.backpack_thirst_upgrade.message", "§b" + drinkName + "§r"), true);
        }
        return true;
    }

    private void returnRemainder(UpgradeContext context, IItemHandlerModifiable itemHandler, BackpackSlotLayout layout, ItemStack remainder) {
        if (remainder.isEmpty()) return;

        IBackpackContainer container = context.container();
        Player player = context.player();
        int firstEmptyStack = -1;

        for (int j : layout.items().range()) {
            ItemStack stack = itemHandler.getStackInSlot(j);

            if (stack.isEmpty() && firstEmptyStack < 0) {
                firstEmptyStack = j;
            }
            if (ItemStack.isSameItemSameComponents(stack, remainder)
                    && stack.getCount() < container.getStackMultiplier() * remainder.getMaxStackSize()) {
                ItemStack insertResult = itemHandler.insertItem(j, remainder, false);
                if (!insertResult.isEmpty()) {
                    player.drop(insertResult, true);
                }
                return;
            }
        }

        if (firstEmptyStack > -1) {
            itemHandler.insertItem(firstEmptyStack, remainder, false);
        } else {
            player.drop(remainder, true);
        }
    }

    private boolean shouldDrink(Player player) {
        if (player.isCreative() || player.isSpectator()) return false;
        return ThirstCompat.isThirstBelow(player, ClientSettings.getInt(player.getUUID(), "ThirstLevel"));
    }

    public static class ThirstPanel implements UpgradePanel {
        private static final GuiIconSprites MSG_ON = new GuiIconSprites(GuiIcon.MESSAGE_ON);
        private static final GuiIconSprites MSG_OFF = new GuiIconSprites(GuiIcon.MESSAGE_OFF);

        private static final String TOOLTIP_PREFIX = "tooltip.fxntstorage.backpack_thirst_upgrade.panel.";

        private final UpgradeContext context;
        private final List<AbstractWidget> widgets = new ArrayList<>();
        private final List<SpriteButton<Boolean>> stateButtons = new ArrayList<>();

        private PurityButton purityButton;

        private int panelX;
        private int panelY;

        public ThirstPanel(UpgradeContext context) {
            this.context = context;
        }

        @Override
        public void setPanelPosition(int leftPos, int imageWidth, int tabY) {
            int oldY = this.panelY;

            this.panelX = leftPos + imageWidth;
            this.panelY = tabY - 10;

            int deltaY = this.panelY - oldY;

            for (AbstractWidget widget : widgets) {
                widget.setPosition(widget.getX(), widget.getY() + deltaY);
            }
        }

        private boolean displayMessage() {
            return context.<BackpackMenu>menu().isUpgradeSettingEnabled(UpgradeDataSync.Field.THIRST_DISPLAY_MESSAGE);
        }

        private int minPurity() {
            return context.<BackpackMenu>menu().getUpgradeSyncValue(UpgradeDataSync.Field.THIRST_MIN_PURITY);
        }

        private static Component describe(String key) {
            return Component.translatable(TOOLTIP_PREFIX + key)
                    .append("\n")
                    .append(Component.translatable(TOOLTIP_PREFIX + key + ".description").withStyle(ChatFormatting.DARK_GRAY));
        }

        private static Component purityTooltip(int purity) {
            if (!ThirstCompat.isPurityEnabled()) {
                return Component.translatable(TOOLTIP_PREFIX + "min_purity",
                                Component.translatable(TOOLTIP_PREFIX + "min_purity.disabled").withStyle(ChatFormatting.DARK_RED)).copy()
                        .append("\n")
                        .append(Component.translatable(TOOLTIP_PREFIX + "min_purity.disabled.description")
                                .withStyle(ChatFormatting.DARK_GRAY));
            }

            Component title = purity <= ThirstCompat.MIN_PURITY
                    ? Component.translatable(TOOLTIP_PREFIX + "min_purity", Component.translatable(TOOLTIP_PREFIX + "min_purity.any"))
                    : Component.translatable(TOOLTIP_PREFIX + "min_purity",
                    Component.literal(ThirstCompat.getPurityText(purity))
                            .withStyle(style -> style.withColor(ThirstCompat.getPurityColor(purity))));
            return title.copy()
                    .append("\n")
                    .append(Component.translatable(TOOLTIP_PREFIX + "min_purity.description").withStyle(ChatFormatting.DARK_GRAY));
        }

        @Override
        public void createWidgets(Consumer<AbstractWidget> widgetAdder) {
            BackpackMenu menu = context.menu();

            purityButton = new PurityButton(
                    panelX + 22, panelY + 31, 18, 18,
                    minPurity(),
                    ThirstPanel::purityTooltip,
                    button -> menu.cycleUpgradeIntSetting(UpgradeDataSync.Field.THIRST_MIN_PURITY,
                            ThirstCompat.MIN_PURITY, ThirstCompat.MAX_PURITY)
            );

            stateButtons.add(new SpriteButton<>(
                    panelX + 41, panelY + 31, 18, 18,
                    displayMessage(),
                    on -> on ? MSG_ON : MSG_OFF,
                    on -> describe(on ? "show_message" : "hide_message"),
                    button -> menu.toggleUpgradeSetting(UpgradeDataSync.Field.THIRST_DISPLAY_MESSAGE)
            ));

            widgetAdder.accept(purityButton);
            widgets.add(purityButton);

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
            if (hoveredSlot instanceof ThirstFilterSlot && !hoveredSlot.hasItem()) {
                graphics.renderTooltip(
                        font,
                        List.of(
                                Component.translatable(TOOLTIP_PREFIX + "filter_slot"),
                                Component.translatable(TOOLTIP_PREFIX + "filter_slot.description")
                                        .withStyle(ChatFormatting.DARK_GRAY)
                        ),
                        Optional.empty(),
                        mouseX,
                        mouseY
                );
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false; // Handled by widgets
        }

        @Override
        public void tick() {
            stateButtons.forEach(button -> button.updateState(displayMessage()));
            if (purityButton != null) purityButton.updateState(minPurity());
        }

        @Override
        public List<AbstractWidget> getWidgets() {
            return widgets;
        }

        @Override
        public void clearWidgets() {
            widgets.clear();
            stateButtons.clear();
            purityButton = null;
        }
    }
}
