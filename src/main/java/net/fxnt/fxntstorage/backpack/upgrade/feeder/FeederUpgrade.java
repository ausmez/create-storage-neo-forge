package net.fxnt.fxntstorage.backpack.upgrade.feeder;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.client.menu.button.GuiIcon;
import net.fxnt.fxntstorage.backpack.client.menu.button.GuiIconSprites;
import net.fxnt.fxntstorage.backpack.client.menu.button.SpriteButton;
import net.fxnt.fxntstorage.backpack.client.menu.slot.FeederFilterSlot;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ClientSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class FeederUpgrade extends AbstractUpgrade {

    public FeederUpgrade() {
        super(UpgradeType.FEEDER);
    }

    @Override
    public List<UpgradeDataSync.Field> getSettings() {
        return List.of(
                UpgradeDataSync.Field.FEEDER_ALLOW_CHORUS_FRUIT,
                UpgradeDataSync.Field.FEEDER_DISPLAY_MESSAGE
        );
    }

    @Override
    public Map<UpgradeDataSync.Field, Boolean> getDefaultSettings() {
        return Map.of(
                UpgradeDataSync.Field.FEEDER_ALLOW_CHORUS_FRUIT, false,
                UpgradeDataSync.Field.FEEDER_DISPLAY_MESSAGE, true
        );
    }

    @Override
    public List<Slot> createSlots(UpgradeContext context) {
        BackpackMenu menu = context.menu();
        return List.of(new FeederFilterSlot(
                menu.container,
                menu.layout.feederFilter().getStartIndex(),
                274, 58,
                () -> menu.hasUpgrade(UpgradeType.FEEDER),
                () -> menu.isPanelExpanded(UpgradeType.FEEDER)
        ));
    }

    @Override
    public int getFilterSlotIndex(BackpackSlotLayout layout) {
        return layout.feederFilter().getStartIndex();
    }

    @Override
    public void onUninstalled(UpgradeContext context) {
        GhostFilterHelper.returnFilterItem(context, getFilterSlotIndex(BackpackSlotLayout.createLayout()));
    }

    @Override
    public boolean filterAccepts(UpgradeContext context, ItemStack stack) {
        return isEdible(stack, context.player()) && !hasNegativeEffects(stack, context.player());
    }

    @Override
    public boolean clicked(UpgradeContext context) {
        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
        if (context.slotId() != getFilterSlotIndex(layout)) return false;

        return GhostFilterHelper.handleClick(context, stack -> filterAccepts(context, stack));
    }

    @Override
    public @Nullable UpgradePanel createPanel(UpgradeContext context) {
        return new FeederPanel(context);
    }

    @Override
    protected void tickActive(UpgradeContext context) {
        if (context.level().getGameTime() % 15 != 0) return;
        if (context.backpack().isEmpty()) return;

        Player player = context.player();
        if (!shouldFeedPlayer(player)) return;

        Level level = context.level();
        IBackpackContainer container = context.container();
        IItemHandlerModifiable itemHandler = container.getItemHandler();
        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
        FilterItemStack filter = FilterItemStack.of(itemHandler.getStackInSlot(layout.feederFilter().getStartIndex()));

        for (int i : layout.items().range()) {
            ItemStack food = itemHandler.getStackInSlot(i);
            if (!filter.isEmpty() && !filter.test(level, food)) continue;
            if (!isEdible(food, player) || hasNegativeEffects(food, player)) continue;

            if (consume(context, itemHandler, layout, i, food)) return;
        }
     }

    private boolean isEdible(@NotNull ItemStack food, LivingEntity player) {
        if (!food.has(DataComponents.FOOD))
            return false;

        FoodProperties foodProperties = food.getItem().getFoodProperties(food, player);
        return foodProperties != null && foodProperties.nutrition() > 0;
    }

    private boolean hasNegativeEffects(@NotNull ItemStack food, LivingEntity player) {
        FoodProperties foodProperties = food.getFoodProperties(player);
        if (foodProperties == null) return false;

        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        UpgradeDataManager manager = UpgradeDataManager.loadFromItem(backpack);

        if (food.is(Items.CHORUS_FRUIT) && !manager.getSetting(UpgradeDataSync.Field.FEEDER_ALLOW_CHORUS_FRUIT, false))
            return true;

        if (food.is(Items.OMINOUS_BOTTLE)) return true;

        SuspiciousStewEffects stewEffects = food.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (stewEffects != null) {
            for (SuspiciousStewEffects.Entry entry : stewEffects.effects()) {
                if (entry.effect().value().getCategory().equals(MobEffectCategory.HARMFUL)) return true;
            }
        }

        // This should capture most foods with negative effects
        for (FoodProperties.PossibleEffect effect : foodProperties.effects()) {
            MobEffectInstance instance = effect.effectSupplier().get();
            if (instance.getEffect().value().getCategory().equals(MobEffectCategory.HARMFUL))
                return true;
        }
        return false;
    }

    private boolean consume(UpgradeContext context, IItemHandlerModifiable itemHandler, BackpackSlotLayout layout, int slot, ItemStack food) {
        Player player = context.player();
        Level level = context.level();
        IBackpackContainer container = context.container();

        String foodName = food.getItem().getName(food).getString();

        // Stash MainHandItem and place food item in Main Hand
        ItemStack mainHandItem = player.getMainHandItem();
        player.getInventory().items.set(player.getInventory().selected, food);

        ItemStack singleItem = food.copyWithCount(1);

        if (singleItem.use(level, player, InteractionHand.MAIN_HAND).getResult() != InteractionResult.CONSUME) {
            player.getInventory().items.set(player.getInventory().selected, mainHandItem);
            return false;
        }

        player.getInventory().items.set(player.getInventory().selected, mainHandItem);
        food.shrink(1);
        itemHandler.setStackInSlot(slot, food);

        ItemStack remainder = EventHooks.onItemUseFinish(player, singleItem.copy(), 0,
                singleItem.getItem().finishUsingItem(singleItem, level, player));
        returnRemainder(context, itemHandler, layout, remainder);

        container.setDataChanged();

        if (container.getUpgradeSetting(UpgradeDataSync.Field.FEEDER_DISPLAY_MESSAGE)) {
            player.displayClientMessage(Component.translatable(
                    "item.fxntstorage.backpack_feeder_upgrade.message", "§a" + foodName + "§r"), true);
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

    private boolean shouldFeedPlayer(Player player) {
        if (player.isCreative() || player.isSpectator()) return false;

        FoodData foodData = player.getFoodData();
        int hunger = foodData.getFoodLevel();
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();

        // Feed immediately
        double healthThreshold = (double) ClientSettings.getInt(player.getUUID(), "FeederHealthThreshold") / 100;
        if (health < maxHealth * healthThreshold && hunger < 20)
            return true;

        return hunger < ClientSettings.getInt(player.getUUID(), "FeederHungerLevel");
    }

    public static class FeederPanel implements UpgradePanel {
        private final List<SpriteButton<FeederState>> stateButtons = new ArrayList<>();

        public record FeederState(boolean allowChorus, boolean displayMessage) {
        }

        private static final GuiIconSprites CHORUS_ON = new GuiIconSprites(GuiIcon.CHORUS_ON);
        private static final GuiIconSprites CHORUS_OFF = new GuiIconSprites(GuiIcon.CHORUS_OFF);
        private static final GuiIconSprites MSG_ON = new GuiIconSprites(GuiIcon.MESSAGE_ON);
        private static final GuiIconSprites MSG_OFF = new GuiIconSprites(GuiIcon.MESSAGE_OFF);

        private static final String TOOLTIP_PREFIX = "tooltip.fxntstorage.backpack_feeder_upgrade.panel.";

        private int panelX;
        private int panelY;

        private final UpgradeContext context;
        private final List<AbstractWidget> widgets = new ArrayList<>();

        public FeederPanel(UpgradeContext context) {
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

        private FeederState getState() {
            BackpackMenu menu = context.menu();
            return new FeederState(
                    menu.isUpgradeSettingEnabled(UpgradeDataSync.Field.FEEDER_ALLOW_CHORUS_FRUIT),
                    menu.isUpgradeSettingEnabled(UpgradeDataSync.Field.FEEDER_DISPLAY_MESSAGE)
            );
        }

        @Override
        public void createWidgets(Consumer<AbstractWidget> widgetAdder) {
            BackpackMenu menu = context.menu();

            FeederState initialState = getState();

            stateButtons.add(
                    new SpriteButton<>(
                            panelX + 22, panelY + 31, 18, 18,
                            initialState,
                            state -> state.allowChorus() ? CHORUS_ON : CHORUS_OFF,
                            state -> state.allowChorus()
                                    ? Component.translatable(TOOLTIP_PREFIX + "allow_chorus").append("\n").append(Component.translatable(TOOLTIP_PREFIX + "allow_chorus.description").withStyle(ChatFormatting.DARK_GRAY))
                                    : Component.translatable(TOOLTIP_PREFIX + "disallow_chorus").append("\n").append(Component.translatable(TOOLTIP_PREFIX + "disallow_chorus.description").withStyle(ChatFormatting.DARK_GRAY)),
                            button -> menu.toggleUpgradeSetting(UpgradeDataSync.Field.FEEDER_ALLOW_CHORUS_FRUIT)
                    )
            );

            stateButtons.add(
                    new SpriteButton<>(
                            panelX + 41, panelY + 31, 18, 18,
                            initialState,
                            state -> state.displayMessage() ? MSG_ON : MSG_OFF,
                            state -> state.displayMessage()
                                    ? Component.translatable(TOOLTIP_PREFIX + "show_message").append("\n").append(Component.translatable(TOOLTIP_PREFIX + "show_message.description").withStyle(ChatFormatting.DARK_GRAY))
                                    : Component.translatable(TOOLTIP_PREFIX + "hide_message").append("\n").append(Component.translatable(TOOLTIP_PREFIX + "hide_message.description").withStyle(ChatFormatting.DARK_GRAY)),
                            button -> menu.toggleUpgradeSetting(UpgradeDataSync.Field.FEEDER_DISPLAY_MESSAGE)
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
            if (hoveredSlot instanceof FeederFilterSlot && !hoveredSlot.hasItem()) {
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
