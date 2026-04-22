package net.fxnt.fxntstorage.backpack.upgrade.magnet;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
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
import java.util.function.Predicate;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;


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
                275, 58,
                () -> menu.hasUpgrade(UpgradeType.MAGNET),
                () -> menu.isPanelExpanded(UpgradeType.MAGNET)
        ));
    }

    @Override
    public @Nullable UpgradePanel createPanel(UpgradeContext context) {
        return new MagnetPanel(context);
    }

    @Override
    public boolean clicked(UpgradeContext context) {
        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

        if (layout.magnetFilter().contains(context.slotId())) {
            Slot slot = context.player().containerMenu.slots.get(context.slotId());
            ItemStack carried = context.player().containerMenu.getCarried();
            ItemStack existing = slot.getItem();

            Predicate<ItemStack> isCreateFilter = stack -> stack.getItem() instanceof FilterItem;

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
                            panelX + 26, panelY + 32, 18, 18,
                            initialState,
                            state -> state.ignoreFan() ? modLoc("cross") : modLoc("check"),
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
