package net.fxnt.fxntstorage.backpack.upgrade.jetpack;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

public enum JetpackModifier {
    NONE(() -> null),
    PROPELLER(AllItems.PROPELLER::asItem),
    ENCASED_FAN(AllBlocks.ENCASED_FAN::asItem),
    BLAZE_BURNER(AllBlocks.BLAZE_BURNER::asItem),
    NETHERITE_BACKTANK(AllItems.NETHERITE_BACKTANK::asItem),
    FLUID_VALVE(AllBlocks.FLUID_VALVE::asItem),
    FIREWORK_ROCKET(() -> Items.FIREWORK_ROCKET);

    public static final int BASE_MAX_HEIGHT = 32;
    public static final int PROPELLER_MAX_HEIGHT = 64;

    // Encased Fan: +10% fuel use per full 16 blocks above this height
    public static final int FAN_PENALTY_START_HEIGHT = 64;
    public static final int FAN_PENALTY_STEP_BLOCKS = 16;
    public static final double FAN_PENALTY_PER_STEP = 0.10;

    public static final double AFTERBURNER_SPEED_MULTIPLIER = 1.5;
    public static final double AFTERBURNER_FUEL_MULTIPLIER = 1.75;

    // Afterburner exhaust ignites entities beside and below the player while firing
    public static final double AFTERBURNER_IGNITE_RADIUS = 1.0;
    public static final double AFTERBURNER_IGNITE_DEPTH = 3.0;
    public static final float AFTERBURNER_IGNITE_SECONDS = 4.0f;

    public static final double NETHERITE_BACKTANK_CAPACITY_MULTIPLIER = 1.5;

    public static final double FLUID_VALVE_SPEED_MULTIPLIER = 0.9;
    public static final double FLUID_VALVE_FUEL_MULTIPLIER = 0.7;

    // Firework Rocket: Jetpack thrust boosts Elytra gliding instead of overriding it
    public static final double ELYTRA_BOOST_BASE_MAX_SPEED = 1.0;
    public static final double ELYTRA_BOOST_SPEED_MULTIPLIER = 1.5;
    public static final double ELYTRA_BOOST_FUEL_MULTIPLIER = 3.0;

    private final Supplier<Item> item;

    JetpackModifier(Supplier<Item> item) {
        this.item = item;
    }

    // Modpacks can turn the whole system off with the jetpackModifiersEnabled server config
    public static boolean isEnabled() {
        return !ConfigManager.ServerConfig.SERVER_SPEC.isLoaded() || ConfigManager.ServerConfig.JETPACK_MODIFIERS_ENABLED.get();
    }

    // Everything reads as NONE while modifiers are disabled, so no behavior, placement or tooltip treats it as one
    public static JetpackModifier fromStack(ItemStack stack) {
        if (stack.isEmpty() || !isEnabled()) return NONE;
        for (JetpackModifier modifier : values()) {
            if (modifier != NONE && stack.is(modifier.item.get())) return modifier;
        }
        return NONE;
    }

    public static JetpackModifier byId(int id) {
        JetpackModifier[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }

    public static boolean isModifier(ItemStack stack) {
        return fromStack(stack) != NONE;
    }

    public String getId() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public ItemStack getDisplayStack() {
        Item stackItem = item.get();
        return stackItem == null ? ItemStack.EMPTY : new ItemStack(stackItem);
    }

    // Blocks above the ground the jetpack can climb to, or Double.MAX_VALUE when uncapped
    public double getMaxHeight() {
        return switch (this) {
            case PROPELLER -> PROPELLER_MAX_HEIGHT;
            case ENCASED_FAN -> Double.MAX_VALUE;
            default -> BASE_MAX_HEIGHT;
        };
    }

    public boolean hasHeightLimit() {
        return this != ENCASED_FAN;
    }

    // Multiplier on the total air held by backtanks
    public double getCapacityMultiplier() {
        return switch (this) {
            case NETHERITE_BACKTANK -> NETHERITE_BACKTANK_CAPACITY_MULTIPLIER;
            default -> 1.0;
        };
    }

    // Fuel use multiplier that always applies, so it is folded into the displayed flight time
    public double getBaseFuelMultiplier() {
        return this == FLUID_VALVE ? FLUID_VALVE_FUEL_MULTIPLIER : 1.0;
    }

    // Full fuel use multiplier for the current flight conditions
    public double getFuelMultiplier(boolean afterburning, boolean elytraBoosting, double heightAboveGround) {
        return switch (this) {
            case FIREWORK_ROCKET -> elytraBoosting ? ELYTRA_BOOST_FUEL_MULTIPLIER : 1.0;
            case BLAZE_BURNER -> afterburning ? AFTERBURNER_FUEL_MULTIPLIER : 1.0;
            case ENCASED_FAN -> {
                if (heightAboveGround <= FAN_PENALTY_START_HEIGHT) yield 1.0;
                int steps = (int) ((heightAboveGround - FAN_PENALTY_START_HEIGHT) / FAN_PENALTY_STEP_BLOCKS);
                yield 1.0 + steps * FAN_PENALTY_PER_STEP;
            }
            default -> getBaseFuelMultiplier();
        };
    }

    public boolean allowsElytraBoost() {
        return this == FIREWORK_ROCKET;
    }

    // Top speed while Elytra boosting
    public double getElytraBoostMaxSpeed() {
        return ELYTRA_BOOST_BASE_MAX_SPEED * ELYTRA_BOOST_SPEED_MULTIPLIER;
    }

    public double getSpeedMultiplier(boolean afterburning) {
        return switch (this) {
            case BLAZE_BURNER -> afterburning ? AFTERBURNER_SPEED_MULTIPLIER : 1.0;
            case FLUID_VALVE -> FLUID_VALVE_SPEED_MULTIPLIER;
            default -> 1.0;
        };
    }

    public Object[] getTooltipArgs() {
        return switch (this) {
            case PROPELLER -> new Object[]{PROPELLER_MAX_HEIGHT};
            case ENCASED_FAN -> new Object[]{percent(1 + FAN_PENALTY_PER_STEP), FAN_PENALTY_STEP_BLOCKS, FAN_PENALTY_START_HEIGHT};
            case BLAZE_BURNER -> new Object[]{percent(AFTERBURNER_SPEED_MULTIPLIER), percent(AFTERBURNER_FUEL_MULTIPLIER)};
            case NETHERITE_BACKTANK -> new Object[]{percent(NETHERITE_BACKTANK_CAPACITY_MULTIPLIER)};
            case FLUID_VALVE -> new Object[]{percent(FLUID_VALVE_FUEL_MULTIPLIER), percent(FLUID_VALVE_SPEED_MULTIPLIER)};
            case FIREWORK_ROCKET -> new Object[]{percent(ELYTRA_BOOST_SPEED_MULTIPLIER), percent(ELYTRA_BOOST_FUEL_MULTIPLIER)};
            default -> new Object[0];
        };
    }

    // How far a multiplier is from 1.0 as a whole percentage, e.g. 1.75 and 0.25 both give 75
    private static long percent(double multiplier) {
        return Math.round(Math.abs(multiplier - 1.0) * 100);
    }

    // Converts raw backtank air into seconds of flight
    public double toEffectiveFuel(double air) {
        return air * getCapacityMultiplier() / getBaseFuelMultiplier();
    }
}
