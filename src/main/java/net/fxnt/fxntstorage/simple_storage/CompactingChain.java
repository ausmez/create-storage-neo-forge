package net.fxnt.fxntstorage.simple_storage;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record CompactingChain(Item t0, Item t1, int t0ToT1, @Nullable Item t2, int t1ToT2) {

    public record TierResult(Item item, int count) {}

    public int tiers() {
        return t2 != null ? 3 : 2;
    }

    // How many T0 items make up a single unit of the highest tier in this chain
    // Used to scale capacity off the highest tier - 1 iron block = 81 nuggets
    public int highestTierT0PerUnit() {
        return t2 != null ? t0ToT1 * t1ToT2 : t0ToT1;
    }

    public ItemStack itemForSlot(int slotIdx) {
        if (tiers() == 2) {
            return slotIdx == 0 ? new ItemStack(t1) : new ItemStack(t0);
        }
        return switch (slotIdx) {
            case 0 -> new ItemStack(t2);
            case 1 -> new ItemStack(t1);
            default -> new ItemStack(t0);
        };
    }

    public int toT0Units(Item item, int count) {
        if (item == t0) return count;
        if (item == t1) return count * t0ToT1;
        if (item == t2) return count * t0ToT1 * t1ToT2;
        return 0;
    }

    public int t1Count(int t0Stored) {
        return t0Stored / t0ToT1;
    }

    public int t2Count(int t0Stored) {
        return t2 != null ? t0Stored / (t0ToT1 * t1ToT2) : 0;
    }

    public TierResult toHighestTier(int t0Stored) {
        if (t2 != null && t0Stored >= t0ToT1 * t1ToT2) {
            return new TierResult(t2, t0Stored / (t0ToT1 * t1ToT2));
        }
        if (t0Stored >= t0ToT1) {
            return new TierResult(t1, t0Stored / t0ToT1);
        }
        return new TierResult(t0, t0Stored);
    }

    // Remainder after converting to highest tier
    public int remainderAfterHighestTier(int t0Stored) {
        if (t2 != null && t0Stored >= t0ToT1 * t1ToT2) {
            return t0Stored % (t0ToT1 * t1ToT2);
        }
        if (t0Stored >= t0ToT1) {
            return t0Stored % t0ToT1;
        }
        return 0;
    }
}
