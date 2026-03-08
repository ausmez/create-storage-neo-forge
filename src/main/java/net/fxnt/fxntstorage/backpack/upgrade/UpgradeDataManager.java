package net.fxnt.fxntstorage.backpack.upgrade;

import net.fxnt.fxntstorage.init.ModDataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UpgradeDataManager {
    private int expandedPanels = 0;
    private final Map<String, Boolean> booleanSettings = new HashMap<>();

    public UpgradeDataManager() {
    }

    public boolean isPanelExpanded(UpgradeType type) {
        return type.isPanelExpandedInMask(expandedPanels);
    }

    public void togglePanel(UpgradeType type) {
        expandedPanels = type.toggleInMask(expandedPanels);
    }

    public void clearPanel(UpgradeType type) {
        expandedPanels = type.clearInMask(expandedPanels);
    }

    public int getExpandedPanelsBitmask() {
        return expandedPanels;
    }

    public void setExpandedPanelsBitmask(int mask) {
        this.expandedPanels = mask;
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
        expandedPanels = 0;
        booleanSettings.clear();
    }

    // Copy from another manager
    public void copyFrom(UpgradeDataManager other) {
        this.expandedPanels = other.expandedPanels;
        this.booleanSettings.clear();
        this.booleanSettings.putAll(other.booleanSettings);
    }

    public static UpgradeDataManager loadFromItem(ItemStack stack) {
        UpgradeDataManager manager = new UpgradeDataManager();

        int rawPanel = stack.getOrDefault(ModDataComponents.BACKPACK_ACTIVE_PANELS, 0);
        manager.expandedPanels = Math.max(0, rawPanel);

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
        stack.set(ModDataComponents.BACKPACK_ACTIVE_PANELS, expandedPanels);

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

        if (tag.contains("ExpandedPanels")) {
            manager.expandedPanels = tag.getInt("ExpandedPanels");
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
        tag.putInt("ExpandedPanels", expandedPanels);

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
