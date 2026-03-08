package net.fxnt.fxntstorage.backpack.upgrade;

import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public abstract class AbstractUpgrade implements IUpgrade {
    protected final UpgradeType type;

    protected AbstractUpgrade(UpgradeType type) {
        this.type = type;
    }

    @Override
    public UpgradeType getType() {
        return type;
    }

    @Override
    public List<UpgradeDataSync.Field> getSettings() {
        return List.of();
    }

    @Override
    public Map<UpgradeDataSync.Field, Boolean> getDefaultSettings() {
        return Map.of();
    }

    @Override
    public @Nullable UpgradePanel createPanel(UpgradeContext context) {
        return null;
    }

    @Override
    public List<Slot> createSlots(UpgradeContext context) {
        return List.of();
    }

    @Override
    public void onInstalled(UpgradeContext context) {
    }

    @Override
    public void onRemoved(UpgradeContext context) {
    }

    @Override
    public void tick(UpgradeContext context) {
        if (!UpgradeHelper.hasActiveUpgrade(context.itemHandler(), type)) {
            return;
        }
        tickActive(context);
    }

    protected void tickActive(UpgradeContext context) {
    }
}
