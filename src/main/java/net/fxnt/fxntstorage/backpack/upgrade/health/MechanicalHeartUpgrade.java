package net.fxnt.fxntstorage.backpack.upgrade.health;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.upgrade.AbstractUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeContext;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class MechanicalHeartUpgrade extends AbstractUpgrade {

    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("a3f5c2b1-4d6e-4f8a-9b0c-1d2e3f4a5b6c");
    private static final String HEALTH_MODIFIER_NAME = "fxntstorage.mechanical_heart";
    private static final String SAVED_HEALTH_KEY = "MechanicalHeartSavedHealth";

    public MechanicalHeartUpgrade() {
        super(UpgradeType.HEALTH);
    }

    @Override
    public void onInstalled(UpgradeContext context) {
        if (context.backpackType() != BackpackMenu.BackpackType.WORN) return;
        applyModifier(context.player());
    }

    @Override
    public void onRemoved(UpgradeContext context) {
        if (context.backpackType() != BackpackMenu.BackpackType.WORN) return;
        clearSavedHealth(context.player());
        removeModifier(context.player());
    }

    @Override
    public void onBackpackEquipped(UpgradeContext context) {
        Player player = context.player();
        applyModifier(player);
        restoreSavedHealth(player);
    }

    @Override
    public void onBackpackUnequipped(UpgradeContext context) {
        removeModifier(context.player());
    }

    private void applyModifier(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;

        // remove to avoid stacking on re-equip
        attribute.removeModifier(HEALTH_MODIFIER_UUID);
        attribute.addTransientModifier(new AttributeModifier(
                HEALTH_MODIFIER_UUID,
                HEALTH_MODIFIER_NAME,
                ConfigManager.ServerConfig.HEALTH_UPGRADE_BONUS.get() * 2,
                AttributeModifier.Operation.ADDITION
        ));

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void removeModifier(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;

        attribute.removeModifier(HEALTH_MODIFIER_UUID);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static void saveCurrentHealth(Player player) {
        float currentHealth = player.getHealth();
        if (currentHealth <= 0) return; // dead players – don't save

        CompoundTag modTag = player.getPersistentData()
                .getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG);
        modTag.putFloat(SAVED_HEALTH_KEY, currentHealth);
        player.getPersistentData().put(ConfigManager.FXNTSTORAGE_SETTINGS_TAG, modTag);
    }

    private static void restoreSavedHealth(Player player) {
        CompoundTag modTag = player.getPersistentData()
                .getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG);

        if (!modTag.contains(SAVED_HEALTH_KEY)) return;

        float savedHealth = modTag.getFloat(SAVED_HEALTH_KEY);
        modTag.remove(SAVED_HEALTH_KEY);
        player.getPersistentData().put(ConfigManager.FXNTSTORAGE_SETTINGS_TAG, modTag);

        // Clamp to current max health as a safety net, then apply.
        float clampedHealth = Math.min(savedHealth, player.getMaxHealth());
        if (clampedHealth > 0) {
            player.setHealth(clampedHealth);
        }
    }

    private static void clearSavedHealth(Player player) {
        CompoundTag modTag = player.getPersistentData()
                .getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG);
        if (modTag.contains(SAVED_HEALTH_KEY)) {
            modTag.remove(SAVED_HEALTH_KEY);
            player.getPersistentData().put(ConfigManager.FXNTSTORAGE_SETTINGS_TAG, modTag);
        }
    }
}
