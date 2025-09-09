package net.fxnt.fxntstorage.util;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.main.BackpackContainer;
import net.fxnt.fxntstorage.backpack.main.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.BackpackOnBackUpgradeHandler;
import net.fxnt.fxntstorage.backpack.upgrade.JetpackHandler;
import net.fxnt.fxntstorage.backpack.upgrade.JetpackManager;
import net.fxnt.fxntstorage.backpack.upgrade.TorchDeployerManager;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.controller.StorageController;
import net.fxnt.fxntstorage.controller.StorageControllerEntity;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.network.packet.CrossbowChargedPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingGetProjectileEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = FXNTStorage.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {
    private static int slowTick = 0;
    private static int mediumTick = 0;
    private static final int slowTicks = 30;
    private static final int mediumTicks = 15;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // Storage Controller
        if (block instanceof StorageController controller) {
            if (!controller.hitFront(state, event.getHitVec())) return;

            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof StorageControllerEntity controllerEntity)) return;

            if (state.getValue(StorageController.CONNECTED)) {
                if (!level.isClientSide) {
                    controllerEntity.transferItemsFromPlayer(player);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player == null || player.isCreative() || player.level().isClientSide || !event.getAction().equals(PlayerInteractEvent.LeftClickBlock.Action.START))
            return;

        Level world = event.getLevel();
        InteractionHand hand = event.getHand();
        BlockPos pos = event.getPos();
        ItemStack tool = player.getMainHandItem();

        // Check if the selected item is tagged as a TOOL
        if (tool.is(Tags.Items.TOOLS) || tool.is(Tags.Items.SHEARS)) {
            new BackpackOnBackUpgradeHandler(player).fromAttackBlockEvent(player, world, hand, pos);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Level world = event.getEntity().getCommandSenderWorld();
        InteractionHand hand = event.getEntity().getUsedItemHand();
        ItemStack weapon = player.getMainHandItem().copy();
        Entity target = event.getTarget();

        // Check if the selected item is tagged as a TOOL/WEAPON
        if ((weapon.is(Tags.Items.TOOLS) || weapon.is(Tags.Items.SHEARS)) && target instanceof LivingEntity livingEntity) {
            new BackpackOnBackUpgradeHandler(player).fromAttackEntityEvent(player, world, hand, livingEntity);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Player player = event.player;

            if (!BackpackHelper.isWearingBackpack(player)) return;
            BackpackOnBackUpgradeHandler handler = new BackpackOnBackUpgradeHandler(player);

            JetpackHandler jetpackHandler = JetpackManager.getJetpackHandler(player);
            if (handler.hasUpgrade(Util.FLIGHT_UPGRADE)) {
                jetpackHandler.execute();
            } else {
                // Account for deactivating flight upgrade while hovering
                jetpackHandler.endHovering(false);
                jetpackHandler.fadeOutVisualAirOverlay();
            }

            if (player.level().isClientSide || player.isSpectator() || !player.isAlive() || player.isSleeping() || player.isDeadOrDying())
                return;

            /* Torch Deployer Upgrade */
            if (handler.hasUpgrade(Util.TORCHDEPLOYER_UPGRADE)) handler.applyTorchDeployerUpgrade(player);

            mediumTick++;
            slowTick++;
            if (mediumTick >= mediumTicks) {
                handler.applyRefillUpgrade();
                handler.applyFeederUpgrade();
                mediumTick = 0;
            }
            if (slowTick >= slowTicks) {
                handler.applyMagnetUpgrade();
                slowTick = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();

        if (oldData.contains(ConfigManager.FXNTSTORAGE_SETTINGS_TAG)) {
            newData.put(ConfigManager.FXNTSTORAGE_SETTINGS_TAG, Objects.requireNonNull(oldData.get(ConfigManager.FXNTSTORAGE_SETTINGS_TAG)));
        }

        JetpackManager.addPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity instanceof Player player) {
            if (player.isSpectator() || player.level().isClientSide || !player.isAlive() || player.isSleeping() || player.isDeadOrDying())
                return;
            if (!BackpackHelper.isWearingBackpack(player)) return;
            if (new BackpackOnBackUpgradeHandler(player).applyFallDamageUpgrade()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onServerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            JetpackManager.addPlayer(player);
            if (player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).isEmpty()) {
                player.getPersistentData().put(ConfigManager.FXNTSTORAGE_SETTINGS_TAG, new CompoundTag());
            }
        }
    }

    @SubscribeEvent
    public static void onServerLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            JetpackManager.removePlayer(player);
            TorchDeployerManager.removePlayer(player);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = player.level();

        if (level.isClientSide && player.isCreative()) return;

        BackpackOnBackUpgradeHandler handler = new BackpackOnBackUpgradeHandler(player);

        // Torch Deployer Cooldown
        if (event.getState().is(Blocks.TORCH) && handler.hasUpgrade(Util.TORCHDEPLOYER_UPGRADE)) {
            TorchDeployerManager.resetCooldown(player);
        }

        // Ore Mining
        if (!handler.hasUpgrade(Util.OREMINING_UPGRADE)) return;

        ItemStack tool = player.getMainHandItem();
        if (tool.isCorrectToolForDrops(event.getState()) || event.getState().is(ModTags.Blocks.BREAKABLE_WITH_ANY_TOOL)) {
            if (ConfigManager.CommonConfig.OREMINE_ORES_ONLY.get() && !event.getState().is(ModTags.Blocks.ORE_MINING_BLOCK)) {
                return;
            }

            boolean mineAllBlocks = player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).getBoolean("MineAllBlocks");

            handler.applyOreMiningUpgrade(level, event.getPos(), player, mineAllBlocks, 64);
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
                    : player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).getBoolean("CheckBackpackForProjectiles");

            if (!checkBackpack || !BackpackHelper.isWearingBackpack(player))
                return;

            ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
            IBackpackContainer backpackContainer = new BackpackContainer(backpack, player);
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
                for (int i = Util.ITEM_SLOT_START_RANGE; i < Util.TOOL_SLOT_END_RANGE; i++) {
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
        IBackpackContainer backpackContainer = new BackpackContainer(backpack, player);
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

        for (int i = Util.ITEM_SLOT_START_RANGE; i < Util.TOOL_SLOT_END_RANGE; i++) {
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

}
