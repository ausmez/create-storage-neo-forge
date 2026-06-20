package net.fxnt.fxntstorage.backpack.mounted;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.item.WrapperMountedItemStorage;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.foundation.codec.CreateCodecs;
import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.JukeboxBuffHandler;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.JukeboxHandler;
import net.fxnt.fxntstorage.backpack.upgrade.workshop.WorkshopUpgrade;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModEntityTypes;
import net.fxnt.fxntstorage.init.ModMenuTypes;
import net.fxnt.fxntstorage.init.ModMountedStorageTypes;
import net.fxnt.fxntstorage.network.packet.WorkshopProcessingPacket;
import net.fxnt.fxntstorage.util.ParticleHelper;
import net.fxnt.fxntstorage.util.SortOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BackpackMountedStorage extends WrapperMountedItemStorage<ItemStackHandler> implements IBackpackContainer {
    public static final MapCodec<BackpackMountedStorage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CreateCodecs.ITEM_STACK_HANDLER.fieldOf("inventory").forGetter(s -> s.wrapped),
            Codec.INT.fieldOf("stack_multiplier").forGetter(s -> s.stackMultiplier),
            CompoundTag.CODEC.fieldOf("upgrade_settings").forGetter(BackpackMountedStorage::serializeUpgradeSettings),
            Codec.STRING.optionalFieldOf("sort_order", "COUNT").forGetter(s -> s.sortOrder.name())
    ).apply(instance, BackpackMountedStorage::new));

    private static final int SLOT_COUNT = BackpackSlotLayout.createLayout().getTotalSlots();
    private static final BackpackSlotLayout LAYOUT = BackpackSlotLayout.createLayout();

    private final int stackMultiplier;
    private final UpgradeDataManager upgradeData = new UpgradeDataManager();
    private SortOrder sortOrder;
    @Nullable
    private UpgradeType expandedPanel = null;

    private boolean lastWorkshopProcessing = false;
    private boolean workshopSyncInitialized = false;

    BackpackMountedStorage(ItemStackHandler wrapped, int stackMultiplier, CompoundTag upgradeSettings, String sortOrderName) {
        super(ModMountedStorageTypes.BACKPACK_MOUNTED.get(), toBackpackHandler(wrapped, stackMultiplier));
        this.stackMultiplier = stackMultiplier;
        for (UpgradeDataSync.Field field : UpgradeDataSync.Field.values()) {
            String key = field.getId();
            if (upgradeSettings.contains(key)) {
                upgradeData.setSetting(field, upgradeSettings.getBoolean(key));
            }
        }
        if (upgradeSettings.contains("ExpandedPanel")) {
            this.expandedPanel = UpgradeType.fromBaseName(upgradeSettings.getString("ExpandedPanel"));
        }
        try {
            this.sortOrder = SortOrder.valueOf(sortOrderName);
        } catch (IllegalArgumentException e) {
            this.sortOrder = SortOrder.COUNT;
        }
    }

    public static BackpackMountedStorage fromStorage(BackpackEntity entity) {
        ItemStackHandler copy = copyToItemStackHandler(entity.getItemHandler());
        CompoundTag upgradeSettings = new CompoundTag();
        for (UpgradeDataSync.Field field : UpgradeDataSync.Field.values()) {
            upgradeSettings.putBoolean(field.getId(), entity.getUpgradeSetting(field));
        }
        UpgradeType expanded = entity.getExpandedPanel();
        if (expanded != null) {
            upgradeSettings.putString("ExpandedPanel", expanded.getId());
        }
        return new BackpackMountedStorage(copy, entity.getStackMultiplier(), upgradeSettings, entity.getSortOrder().name());
    }

    private CompoundTag serializeUpgradeSettings() {
        CompoundTag tag = new CompoundTag();
        for (UpgradeDataSync.Field field : UpgradeDataSync.Field.values()) {
            tag.putBoolean(field.getId(), upgradeData.getSetting(field));
        }
        if (expandedPanel != null) {
            tag.putString("ExpandedPanel", expandedPanel.getId());
        }
        return tag;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64 * stackMultiplier;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!LAYOUT.items().contains(slot)) return stack;
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!LAYOUT.items().contains(slot)) return ItemStack.EMPTY;
        return super.extractItem(slot, amount, simulate);
    }

    @Override
    public boolean handleInteraction(ServerPlayer player, Contraption contraption, StructureTemplate.StructureBlockInfo info) {
        int contraptionId = contraption.entity.getId();
        BlockPos localPos = info.pos();
        int multiplier = this.stackMultiplier;
        Component title = Component.translatable(info.state().getBlock().getDescriptionId());
        BackpackMountedStorage self = this;

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> {
                    BackpackMenu menu = new BackpackMenu(ModMenuTypes.BACKPACK_MENU.get(), id, inv, self, BackpackMenu.BackpackType.CONTRAPTION, null);
                    menu.setContraptionId(contraptionId);
                    return menu;
                },
                title
        ), (RegistryFriendlyByteBuf buf) -> {
            buf.writeEnum(BackpackMenu.BackpackType.CONTRAPTION);
            buf.writeInt(contraptionId);
            buf.writeBlockPos(localPos);
            buf.writeInt(multiplier);
        });

        return true;
    }

    private static ItemStackHandler toBackpackHandler(ItemStackHandler source, int multiplier) {
        BackpackItemStackHandler handler = new BackpackItemStackHandler(source.getSlots(), multiplier);
        for (int i = 0; i < source.getSlots(); i++) {
            handler.setStackInSlot(i, source.getStackInSlot(i));
        }
        return handler;
    }

    public static BackpackMountedStorage createEmpty(int stackMultiplier) {
        return new BackpackMountedStorage(new ItemStackHandler(SLOT_COUNT), stackMultiplier, new CompoundTag(), SortOrder.COUNT.name());
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof BackpackEntity backpackEntity)) return;

        ItemStackHandler entityHandler = backpackEntity.getItemHandler();
        for (int i = 0; i < wrapped.getSlots(); i++) {
            entityHandler.setStackInSlot(i, wrapped.getStackInSlot(i));
        }
        for (UpgradeDataSync.Field field : UpgradeDataSync.Field.values()) {
            backpackEntity.setUpgradeSetting(field, upgradeData.getSetting(field));
        }
        backpackEntity.setExpandedPanel(this.expandedPanel);
        backpackEntity.setSortOrder(this.sortOrder);
    }

    protected void tickMagnet(MovementContext context) {
        UpgradeDataManager upgradeData = getUpgradeData();
        boolean ignoreItemsProcessing = upgradeData.getSetting(UpgradeDataSync.Field.MAGNET_IGNORE_FAN, true);

        int filterSlotIndex = LAYOUT.magnetFilter().getStartIndex();
        FilterItemStack filter = FilterItemStack.of(getStackInSlot(filterSlotIndex));

        int range = ConfigManager.ServerConfig.MAGNET_PULL_RANGE.get();
        Vec3 worldPos = context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1.0f);
        BlockPos blockPos = BlockPos.containing(worldPos);
        AABB boundingBox = new AABB(blockPos).inflate(range);

        List<ItemEntity> nearbyItems = context.world.getEntitiesOfClass(ItemEntity.class, boundingBox);
        if (nearbyItems.isEmpty()) return;

        var stand = ModEntityTypes.MAGNET_PICKUP_ENTITY.create(context.world);
        if (stand == null) return;

        stand.setPos(worldPos.x, worldPos.y - 0.5, worldPos.z);
        context.world.addFreshEntity(stand);

        for (ItemEntity itemEntity : nearbyItems) {
            ItemStack stack = itemEntity.getItem();
            if (stack.getItem() instanceof BackpackItem) continue;

            if (!filter.isEmpty() && !filter.test(context.world, stack, stack.has(DataComponents.POTION_CONTENTS)))
                continue;

            if (ignoreItemsProcessing) {
                if (itemEntity.getAge() < 30) continue;
                if (itemEntity.getPersistentData()
                        .getCompound("CreateData")
                        .getCompound("Processing")
                        .getInt("Time") > 0) continue;
            }

            if (BackpackHelper.itemEntityToBackpack(this, itemEntity, null))
                stand.take(itemEntity, stack.getCount());
        }
        stand.discard();
    }

    protected void tickJukebox(MovementContext context) {
        boolean notesEnabled = ConfigManager.ServerConfig.JUKEBOX_NOTES_ENABLED.get();
        boolean buffsEnabled = ConfigManager.ServerConfig.JUKEBOX_BUFFS_ENABLED.get();
        if (!notesEnabled && !buffsEnabled) return;

        int entityId = context.contraption.entity.getId();
        if (!JukeboxHandler.isEntityPlaying(entityId)) return;

        long gameTime = context.world.getGameTime();
        Vec3 worldPos = context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1.0f);
        BlockPos worldBlockPos = BlockPos.containing(worldPos);

        if (notesEnabled) {
            int particleInterval = context.world.random.nextInt(31) + 10;
            if (gameTime % particleInterval == 0)
                ParticleHelper.jukeboxParticles(context.world, worldBlockPos);
        }

        if (!buffsEnabled) return;
        if (gameTime % 40 != 0) return;

        ItemStack disc = getStackInSlot(LAYOUT.jukeboxDiscs().getStartIndex());
        if (disc.isEmpty()) return;

        JukeboxPlayable playable = disc.get(DataComponents.JUKEBOX_PLAYABLE);
        if (playable == null) return;

        int range = ConfigManager.ServerConfig.JUKEBOX_BUFFS_RANGE.getAsInt();
        AABB area = new AABB(worldBlockPos).inflate(range);
        for (Player player : context.world.getEntitiesOfClass(Player.class, area)) {
            JukeboxBuffHandler.applyMusicBuffsFromBlock(player, playable.song().key().location());
        }
    }

    protected void tickWorkshop(MovementContext context) {
        IUpgrade workshop = UpgradeRegistry.get(UpgradeType.WORKSHOP);
        if (workshop == null) return;

        Vec3 worldPos = context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1.0f);
        BlockPos worldBlockPos = BlockPos.containing(worldPos);
        UpgradeContext ctx = UpgradeContext.forBlock(this, context.world, BackpackMenu.BackpackType.CONTRAPTION, worldBlockPos);
        workshop.tick(ctx);

        boolean spinning = WorkshopUpgrade.isSpinning(this);
        if (!workshopSyncInitialized || spinning != lastWorkshopProcessing) {
            workshopSyncInitialized = true;
            lastWorkshopProcessing = spinning;
            updateClientWorkshopProcessing(context, spinning);
        }
    }

    private void updateClientWorkshopProcessing(MovementContext context, boolean processing) {
        context.blockEntityData.putBoolean("WorkshopProcessing", processing);
        StructureTemplate.StructureBlockInfo info = context.contraption.getBlocks().get(context.localPos);
        if (info != null) {
            StructureTemplate.StructureBlockInfo updated =
                    new StructureTemplate.StructureBlockInfo(context.localPos, info.state(), context.blockEntityData);
            context.contraption.getBlocks().put(context.localPos, updated);
        }
        PacketDistributor.sendToPlayersTrackingEntity(context.contraption.entity,
                WorkshopProcessingPacket.forContraption(context.contraption.entity.getId(), processing, context.localPos));
    }

    protected boolean hasActiveUpgrade(UpgradeType upgrade) {
        return UpgradeHelper.hasActiveUpgrade(getItemHandler(), upgrade);
    }

    public UpgradeDataManager getUpgradeData() {
        return upgradeData;
    }

    @Override
    public ItemStackHandler getItemHandler() {
        return wrapped;
    }

    @Override
    public int getStackMultiplier() {
        return stackMultiplier;
    }

    @Override
    public void setPlayerInteraction(boolean isPlayer) {
    }

    @Override
    public void setDataChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(SortOrder order) {
        this.sortOrder = order;
    }

    @Override
    public boolean isPanelExpanded(UpgradeType type) {
        return expandedPanel == type;
    }

    @Override
    public void togglePanelExpanded(UpgradeType type) {
        expandedPanel = (expandedPanel == type) ? null : type;
    }

    @Override
    public void clearPanelExpanded(UpgradeType type) {
        if (expandedPanel == type) expandedPanel = null;
    }

    @Override
    public @Nullable UpgradeType getExpandedPanel() {
        return expandedPanel;
    }

    @Override
    public void setExpandedPanel(@Nullable UpgradeType type) {
        expandedPanel = type;
    }

    @Override
    public boolean getUpgradeSetting(UpgradeDataSync.Field setting) {
        return upgradeData.getSetting(setting);
    }

    @Override
    public void setUpgradeSetting(UpgradeDataSync.Field setting, boolean value) {
        upgradeData.setSetting(setting, value);
    }

    @Override
    public void saveSettings() {
    }

    private static class BackpackItemStackHandler extends ItemStackHandler {
        private final int stackMultiplier;

        BackpackItemStackHandler(int size, int stackMultiplier) {
            super(size);
            this.stackMultiplier = stackMultiplier;
        }

        @Override
        public int getSlotLimit(int slot) {
            ItemStack current = getStackInSlot(slot);
            int maxStack = current.isEmpty() ? 64 : current.getMaxStackSize();
            return maxStack * stackMultiplier;
        }

        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return stack.getMaxStackSize() * stackMultiplier;
        }
    }
}