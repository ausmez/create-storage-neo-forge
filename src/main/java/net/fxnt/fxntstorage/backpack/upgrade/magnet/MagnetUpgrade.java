package net.fxnt.fxntstorage.backpack.upgrade.magnet;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.client.menu.button.GuiIcon;
import net.fxnt.fxntstorage.backpack.client.menu.button.ItemSpriteButton;
import net.fxnt.fxntstorage.backpack.client.menu.slot.MagnetFilterSlot;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;


public class MagnetUpgrade extends AbstractUpgrade {

    public MagnetUpgrade() {
        super(UpgradeType.MAGNET);
    }

    @Override
    public List<UpgradeDataSync.Field> getSettings() {
        return List.of(UpgradeDataSync.Field.MAGNET_IGNORE_FAN);
    }

    @Override
    public Map<UpgradeDataSync.Field, Boolean> getDefaultSettings() {
        return Map.of(UpgradeDataSync.Field.MAGNET_IGNORE_FAN, true);
    }

    @Override
    public List<Slot> createSlots(UpgradeContext context) {
        BackpackMenu menu = context.menu();
        return List.of(new MagnetFilterSlot(
                menu.container,
                menu.layout.magnetFilter().getStartIndex(),
                274, 58,
                () -> menu.hasUpgrade(UpgradeType.MAGNET),
                () -> menu.isPanelExpanded(UpgradeType.MAGNET)
        ));
    }

    @Override
    public @Nullable UpgradePanel createPanel(UpgradeContext context) {
        return new MagnetPanel(context);
    }

    @Override
    public int getFilterSlotIndex(BackpackSlotLayout layout) {
        return layout.magnetFilter().getStartIndex();
    }

    @Override
    public void onUninstalled(UpgradeContext context) {
        GhostFilterHelper.returnFilterItem(context, getFilterSlotIndex(BackpackSlotLayout.createLayout()));
    }

    @Override
    public boolean clicked(UpgradeContext context) {
        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();
        if (context.slotId() != getFilterSlotIndex(layout)) return false;

        // No filterAccepts override: the magnet filters on anything
        return GhostFilterHelper.handleClick(context, stack -> filterAccepts(context, stack));
    }

    @Override
    protected void tickActive(UpgradeContext context) {
        if (context.level().getGameTime() % 30 != 0)
            return;

        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

        AABB boundingBox;
        UpgradeDataManager manager;

        int range = ConfigManager.ServerConfig.MAGNET_PULL_RANGE.get();

        switch (context.backpackType()) {
            case WORN -> {
                boundingBox = new AABB(context.player().blockPosition()).inflate(range);
                manager = UpgradeDataManager.loadFromItem(context.backpack());
            }
            case BLOCK -> {
                boundingBox = new AABB(context.blockPos()).inflate(range);

                CompoundTag tag = ((BackpackEntity) context.container())
                        .saveWithoutMetadata(context.level().registryAccess());

                manager = UpgradeDataManager.loadFromNBT(tag);
            }
            default -> {
                return;
            }
        }

        List<ItemEntity> nearbyItems = context.level().getEntitiesOfClass(ItemEntity.class, boundingBox);
        if (nearbyItems.isEmpty()) return;

        int filterSlotIndex = layout.magnetFilter().getStartIndex();
        FilterItemStack filter = FilterItemStack.of(context.itemHandler().getStackInSlot(filterSlotIndex));
        boolean ignoreItemsProcessing = manager.getSetting(UpgradeDataSync.Field.MAGNET_IGNORE_FAN, true);

        if (context.backpackType() == BackpackMenu.BackpackType.WORN) {
            for (ItemEntity itemEntity : nearbyItems) {
                ItemStack stack = itemEntity.getItem();
                // Ignore backpacks
                if (stack.getItem() instanceof BackpackItem) continue;

                if (!passesFilter(context, itemEntity, filter, ignoreItemsProcessing)) continue;

                // Apply magnet
                if (BackpackHelper.itemEntityToBackpack(context.container(), itemEntity, context.player())) {
                    context.player().take(itemEntity, stack.getCount());
                }
            }
        } else if (context.backpackType() == BackpackMenu.BackpackType.BLOCK) {
            Level level = context.level();
            BlockPos pos = context.blockPos();

            MagnetPickupEntity stand = ModEntityTypes.MAGNET_PICKUP_ENTITY.create(level);
            if (stand == null) return;

            stand.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            level.addFreshEntity(stand);

            for (ItemEntity itemEntity : nearbyItems) {
                ItemStack stack = itemEntity.getItem();
                if (stack.getItem() instanceof BackpackItem) continue;

                // Apply filter
                if (!passesFilter(context, itemEntity, filter, ignoreItemsProcessing)) continue;

                if (BackpackHelper.itemEntityToBackpack(context.container(), itemEntity, null))
                    stand.take(itemEntity, stack.getCount());
            }
            stand.discard();
        }
    }

