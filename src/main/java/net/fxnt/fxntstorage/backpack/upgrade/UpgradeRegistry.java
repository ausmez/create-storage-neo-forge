package net.fxnt.fxntstorage.backpack.upgrade;

import net.fxnt.fxntstorage.backpack.upgrade.crafting.CraftingUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.falldamage.FallDamageUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.feeder.FeederUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.health.MechanicalHeartUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.itempickup.ItemPickupUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.JukeboxUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.magnet.MagnetUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.oremining.OreMiningUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.pickblock.PickBlockUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.refill.RefillUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.toolswap.ToolSwapUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.torch.TorchDeployerUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.workshop.WorkshopUpgrade;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class UpgradeRegistry {
    private static final Map<UpgradeType, IUpgrade> UPGRADES = new EnumMap<>(UpgradeType.class);
    private static boolean initialized = false;

    public static void register() {
        if (initialized) {
            throw new IllegalStateException("UpgradeRegistry already initialized");
        }

        register(new FeederUpgrade());
        register(new CraftingUpgrade());
        register(new WorkshopUpgrade());
        register(new JetpackUpgrade());
        register(new JukeboxUpgrade());
        register(new MagnetUpgrade());
        register(new ToolSwapUpgrade());
        register(new OreMiningUpgrade());
        register(new TorchDeployerUpgrade());
        register(new PickBlockUpgrade());
        register(new RefillUpgrade());
        register(new ItemPickupUpgrade());
        register(new FallDamageUpgrade());
        register(new MechanicalHeartUpgrade());

        initialized = true;
    }

    public static void register(IUpgrade upgrade) {
        if (UPGRADES.containsKey(upgrade.getType())) {
            throw new IllegalArgumentException("Upgrade already registered: " + upgrade.getType());
        }
        UPGRADES.put(upgrade.getType(), upgrade);
    }

    public static IUpgrade get(UpgradeType type) {
        return UPGRADES.get(type);
    }

    public static Collection<IUpgrade> getAll() {
        return Collections.unmodifiableCollection(UPGRADES.values());
    }

    public static boolean getDefaultSetting(UpgradeDataSync.Field field) {
        for (IUpgrade upgrade : UPGRADES.values()) {
            Map<UpgradeDataSync.Field, Boolean> defaults = upgrade.getDefaultSettings();
            if (defaults.containsKey(field)) {
                return defaults.get(field);
            }
        }
        return false;
    }
}
