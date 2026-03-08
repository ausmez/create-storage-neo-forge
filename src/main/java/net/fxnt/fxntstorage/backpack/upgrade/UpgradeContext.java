package net.fxnt.fxntstorage.backpack.upgrade;

import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class UpgradeContext {
    private final Object menu;
    private final Player player;
    private final Level level;
    private final ItemStack backpack;
    private final ItemStackHandler itemHandler;
    private final IBackpackContainer container;
    private final BackpackMenu.BackpackType backpackType;
    private final BlockPos blockPos;
    private final int slotId;
    private final int button;

    private UpgradeContext(Object menu, Player player, Level level, ItemStack backpack, IBackpackContainer container,
                           ItemStackHandler itemHandler, BackpackMenu.BackpackType backpackType, BlockPos blockPos, int slotId, int button) {
        this.menu = menu;
        this.player = player;
        this.level = level;
        this.backpack = backpack;
        this.container = container;
        this.itemHandler = itemHandler;
        this.backpackType = backpackType;
        this.blockPos = blockPos;
        this.slotId = slotId;
        this.button = button;
    }

    public static UpgradeContext forMenu(Object menu, Player player, ItemStackHandler handler,
                                         BackpackMenu.BackpackType type, @Nullable BlockPos pos) {
        return new UpgradeContext(menu, player, player.level(), null, null, handler, type, pos, -999,-1);
    }

    public static UpgradeContext forMenuWithSlot(Object menu, Player player, IBackpackContainer container,
                                                 BackpackMenu.BackpackType type, @Nullable BlockPos pos, int slotId, int button) {
        return new UpgradeContext(menu, player, player.level(), null, container, container.getItemHandler(), type, pos, slotId, button);
    }

    public static UpgradeContext forPlayer(Player player, ItemStack backpack, IBackpackContainer container,
                                           BackpackMenu.BackpackType type, @Nullable BlockPos pos) {
        return new UpgradeContext(null, player, player.level(), backpack, container, null, type, pos, -999,-1);
    }

    public static UpgradeContext forWornBackpack(Player player, ItemStack backpack, IBackpackContainer container) {
        return forPlayer(player, backpack, container, BackpackMenu.BackpackType.WORN, null);
    }

    public static UpgradeContext forBlock(IBackpackContainer container, Level level, BackpackMenu.BackpackType type,
                                          @Nullable BlockPos pos) {
        return new UpgradeContext(null, null, level, null, container, null, type, pos, -999,-1);
    }

    public static UpgradeContext forUpgradeSlot(Player player, IBackpackContainer container,
                                                BackpackMenu.BackpackType type) {
        return new UpgradeContext(null, player, player.level(), null, container, null, type, null, -999,-1);
    }

    @SuppressWarnings("unchecked")
    public <T> T menu() {
        return (T) menu;
    }

    public Player player() {
        return player;
    }

    public ItemStack backpack() {
        return backpack;
    }

    public IBackpackContainer container() {
        return container;
    }

    public ItemStackHandler itemHandler() {
        return itemHandler != null ? itemHandler : container != null ? container.getItemHandler() : null;
    }

    public BackpackMenu.BackpackType backpackType() {
        return backpackType;
    }

    @Nullable
    public BlockPos blockPos() {
        return blockPos;
    }

    public boolean isClientSide() {
        return player == null ? level.isClientSide : player.level().isClientSide;
    }

    public Level level() {
        return level;
    }

    public int slotId() { return slotId; }

    public int button() { return button; }
}
