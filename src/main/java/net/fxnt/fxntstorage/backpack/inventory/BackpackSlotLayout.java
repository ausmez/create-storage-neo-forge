package net.fxnt.fxntstorage.backpack.inventory;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BackpackSlotLayout {

    // Section definitions
    private final SlotSection items;
    private final SlotSection tools;
    private final SlotSection upgrades;
    private final SlotSection jukeboxDiscs;
    private final SlotSection magnetFilter;
    private final SlotSection feederFilter;

    private final int totalSlots;

    // Default layout constants
    private static final int ITEM_SLOTS = 108;
    private static final int TOOL_SLOTS = 24;
    private static final int UPGRADE_SLOTS = 6;
    private static final int JUKEBOX_SLOTS = 1;
    private static final int MAGNET_FILTER_SLOTS = 1;
    private static final int FEEDER_FILTER_SLOTS = 1;
    private static final int PLAYER_INV_SLOTS = 27;
    private static final int PLAYER_HOTBAR_SLOTS = 9;

    // Creates a backpack layout with slot counts
    public static BackpackSlotLayout createLayout() {
        return new Builder()
                .items(ITEM_SLOTS)
                .tools(TOOL_SLOTS)
                .upgrades(UPGRADE_SLOTS)
                .jukeboxDiscs(JUKEBOX_SLOTS)
                .magnetFilter(MAGNET_FILTER_SLOTS)
                .feederFilter(FEEDER_FILTER_SLOTS)
                .build();
    }

    private BackpackSlotLayout(Builder builder) {
        int offset = 0;

        this.items = new SlotSection("Items", offset, builder.itemSlots);
        offset += builder.itemSlots;

        this.tools = new SlotSection("Tools", offset, builder.toolSlots);
        offset += builder.toolSlots;

        this.upgrades = new SlotSection("Upgrades", offset, builder.upgradeSlots);
        offset += builder.upgradeSlots;

        this.jukeboxDiscs = new SlotSection("JukeboxDiscs", offset, builder.jukeboxSlots);
        offset += builder.jukeboxSlots;

        this.magnetFilter = new SlotSection("MagnetFilter", offset, builder.magnetFilter);
        offset += builder.magnetFilter;

        this.feederFilter = new SlotSection("FeederFilter", offset, builder.feederFilter);
        offset += builder.feederFilter;

        this.totalSlots = offset;
    }

    // Accessors
    public SlotSection items() {
        return items;
    }

    public SlotSection tools() {
        return tools;
    }

    public SlotSection upgrades() {
        return upgrades;
    }

    public SlotSection jukeboxDiscs() {
        return jukeboxDiscs;
    }

    public SlotSection magnetFilter() {
        return magnetFilter;
    }

    public SlotSection feederFilter() {
        return feederFilter;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    // ** Only used for SortRange **
    public SlotSection playerInventory() {
        return new SlotSection("Inventory", totalSlots, PLAYER_INV_SLOTS);
    }

    // ** Only used for SortRange **
    public SlotSection playerHotbar() {
        return new SlotSection("Hotbar", playerInventory().getEndIndex(), PLAYER_HOTBAR_SLOTS);
    }

    public SlotSection getSectionForSlot(int slotIndex) {
        if (items.contains(slotIndex)) return items;
        if (tools.contains(slotIndex)) return tools;
        if (upgrades.contains(slotIndex)) return upgrades;
        if (jukeboxDiscs.contains(slotIndex)) return jukeboxDiscs;
        if (magnetFilter.contains(slotIndex)) return magnetFilter;
        if (feederFilter.contains(slotIndex)) return feederFilter;

        if (playerInventory().contains(slotIndex)) return playerInventory();
        if (playerHotbar().contains(slotIndex)) return playerHotbar();

        throw new IllegalArgumentException("Slot index " + slotIndex + " is out of range");
    }

    // Gets all sections in order
    public List<SlotSection> getAllSections() {
        return List.of(items, tools, upgrades, jukeboxDiscs, magnetFilter, feederFilter);
    }

    // Represents a contiguous section of slots
    public static class SlotSection {
        private final String name;
        private final int startIndex;
        private final int count;
        private final int endIndex; // exclusive

        SlotSection(String name, int startIndex, int count) {
            this.name = name;
            this.startIndex = startIndex;
            this.count = count;
            this.endIndex = startIndex + count;
        }

        public String getName() {
            return name;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getCount() {
            return count;
        }

        public int getEndIndex() {
            return endIndex;
        }

        public boolean contains(int slotIndex) {
            return slotIndex >= startIndex && slotIndex < endIndex;
        }

        public IntRange range() {
            return new IntRange(startIndex, endIndex);
        }

        @Override
        public String toString() {
            return String.format("%s[%d-%d) (%d slots)", name, startIndex, endIndex, count);
        }
    }

    public IntRange getMultiRange(SlotSection... sections) {
        if (sections.length == 0)
            throw new IllegalArgumentException("Must provide at least one section");

        int minStart = Integer.MAX_VALUE;
        int maxEnd = Integer.MIN_VALUE;

        for (SlotSection section : sections) {
            minStart = Math.min(minStart, section.getStartIndex());
            maxEnd = Math.max(maxEnd, section.getEndIndex());
        }

        return new IntRange(minStart, maxEnd);
    }

    public IntRange getItemsAndToolsRange() {
        return getMultiRange(items, tools);
    }

    public IntRange getFiltersRange() {
        return getMultiRange(magnetFilter, feederFilter);
    }

    public static class IntRange implements Iterable<Integer> {
        private final int start;
        private final int end;

        public IntRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public java.util.@NotNull Iterator<Integer> iterator() {
            return new java.util.Iterator<>() {
                private int current = start;

                @Override
                public boolean hasNext() {
                    return current < end;
                }

                @Override
                public Integer next() {
                    if (!hasNext()) {
                        throw new java.util.NoSuchElementException();
                    }
                    return current++;
                }
            };
        }
    }

    // Determines sort range for a given slot.
    public SortRange getSortRangeForSlot(int slotIndex) {
        // Items section
        if (items().contains(slotIndex)) {
            return new SortRange(items().getStartIndex(), items().getEndIndex(), SortType.ITEMS);
        }

        // Tools section
        if (tools().contains(slotIndex)) {
            return new SortRange(tools().getStartIndex(), tools().getEndIndex(), SortType.TOOLS);
        }

        // Upgrades - don't sort
        if (upgrades().contains(slotIndex)) {
            return SortRange.NONE;
        }

        // Jukebox disc - don't sort
        if (jukeboxDiscs().contains(slotIndex)) {
            return SortRange.NONE;
        }

        // Magnet Filter - don't sort
        if (magnetFilter().contains(slotIndex)) {
            return SortRange.NONE;
        }

        // Feeder Filter - don't sort
        if (feederFilter().contains(slotIndex)) {
            return SortRange.NONE;
        }

        // Player inventory
        if (playerInventory().contains(slotIndex)) {
            return new SortRange(playerInventory().getStartIndex(), playerInventory().getEndIndex(), SortType.PLAYER_INVENTORY);
        }

        // Player hotbar
        if (playerHotbar().contains(slotIndex)) {
            return new SortRange(playerHotbar().getStartIndex(), playerHotbar().getEndIndex(), SortType.PLAYER_HOTBAR);
        }

        return SortRange.NONE;
    }

    public static class SortRange {
        public static final SortRange NONE = new SortRange(-1, -1, SortType.NONE);

        private final int startIndex;
        private final int endIndex;
        private final SortType type;

        public SortRange(int startIndex, int endIndex, SortType type) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.type = type;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        public boolean isValid() {
            return this != NONE;
        }

        @Override
        public String toString() {
            if (this == NONE) return "SortRange[NONE]";
            return String.format("SortRange[%d-%d, %s]", startIndex, endIndex, type);
        }
    }

    public enum SortType {
        NONE,
        ITEMS,
        TOOLS,
        PLAYER_INVENTORY,
        PLAYER_HOTBAR
    }

    public static class Builder {
        private int itemSlots = 0;
        private int toolSlots = 0;
        private int upgradeSlots = 0;
        private int jukeboxSlots = 0;
        private int magnetFilter = 0;
        private int feederFilter = 0;

        public Builder items(int count) {
            this.itemSlots = count;
            return this;
        }

        public Builder tools(int count) {
            this.toolSlots = count;
            return this;
        }

        public Builder upgrades(int count) {
            this.upgradeSlots = count;
            return this;
        }

        public Builder jukeboxDiscs(int count) {
            this.jukeboxSlots = count;
            return this;
        }

        public Builder magnetFilter(int count) {
            this.magnetFilter = count;
            return this;
        }

        public Builder feederFilter(int count) {
            this.feederFilter = count;
            return this;
        }

        public BackpackSlotLayout build() {
            return new BackpackSlotLayout(this);
        }
    }
}