    private boolean passesFilter(UpgradeContext context, ItemEntity itemEntity, FilterItemStack filter, boolean ignoreItemsProcessing) {
        ItemStack stack = itemEntity.getItem();

        // Apply filter
        if (!filter.isEmpty() && !filter.test(context.level(), stack, stack.has(DataComponents.POTION_CONTENTS)))
            return false;

        if (ignoreItemsProcessing) {
            if (itemEntity.getAge() < 30)
                return false;
            return itemEntity.getPersistentData()
                    .getCompound("CreateData")
                    .getCompound("Processing")
                    .getInt("Time") <= 0;
        }
        return true;
    }

    public static class MagnetPanel implements UpgradePanel {
        private final List<ItemSpriteButton<MagnetState>> stateButtons = new ArrayList<>();

        public record MagnetState(boolean ignoreFan) {
        }

        private int panelX;
        private int panelY;

        private final UpgradeContext context;
        private final List<AbstractWidget> widgets = new ArrayList<>();

        public MagnetPanel(UpgradeContext context) {
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

        private MagnetState getState() {
            BackpackMenu menu = context.menu();
            return new MagnetState(
                    menu.isUpgradeSettingEnabled(UpgradeDataSync.Field.MAGNET_IGNORE_FAN)
            );
        }

        @Override
        public void createWidgets(Consumer<AbstractWidget> widgetAdder) {
            BackpackMenu menu = context.menu();

            MagnetState initialState = getState();

            stateButtons.add(
                    new ItemSpriteButton<>(
                            panelX + 22, panelY + 31, 18, 18,
                            initialState,
                            state -> state.ignoreFan() ? GuiIcon.CROSS : GuiIcon.CHECK,
                            state -> state.ignoreFan()
                                    ? Component.translatable("tooltip.fxntstorage.backpack_magnet_upgrade.panel.ignore_fan_items").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_magnet_upgrade.panel.ignore_fan_items.description").withStyle(ChatFormatting.DARK_GRAY))
                                    : Component.translatable("tooltip.fxntstorage.backpack_magnet_upgrade.panel.pull_fan_items").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_magnet_upgrade.panel.pull_fan_items.description").withStyle(ChatFormatting.DARK_GRAY)),
                            state -> AllBlocks.ENCASED_FAN.asStack(), 14,
                            button -> menu.toggleUpgradeSetting(UpgradeDataSync.Field.MAGNET_IGNORE_FAN)

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
            if (hoveredSlot instanceof MagnetFilterSlot && !hoveredSlot.hasItem()) {
                graphics.renderTooltip(
                        font,
                        List.of(
                                Component.translatable("tooltip.fxntstorage.backpack_magnet_upgrade.panel.filter_slot"),
                                Component.translatable("tooltip.fxntstorage.backpack_magnet_upgrade.panel.filter_slot.description")
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
