package net.fxnt.fxntstorage.util;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.upgrade.BackpackOnBackUpgradeHandler;
import net.fxnt.fxntstorage.backpack.upgrade.JetpackHandler;
import net.fxnt.fxntstorage.backpack.upgrade.JetpackManager;
import net.fxnt.fxntstorage.backpack.upgrade.TorchDeployerManager;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Objects;

@EventBusSubscriber(modid = FXNTStorage.MOD_ID)
public class EventHandler {
    private static int slowTick = 0;
    private static int mediumTick = 0;
    private static final int slowTicks = 30;
    private static final int mediumTicks = 15;

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player.isCreative() || player.level().isClientSide() || !event.getAction().equals(PlayerInteractEvent.LeftClickBlock.Action.START))
            return;

        Level world = event.getLevel();
        InteractionHand hand = event.getHand();
        BlockPos pos = event.getPos();
        ItemStack tool = player.getMainHandItem();

        // Check if the selected item is tagged as a TOOL
        if (tool.is(Tags.Items.TOOLS) || tool.is(Tags.Items.TOOLS_SHEAR)) {
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
        if ((weapon.is(Tags.Items.TOOLS) || weapon.is(Tags.Items.TOOLS_SHEAR)) && target instanceof LivingEntity livingEntity) {
            new BackpackOnBackUpgradeHandler(player).fromAttackEntityEvent(player, world, hand, livingEntity);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();

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

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();

        if (oldData.contains(ConfigManager.FXNTSTORAGE_SETTINGS_TAG)) {
            newData.put(ConfigManager.FXNTSTORAGE_SETTINGS_TAG, Objects.requireNonNull(oldData.get(ConfigManager.FXNTSTORAGE_SETTINGS_TAG)));
        }
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
            JetpackManager.onPlayerJoin(player);
            if (player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).isEmpty()) {
                player.getPersistentData().put(ConfigManager.FXNTSTORAGE_SETTINGS_TAG, new CompoundTag());
            }
        }
    }

    @SubscribeEvent
    public static void onServerLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            JetpackManager.onPlayerLeave(player);
            TorchDeployerManager.onPlayerLeave(player);
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

}
