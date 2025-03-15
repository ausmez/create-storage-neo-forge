package net.fxnt.fxntstorage.util;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpacks.upgrades.BackpackOnBackUpgradeHandler;
import net.fxnt.fxntstorage.backpacks.upgrades.JetpackHandler;
import net.fxnt.fxntstorage.backpacks.upgrades.JetpackManager;
import net.fxnt.fxntstorage.backpacks.util.BackpackHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FXNTStorage.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventHandler {
    private static int slowTick = 0;
    private static int mediumTick = 0;
    private static final int slowTicks = 30;
    private static final int mediumTicks = 15;

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player == null || player.isCreative() || player.level().isClientSide() || !event.getAction().equals(PlayerInteractEvent.LeftClickBlock.Action.START))
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

    // TODO: Not convinced this is an efficient way to do this
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        boolean hasFlightUpgrade;

        if (event.phase == TickEvent.Phase.START) {
            Player player = event.player;

            if (player.level().isClientSide || player.isSpectator() || !player.isAlive() || player.isSleeping() || player.isDeadOrDying())
                return;

            // vvv SERVER-SIDE vvv //
            hasFlightUpgrade = new BackpackOnBackUpgradeHandler(player).hasUpgrade(Util.FLIGHT_UPGRADE);
            JetpackHandler jetpackHandler = JetpackManager.getJetpackHandler(player);

            if (hasFlightUpgrade) {
                jetpackHandler.execute();
            } else {
                // To account for deactivating flight upgrade while hovering
                jetpackHandler.endHovering(false);
                jetpackHandler.fadeOutVisualAirOverlay();
            }

            /* ServerPlayerTickMixin */
            if (!BackpackHelper.isWearingBackpack(player)) return;

            mediumTick++;
            slowTick++;
            if (mediumTick >= mediumTicks) {
                BackpackOnBackUpgradeHandler handler = new BackpackOnBackUpgradeHandler(player);
                handler.applyRefillUpgrade();
                handler.applyFeederUpgrade();
                mediumTick = 0;
            }
            if (slowTick >= slowTicks) {
                new BackpackOnBackUpgradeHandler(player).applyMagnetUpgrade();
                slowTick = 0;
            }

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
            FXNTStorage.LOGGER.debug("Adding player {} to the JetpackManager", event.getEntity().getUUID());
            JetpackManager.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onServerLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FXNTStorage.LOGGER.debug("Removing player {} from the JetpackManager", event.getEntity().getUUID());
            JetpackManager.onPlayerLeave(player);
        }
    }

}
