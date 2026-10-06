package net.fxnt.fxntstorage.simple_storage;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record CompactingChain(Item t0, Item t1, int t0ToT1, @Nullable Item t2, int t1ToT2,
                              Set<Item> t0Variants, Set<Item> t1Variants) {

    public record TierResult(Item item, int count) {}

    // Express a raw T0 amount as the whole amount in each tier
    public List<ItemStack> tierViews(int t0Stored) {
        List<ItemStack> result = new ArrayList<>();
        if (t2 != null) {
            int count = t2Count(t0Stored);
            if (count > 0) result.add(new ItemStack(t2, count));
        }
        int t1Count = t1Count(t0Stored);
        if (t1Count > 0) result.add(new ItemStack(t1, t1Count));
        if (t0Stored > 0) result.add(new ItemStack(t0, t0Stored));
        return result;
    }

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

    // Tier index (0 = T0) of the item or one of its variants, -1 if it isn't part of this chain
    public int tierOf(Item item) {
        if (item == t0 || t0Variants.contains(item)) return 0;
        if (item == t1 || t1Variants.contains(item)) return 1;
        if (t2 != null && item == t2) return 2;
        return -1;
    }

    public int toT0Units(Item item, int count) {
        return switch (tierOf(item)) {
            case 0 -> count;
            case 1 -> count * t0ToT1;
            case 2 -> count * t0ToT1 * t1ToT2;
            default -> 0;
        };
    }

    // Every item the chain accepts
    public List<Item> acceptedItems() {
        List<Item> result = new ArrayList<>();
        result.add(t0);
        result.addAll(t0Variants);
        result.add(t1);
        result.addAll(t1Variants);
        if (t2 != null) result.add(t2);
        return result;
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
