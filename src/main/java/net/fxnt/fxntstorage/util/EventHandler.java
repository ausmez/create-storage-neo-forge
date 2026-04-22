package net.fxnt.fxntstorage.util;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.backpack.upgrade.health.MechanicalHeartUpgrade;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackHandler;
import net.fxnt.fxntstorage.backpack.upgrade.jetpack.JetpackManager;
import net.fxnt.fxntstorage.backpack.upgrade.jukebox.JukeboxHandler;
import net.fxnt.fxntstorage.backpack.upgrade.torch.TorchDeployerManager;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ClientSettings;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.container.StorageBox;
import net.fxnt.fxntstorage.container.StorageBoxEntity;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.network.packet.CrossbowChargedPacket;
import net.fxnt.fxntstorage.registry.ContraptionStorageFilters;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingGetProjectileEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.network.NetworkHooks;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = FXNTStorage.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {
    private static final BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player.isCreative() || player.level().isClientSide) return;
        if (!event.getAction().equals(PlayerInteractEvent.LeftClickBlock.Action.START)) return;

        if (!BackpackHelper.isWearingBackpack(player)) return;

        UpgradeEventDispatcher.dispatchLeftClickBlock(player, event.getHand(), event.getPos());
    }

    @SubscribeEvent
    public static void onRightClickStorageBox(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.isSpectator()) return;
        if (!player.isShiftKeyDown()) return;
        if (!player.getMainHandItem().isEmpty()) return;

        var pos = event.getPos();
        var level = player.level();
        var state = level.getBlockState(pos);

        if (state.getBlock() instanceof StorageBox) {
            if (state.getValue(StorageBox.FACING) != event.getHitVec().getDirection()) return;
        } else if (state.getBlock() instanceof SimpleStorageBox) {
            if (state.getValue(SimpleStorageBox.FACING) != event.getHitVec().getDirection()) return;
        } else {
            return;
        }

        // Open GUI on the main-hand pass; cancel both hands to prevent off-hand block placement
        if (event.getHand() == InteractionHand.MAIN_HAND && !level.isClientSide) {
            var entity = level.getBlockEntity(pos);
            if (entity instanceof StorageBoxEntity sbe) {
                NetworkHooks.openScreen((ServerPlayer) player, sbe, pos);
            } else if (entity instanceof SimpleStorageBoxEntity ssbe) {
                NetworkHooks.openScreen((ServerPlayer) player, ssbe, pos);
            }
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        if (!(event.getTarget() instanceof LivingEntity livingEntity)) return;
        if (!BackpackHelper.isWearingBackpack(player)) return;

        UpgradeEventDispatcher.dispatchAttackEntity(player, player.getUsedItemHand(), livingEntity);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Player player = event.player;

            ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
            BackpackContainer container = new BackpackContainer(player, backpack);

            JetpackHandler jetpackHandler = JetpackManager.getJetpackHandler(player);

            if (UpgradeHelper.hasActiveUpgrade(container, UpgradeType.FLIGHT)) {
                jetpackHandler.execute();
            } else if (jetpackHandler.isCleanupNeeded()) {
                // Account for deactivating flight upgrade while hovering
                player.setNoGravity(false);
                jetpackHandler.fadeOutVisualAirOverlay();
            }

            if (player.level().isClientSide || player.isSpectator() || !player.isAlive() || player.isSleeping() || player.isDeadOrDying())
                return;

            // NEW upgrade tick system
            IBackpackContainer tickContainer = (player.containerMenu instanceof BackpackMenu menu
                    && menu.type == BackpackMenu.BackpackType.WORN)
                    ? menu.container : container;
            for (IUpgrade upgrade : UpgradeRegistry.getAll()) {
                UpgradeContext ctx = UpgradeContext.forWornBackpack(player, backpack, tickContainer);
                upgrade.tick(ctx);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            CompoundTag oldData = event.getOriginal().getPersistentData();
            CompoundTag newData = event.getEntity().getPersistentData();

            if (oldData.contains(ConfigManager.FXNTSTORAGE_SETTINGS_TAG)) {
                newData.put(ConfigManager.FXNTSTORAGE_SETTINGS_TAG, Objects.requireNonNull(oldData.get(ConfigManager.FXNTSTORAGE_SETTINGS_TAG)));
            }
        }

        JetpackManager.addPlayer(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            stopJukeboxIfPlaying(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity instanceof Player player) {
            if (player.isSpectator() || player.level().isClientSide || !player.isAlive() || player.isSleeping() || player.isDeadOrDying())
                return;
            if (!BackpackHelper.isWearingBackpack(player)) return;
            if (UpgradeEventDispatcher.dispatchLivingFall(player, event)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            JetpackManager.addPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onServerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            JetpackManager.addPlayer(player);
            JukeboxHandler.syncBlocksToPlayers(player);

            CompoundTag persistent = player.getPersistentData();
            if (!persistent.contains(ConfigManager.FXNTSTORAGE_SETTINGS_TAG)) {
                persistent.put(ConfigManager.FXNTSTORAGE_SETTINGS_TAG, new CompoundTag());
            } else {
                CompoundTag modTag = persistent.getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG);

                int version = modTag.getInt("DataVersion");
                if (version < ConfigManager.CURRENT_DATA_VERSION) {
                    ConfigManager.migrateSettings(persistent, version);
                    modTag.putInt("DataVersion", ConfigManager.CURRENT_DATA_VERSION);
                    persistent.put(ConfigManager.FXNTSTORAGE_SETTINGS_TAG, modTag);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        playersUsingBackpackArrows.remove(player.getUUID());
        JetpackManager.removePlayer(player);
        JukeboxHandler.stopPlayer(player);
        TorchDeployerManager.removePlayer(player);
        if (BackpackHelper.isWearingBackpack(player)
                && UpgradeHelper.hasActiveUpgrade(new BackpackContainer(player, BackpackHelper.getEquippedBackpackStack(player)), UpgradeType.HEALTH)) {
            MechanicalHeartUpgrade.saveCurrentHealth(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        playersUsingBackpackArrows.clear();
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedEntities().removeIf(entity -> {
            if (!(entity instanceof ItemEntity itemEntity)) return false;
            Item item = itemEntity.getItem().getItem();
            return item instanceof BlockItem blockItem &&
                    (blockItem.getBlock() instanceof StorageBox ||
                            blockItem.getBlock() instanceof SimpleStorageBox);
        });
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = player.level();

        if (level.isClientSide || player.isCreative()) return;

        // Ore Mining and Torch Deployer
        if (!BackpackHelper.isWearingBackpack(player)) return;
        if (UpgradeEventDispatcher.dispatchBlockBreak(player, event)) {
            event.setCanceled(true);
        }
    }

    // Track players who should consume from backpack (no arrows in inventory)
    private static final Set<UUID> playersUsingBackpackArrows = new HashSet<>();

    @SubscribeEvent
    public static void onProjectileEvent(LivingGetProjectileEvent event) {
        if (event.getEntity() instanceof Player player
                && event.getProjectileWeaponItemStack().getItem() instanceof ProjectileWeaponItem weapon) {

            if (!event.getProjectileItemStack().isEmpty()) return;

            // Check Backpack
            boolean checkBackpack = player.level().isClientSide
                    ? ConfigManager.ClientConfig.CHECK_BACKPACK_FOR_PROJECTILES.get()
                    : ClientSettings.getBoolean(player.getUUID(), "CheckBackpackForProjectiles");

            if (!checkBackpack || !BackpackHelper.isWearingBackpack(player))
                return;

            ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
            IBackpackContainer backpackContainer = new BackpackContainer(player, backpack);
            IItemHandler itemHandler = backpackContainer.getItemHandler();

            Predicate<ItemStack> predicate = weapon.getAllSupportedProjectiles();

            // Check if player has arrows in inventory first
            boolean hasArrowsInInventory = false;
            for (ItemStack invStack : player.getInventory().items) {
                if (predicate.test(invStack)) {
                    hasArrowsInInventory = true;
                    break;
                }
            }

            // Only provide arrows from backpack if no arrows found in inventory
            if (!hasArrowsInInventory) {
                for (int i : layout.getItemsAndToolsRange()) {
                    ItemStack stack = itemHandler.getStackInSlot(i);

                    if (predicate.test(stack)) {
                        event.setProjectileItemStack(stack.copy());
                        // Mark that this player should consume from backpack when arrow is loosed
                        if (!player.level().isClientSide)
                            playersUsingBackpackArrows.add(player.getUUID());
                        break;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getEntity().level().isClientSide) return;

        Player player = event.getEntity();
        UUID playerUUID = player.getUUID();

        if (player.isCreative()) {
            playersUsingBackpackArrows.remove(playerUUID);
            return;
        }

        // Only proceed if this player was marked to use backpack arrows
        if (!playersUsingBackpackArrows.remove(playerUUID)) return;

        // Check if charge is sufficient
        if (event.getCharge() < 3) return;

        consumeArrowFromBackpack(player);
    }

    @SubscribeEvent
    public static void onCrossbowCharged(LivingEntityUseItemEvent.Stop event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack itemStack = event.getItem();
        if (!(itemStack.getItem() instanceof CrossbowItem)) return;

        // Check if the crossbow is now charged (loaded)
        if (event.getDuration() >= 0) return;

        UUID playerUUID = player.getUUID();

        // Only proceed if this player was marked to use backpack arrows
        if (!playersUsingBackpackArrows.remove(playerUUID)) return;

        if (player.isCreative()) return;

        if (player.level().isClientSide) {
            ModNetwork.sendToServer(new CrossbowChargedPacket());
        } else {
            consumeArrowFromBackpack(player);
        }
    }

    public static void consumeArrowFromBackpack(Player player) {
        if (!BackpackHelper.isWearingBackpack(player)) return;

        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        IBackpackContainer backpackContainer = new BackpackContainer(player, backpack);
        IItemHandlerModifiable itemHandler = backpackContainer.getItemHandler();

        ItemStack heldItem = player.getMainHandItem();
        ProjectileWeaponItem weapon = null;

        if (heldItem.getItem() instanceof ProjectileWeaponItem mainHand) {
            weapon = mainHand;
        } else {
            heldItem = player.getOffhandItem();
            if (heldItem.getItem() instanceof ProjectileWeaponItem offHand) {
                weapon = offHand;
            }
        }

        // Neither hand has a ProjectileWeaponItem equipped
        if (weapon == null) return;

        Predicate<ItemStack> predicate = weapon.getAllSupportedProjectiles();

        for (int i : layout.getItemsAndToolsRange()) {
            ItemStack stack = itemHandler.getStackInSlot(i);

            if (predicate.test(stack)) {
                stack.shrink(1);
                itemHandler.setStackInSlot(i, stack);
                if (!player.level().isClientSide)
                    backpackContainer.setDataChanged();
                break;
            }
        }
    }

    // --- StorageBoxMountedStorage & SimpleStorageBoxMountedStorage ---
    @SubscribeEvent
    public static void onEntityRemoved(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof AbstractContraptionEntity contraption) {
            ContraptionStorageFilters.cleanupContraption(contraption.getContraption());
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getSlot() != EquipmentSlot.CHEST) return;

        ItemStack oldStack = event.getFrom();
        ItemStack newStack = event.getTo();

        // Backpack was removed from chest slot
        if (isBackpack(oldStack) && !isBackpack(newStack)) {
            stopJukeboxIfPlaying(player);
            UpgradeEventDispatcher.dispatchBackpackUnequipped(player, oldStack);
        }

        // Backpack was equipped in the check slot
        if (!isBackpack(oldStack) && isBackpack(newStack)) {
            UpgradeEventDispatcher.dispatchBackpackEquipped(player, newStack);
        }
    }

    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getIdentifier().equals("back")) return;

        ItemStack oldStack = event.getFrom();
        ItemStack newStack = event.getTo();

        // Backpack removed from a back slot
        if (isBackpack(oldStack) && !isBackpack(newStack)) {
            stopJukeboxIfPlaying(player);
            UpgradeEventDispatcher.dispatchBackpackUnequipped(player, oldStack);
        }

        if (!isBackpack(oldStack) && isBackpack(newStack)) {
            UpgradeEventDispatcher.dispatchBackpackEquipped(player, newStack);
        }
    }

    private static boolean isBackpack(ItemStack stack) {
        return stack.is(ModTags.Items.BACKPACK_ITEM);
    }

    private static void stopJukeboxIfPlaying(ServerPlayer player) {
        if (JukeboxHandler.isPlayerPlaying(player)) {
            JukeboxHandler.stopPlayer(player);
        }
    }
}
