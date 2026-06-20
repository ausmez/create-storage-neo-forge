package net.fxnt.fxntstorage.backpack.upgrade;

import net.fxnt.fxntstorage.init.ModDataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UpgradeDataManager {
    // The single upgrade whose panel is currently expanded, or null when none is open
    @Nullable
    private UpgradeType expandedPanel = null;
    private final Map<String, Boolean> booleanSettings = new HashMap<>();

    public UpgradeDataManager() {
    }

    public boolean isPanelExpanded(UpgradeType type) {
        return expandedPanel == type;
    }

    public void togglePanel(UpgradeType type) {
        expandedPanel = (expandedPanel == type) ? null : type;
    }

    public void clearPanel(UpgradeType type) {
        if (expandedPanel == type) expandedPanel = null;
    }

    @Nullable
    public UpgradeType getExpandedPanel() {
        return expandedPanel;
    }

    public void setExpandedPanel(@Nullable UpgradeType type) {
        this.expandedPanel = type;
    }

    public boolean getSetting(UpgradeDataSync.Field field, boolean defaultValue) {
        return booleanSettings.getOrDefault(field.getId(), defaultValue);
    }

    public boolean getSetting(UpgradeDataSync.Field field) {
        boolean registryDefault = UpgradeRegistry.getDefaultSetting(field);
        return booleanSettings.getOrDefault(field.getId(), registryDefault);
    }

    public void setSetting(UpgradeDataSync.Field field, boolean value) {
        booleanSettings.put(field.getId(), value);
    }

    public boolean hasSetting(UpgradeDataSync.Field field) {
        return booleanSettings.containsKey(field.getId());
    }

    public void clearSetting(UpgradeDataSync.Field field) {
        booleanSettings.remove(field.getId());
    }

    public void clear() {
        expandedPanel = null;
        booleanSettings.clear();
    }

    // Copy from another manager
    public void copyFrom(UpgradeDataManager other) {
        this.expandedPanel = other.expandedPanel;
        this.booleanSettings.clear();
        this.booleanSettings.putAll(other.booleanSettings);
    }

    public static UpgradeDataManager loadFromItem(ItemStack stack) {
        UpgradeDataManager manager = new UpgradeDataManager();

        manager.expandedPanel = UpgradeType.fromBaseName(
                stack.getOrDefault(ModDataComponents.BACKPACK_ACTIVE_PANELS, ""));

        for (UpgradeDataSync.Field field : UpgradeDataSync.Field.values()) {
            DataComponentType<Boolean> component = ModDataComponents.getComponentForField(field);
            if (component != null) {
                boolean defaultValue = UpgradeRegistry.getDefaultSetting(field);
                manager.setSetting(field, stack.getOrDefault(component, defaultValue));
            }
        }

        return manager;
    }

    public void saveToItem(ItemStack stack) {
        if (expandedPanel != null) {
            stack.set(ModDataComponents.BACKPACK_ACTIVE_PANELS, expandedPanel.getId());
        } else {
            stack.remove(ModDataComponents.BACKPACK_ACTIVE_PANELS);
        }

        for (UpgradeDataSync.Field field : UpgradeDataSync.Field.values()) {
            DataComponentType<Boolean> component = ModDataComponents.getComponentForField(field);
            if (component == null) continue;

            if (booleanSettings.containsKey(field.getId())) {
                stack.set(component, booleanSettings.get(field.getId()));
            } else {
                stack.remove(component);
            }
        }
    }

    public static UpgradeDataManager loadFromNBT(CompoundTag tag) {
        UpgradeDataManager manager = new UpgradeDataManager();

        if (tag.contains("ExpandedPanel")) {
            manager.expandedPanel = UpgradeType.fromBaseName(tag.getString("ExpandedPanel"));
        }

        if (tag.contains("UpgradeSettings")) {
            CompoundTag settingsTag = tag.getCompound("UpgradeSettings");
            for (String key : settingsTag.getAllKeys()) {
                manager.booleanSettings.put(key, settingsTag.getBoolean(key));
            }
        }

        return manager;
    }

    public void saveToNBT(CompoundTag tag, Set<UpgradeType> installedTypes) {
        if (expandedPanel != null) {
            tag.putString("ExpandedPanel", expandedPanel.getId());
        }

        CompoundTag settingsTag = new CompoundTag();
        for (UpgradeType type : installedTypes) {
            IUpgrade upgrade = UpgradeRegistry.get(type);
            if (upgrade == null) continue;
            for (UpgradeDataSync.Field field : upgrade.getSettings()) {
                // Only write if explicitly set; skip fields that equal the registry default
                // and have never been written (i.e. not present in the map), to keep NBT clean.
                if (booleanSettings.containsKey(field.getId())) {
                    settingsTag.putBoolean(field.getId(), booleanSettings.get(field.getId()));
                }
            }
        }
        if (!settingsTag.isEmpty()) {
            tag.put("UpgradeSettings", settingsTag);
        }
    }
}
