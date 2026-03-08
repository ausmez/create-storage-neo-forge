package net.fxnt.fxntstorage.backpack.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UpgradeDataManager {
    private static final String NBT_EXPANDED_PANELS = "BackpackActivePanels";
    private static final String NBT_UPGRADE_SETTINGS = "BackpackUpgradeSettings";

    private int expandedPanels = 0;
    private final Map<String, Boolean> booleanSettings = new HashMap<>();

    private boolean dirty = false;

    public UpgradeDataManager() {
    }

    public boolean isPanelExpanded(UpgradeType type) {
        return type.isPanelExpandedInMask(expandedPanels);
    }

    public void togglePanel(UpgradeType type) {
        expandedPanels = type.toggleInMask(expandedPanels);
        dirty = true;
    }

    public void clearPanel(UpgradeType type) {
        expandedPanels = type.clearInMask(expandedPanels);
        dirty = true;
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
        dirty = true;
    }

    public boolean hasSetting(UpgradeDataSync.Field field) {
        return booleanSettings.containsKey(field.getId());
    }

    public void clearSetting(UpgradeDataSync.Field field) {
        booleanSettings.remove(field.getId());
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clear() {
        expandedPanels = 0;
        booleanSettings.clear();
        dirty = true;
    }

    // Copy from another manager
    public void copyFrom(UpgradeDataManager other) {
        this.expandedPanels = other.expandedPanels;
        this.booleanSettings.clear();
        this.booleanSettings.putAll(other.booleanSettings);
        dirty = true;
    }

    public static UpgradeDataManager loadFromItem(ItemStack stack) {
        UpgradeDataManager manager = new UpgradeDataManager();

        CompoundTag tag = stack.getTag();
        if (tag == null) return manager;

        if (tag.contains(NBT_EXPANDED_PANELS)) {
            manager.expandedPanels = Math.max(0, tag.getInt(NBT_EXPANDED_PANELS));
        }

        if (tag.contains(NBT_UPGRADE_SETTINGS)) {
            CompoundTag settingsTag = tag.getCompound(NBT_UPGRADE_SETTINGS);
            for (UpgradeDataSync.Field field : UpgradeDataSync.Field.values()) {
                String id = field.getId();
                if (settingsTag.contains(id)) {
                    manager.setSetting(field, settingsTag.getBoolean(id));
                }
            }
        }

        return manager;
    }

    public void saveToItem(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(NBT_EXPANDED_PANELS, expandedPanels);

        CompoundTag settingsTag = new CompoundTag();
        for (Map.Entry<String, Boolean> entry : booleanSettings.entrySet()) {
            settingsTag.putBoolean(entry.getKey(), entry.getValue());
        }
        tag.put(NBT_UPGRADE_SETTINGS, settingsTag);
        dirty = false;
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
        dirty = false;
    }
}
