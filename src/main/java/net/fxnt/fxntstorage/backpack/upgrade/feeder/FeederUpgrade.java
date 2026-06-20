package net.fxnt.fxntstorage.backpack.upgrade.feeder;

import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.filter.PackageFilterItem;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.client.menu.button.SpriteButton;
import net.fxnt.fxntstorage.backpack.client.menu.slot.FeederFilterSlot;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.config.ClientSettings;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.fxnt.fxntstorage.util.Util.hasNegativeEffects;

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
    public boolean clicked(UpgradeContext context) {
        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

        if (layout.feederFilter().contains(context.slotId())) {
            Slot slot = context.player().containerMenu.slots.get(context.slotId());
            ItemStack carried = context.player().containerMenu.getCarried();
            ItemStack existing = slot.getItem();

            Predicate<ItemStack> isCreateFilter = stack -> stack.getItem() instanceof FilterItem;
            Predicate<ItemStack> isAttributeListFilter = stack -> stack.getItem() instanceof FilterItem && !(stack.getItem() instanceof PackageFilterItem);
            Predicate<ItemStack> isGoodFood = stack -> Util.isEdible(stack, context.player()) && !Util.hasNegativeEffects(stack, context.player());

            boolean requiresFood = layout.feederFilter().contains(context.slotId());
            boolean carriedIsFilter = !carried.isEmpty() && isCreateFilter.test(carried);
            boolean existingIsFilter = !existing.isEmpty() && isCreateFilter.test(existing);

            // RIGHT CLICK: clear slot
            if (context.button() == 1 && !existingIsFilter) {
                slot.set(ItemStack.EMPTY);
                context.container().setDataChanged();
                return true;
            }

            // PICK UP existing filter
            if (existingIsFilter && carried.isEmpty()) {
                context.player().containerMenu.setCarried(existing);
                slot.set(ItemStack.EMPTY);
                context.container().setDataChanged();
                return true;
            }

            if (existingIsFilter && carriedIsFilter) {
                if (carried.getCount() > 1) {
                    context.player().drop(existing, true);
                    slot.set(carried.copyWithCount(1));
                    carried.shrink(1);
                } else {
                    context.player().containerMenu.setCarried(existing);
                    slot.set(carried);
                }
                context.container().setDataChanged();
                return true;
            }

            if (requiresFood && !isGoodFood.test(carried) && !isAttributeListFilter.test(carried)) return true;
            if (existingIsFilter || carried.isEmpty()) return true;

            ItemStack ghost;
            if (carriedIsFilter) {
                if (carried.getCount() == 1) {
                    ghost = carried;
                    context.player().containerMenu.setCarried(ItemStack.EMPTY);
                } else {
                    ghost = carried.copyWithCount(1);
                    carried.shrink(1);
                    context.player().containerMenu.setCarried(carried);
                }
            } else {
                if (carried.has(DataComponents.POTION_CONTENTS)) {
                    ghost = carried.copyWithCount(1);
                } else {
                    ghost = new ItemStack(carried.getItem(), 1);
                }
            }

            slot.set(ghost);
            context.container().setDataChanged();
        } else {
            return false;
        }
        return true;
    }

    @Override
    public @Nullable UpgradePanel createPanel(UpgradeContext context) {
        return new FeederPanel(context);
    }

    @Override
    protected void tickActive(UpgradeContext context) {
        if (context.level().getGameTime() % 15 != 0)
            return;

        if (context.backpack().isEmpty()) return;

        Player player = context.player();
        Level level = context.level();

        boolean doFeed = shouldFeedPlayer(player);

        if (doFeed) {
            // Look for food in backpack
            IBackpackContainer container = context.container();
            IItemHandlerModifiable itemHandler = container.getItemHandler();
            BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
            FilterItemStack filter = FilterItemStack.of(itemHandler.getStackInSlot(layout.feederFilter().getStartIndex()));

            for (int i : layout.items().range()) {
                ItemStack food = itemHandler.getStackInSlot(i);
                if (!filter.isEmpty()) {
                    if (!filter.test(level, food))
                        continue;
                }

                if (!Util.isEdible(food, player) || hasNegativeEffects(food, player)) continue;

                String foodName = food.getItem().getName(food).getString();

                // Stash MainHandItem and place food item in Main Hand
                ItemStack mainHandItem = player.getMainHandItem();
                player.getInventory().items.set(player.getInventory().selected, food);

                ItemStack singleItem = food.copyWithCount(1);

                // Use the food item and check if it was consumed
                if (singleItem.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME) {
                    player.getInventory().items.set(player.getInventory().selected, mainHandItem);
                    food.shrink(1);
                    itemHandler.setStackInSlot(i, food);

                    ItemStack remainder = EventHooks.onItemUseFinish(player, singleItem.copy(), 0, singleItem.getItem().finishUsingItem(singleItem, level, player));
                    if (!remainder.isEmpty()) {
                        boolean itemPlaced = false;
                        int firstEmptyStack = -1;

                        for (int j : layout.items().range()) {
                            ItemStack stack = itemHandler.getStackInSlot(j);

                            if (stack.isEmpty() && firstEmptyStack < 0) {
                                firstEmptyStack = j;
                            }
                            if ((ItemStack.isSameItemSameComponents(stack, remainder) && stack.getCount() < container.getStackMultiplier() * remainder.getMaxStackSize())) {
                                ItemStack insertResult = itemHandler.insertItem(j, remainder, false);
                                if (!insertResult.isEmpty()) {
                                    player.drop(remainder, true);
                                }
                                itemPlaced = true;
                                break;
                            }
                        }

                        if (!itemPlaced && firstEmptyStack > -1) {
                            itemHandler.insertItem(firstEmptyStack, remainder, false);
                        }
                    }

                    container.setDataChanged();

                    boolean displayMessage = container.getUpgradeSetting(UpgradeDataSync.Field.FEEDER_DISPLAY_MESSAGE);

                    if (displayMessage) {
                        String foodNameFormatted = (Util.isVowel(foodName.charAt(0)) ? "an" : "a") + " §a" + foodName + "§r";
                        player.displayClientMessage(Component.translatable("item.fxntstorage.backpack_feeder_upgrade.message", foodNameFormatted), true);
                    }

                } else {
                    // For some reason, food item was not consumed, revert item
                    player.getInventory().items.set(player.getInventory().selected, mainHandItem);
                }

                return;
            }
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

        private static final WidgetSprites CHORUS_ON = UpgradePanel.createWidgetSprites("chorus_on");
        private static final WidgetSprites CHORUS_OFF = UpgradePanel.createWidgetSprites("chorus_off");
        private static final WidgetSprites MSG_ON = UpgradePanel.createWidgetSprites("message_on");
        private static final WidgetSprites MSG_OFF = UpgradePanel.createWidgetSprites("message_off");

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
                                    ? Component.translatable("tooltip.fxntstorage.backpack_feeder_upgrade.panel.allow_chorus").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_feeder_upgrade.panel.allow_chorus.description").withStyle(ChatFormatting.DARK_GRAY))
                                    : Component.translatable("tooltip.fxntstorage.backpack_feeder_upgrade.panel.disallow_chorus").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_feeder_upgrade.panel.disallow_chorus.description").withStyle(ChatFormatting.DARK_GRAY)),
                            button -> menu.toggleUpgradeSetting(UpgradeDataSync.Field.FEEDER_ALLOW_CHORUS_FRUIT)
                    )
            );

            stateButtons.add(
                    new SpriteButton<>(
                            panelX + 41, panelY + 31, 18, 18,
                            initialState,
                            state -> state.displayMessage() ? MSG_ON : MSG_OFF,
                            state -> state.displayMessage()
                                    ? Component.translatable("tooltip.fxntstorage.backpack_feeder_upgrade.panel.show_message").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_feeder_upgrade.panel.show_message.description").withStyle(ChatFormatting.DARK_GRAY))
                                    : Component.translatable("tooltip.fxntstorage.backpack_feeder_upgrade.panel.hide_message").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_feeder_upgrade.panel.hide_message.description").withStyle(ChatFormatting.DARK_GRAY)),
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
                                Component.translatable("tooltip.fxntstorage.backpack_feeder_upgrade.panel.filter_slot"),
                                Component.translatable("tooltip.fxntstorage.backpack_feeder_upgrade.panel.filter_slot.description")
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
