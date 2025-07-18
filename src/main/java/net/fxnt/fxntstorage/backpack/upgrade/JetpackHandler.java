package net.fxnt.fxntstorage.backpack.upgrade;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.fxnt.fxntstorage.backpack.main.BackpackContainer;
import net.fxnt.fxntstorage.backpack.main.BackpackMenu;
import net.fxnt.fxntstorage.backpack.main.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.VisualJetpackAirPacket;
import net.fxnt.fxntstorage.util.ParticleHelper;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JetpackHandler {
    private final Player player;
    public boolean isHovering = false;
    public double hoverHeight = 0;
    private float jetPackFuelRemaining;
    private long lastRuntime = 0;

    private static final int maxAllowedHeight = 28;
    private static final double gravity = -0.44; //-1.7;

    private long airGaugeLastCleared = 0;
    private boolean airGaugeCleared = false;

    private boolean hasJumpedFromGround = false;
    private boolean isJumping = false;


    public JetpackHandler(Player player) {
        this.player = player;
    }

    public void execute() {
        if (player != null) {
            boolean isClientSide = player.level().isClientSide();

            jetPackFuelRemaining = (float) calculateJetPackFuel(player);

            if (player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).getBoolean("JetpackFlying")) { // Space bar has been pressed
                isJumping = true;

                // If we are here, the player IS JUMPING!
                if (jetPackFuelRemaining <= 0) {
                    endHovering(false);
                    return;
                }

                player.setNoGravity(true);
                player.resetFallDistance();

                if (player.isShiftKeyDown()) {
                    startHovering(true);
                } else if (isHovering) {
                    endHovering(true);
                }

                updateJetpackMovement();
                if (!isClientSide) {
                    depleteJetPackFuel();
                    if (BackpackHelper.isWearingBackpack(player, true)) {
                        ParticleHelper.jetPackParticles(player);
                    }
                }

                if (player.onGround() && isHovering) {
                    deactivateHovering();
                }

                if (player.onGround() && !hasJumpedFromGround) {
                    if (!isClientSide) {
                        player.playNotifySound(AllSoundEvents.STEAM.getMainEvent(), SoundSource.PLAYERS, 0.1f, 1.0f);
                    }
                    hasJumpedFromGround = true;
                }

                if (!player.onGround()) {
                    hasJumpedFromGround = false;
                }

            } else { // Space bar has been released
                isJumping = false;

                if (isHovering) {

                    if (player.onGround()) endHovering(true);

                    if (jetPackFuelRemaining <= 0) {
                        endHovering(false);
                        return;
                    }

                    updateJetpackMovement();
                    if (!isClientSide) {
                        depleteJetPackFuel();
                        if (BackpackHelper.isWearingBackpack(player, true)) {
                            ParticleHelper.jetPackParticles(player);
                        }
                    }

                } else {
                    player.setNoGravity(false);

                    // If player is on the ground and air gauge hasn't been cleared, start a timer
                    fadeOutVisualAirOverlay();
                }
            }
        }
    }

    public void fadeOutVisualAirOverlay() {
        if (player.level().isClientSide()) return;

        if (player.onGround() && !airGaugeCleared) {
            if (airGaugeLastCleared == 0) airGaugeLastCleared = System.currentTimeMillis();

            long currentTime = System.currentTimeMillis();
            if (currentTime - airGaugeLastCleared >= 1250) {
                // Send network packet to clear the air gauge, after ~1.25 secs
                ModNetwork.sendToPlayer((ServerPlayer) player, new VisualJetpackAirPacket(-1));
                airGaugeCleared = true;
            }
        } else {
            airGaugeLastCleared = 0;
        }
    }

    private static int getDistanceToGround(@NotNull Player player) {
        BlockPos blockPos = player.blockPosition();
        int y = player.getBlockY();
        int distance = 0;

        for (int i = y; i >= -64; i--) {
            BlockPos pos = blockPos.atY(i);
            if (!player.level().getBlockState(pos).isAir()) {
                break; // Stop when a non-air block is encountered
            }
            distance++;
        }
        return distance;
    }

    public void toggleHover() {
        if (player.onGround()) {
            endHovering(false);
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("JetpackHover", false);
            return;
        }
        if (!isHovering) {
            startHovering(true);
        } else {
            player.setNoGravity(false);
            endHovering(true);
        }
        player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("JetpackHover", isHovering);
    }

    public void deactivateHovering() {
        endHovering(true);
    }

    public void startHovering(boolean announce) {
        isHovering = true;
        hoverHeight = player.getY();
        if (announce) displayHoverMessage(true);
    }

    public void endHovering(boolean announce) {
        isHovering = false;
        hoverHeight = 0;
        if (announce) displayHoverMessage(false);
    }

    private void displayHoverMessage(boolean isStarting) {
        boolean isElytraBoost = player.getItemBySlot(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA)
                && player.isFallFlying() && ConfigManager.CommonConfig.ELYTRA_BOOST_ENABLED.get();
        String messageKey = isStarting
                ? (isElytraBoost ? "item.fxntstorage.jetpack.elytra_boost_enabled" : "item.fxntstorage.jetpack.hover_enabled")
                : (isElytraBoost ? "item.fxntstorage.jetpack.elytra_boost_disabled" : "item.fxntstorage.jetpack.hover_disabled");

        player.displayClientMessage(Component.translatable(messageKey), true);
    }

    public void updateJetpackMovement() {
        Vec3 lookVec = player.getLookAngle();
        Vec3 flatLookDirection = new Vec3(lookVec.x, 0, lookVec.z);

        if (flatLookDirection.lengthSqr() < 1.0E-6) {
            float yaw = player.getYRot() * ((float) Math.PI / 180F);
            flatLookDirection = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        flatLookDirection = flatLookDirection.normalize();

        Vec3 strafeDirection = flatLookDirection.cross(new Vec3(0, 1, 0)).normalize();

        double forward = player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).getFloat("JetpackForward");
        double strafe = player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).getFloat("JetpackLeft");

        double forwardWeight = isHovering ? 1.0 : 1.5;
        double strafeWeight = isHovering ? 0.4 : 0.6;
        Vec3 movementDirection = flatLookDirection.scale(forward * forwardWeight).add(strafeDirection.scale(strafe * strafeWeight));

        // Apply acceleration and max speed
        double acceleration = isHovering ? 0.05 : 0.25;

        double horizontalSpeed = calculateHorizontalSpeed();
        Vec3 horizontalVelocity = applyMovementPhysics(player.getDeltaMovement(), movementDirection.normalize(), acceleration, horizontalSpeed);

        double flySpeed = calculateVerticalSpeed();
        double verticalHoverSpeed = isHovering ? calculateVerticalHoveringSpeed(hoverHeight) : 0;
        double verticalSpeed = player.getDeltaMovement().y;

        int distanceToGround = getDistanceToGround(player);

        if ((jetPackFuelRemaining < 10.0f || distanceToGround > maxAllowedHeight) && isJumping) {
            // Prevent player going up (Slow fall speed)
            verticalSpeed = lerp(verticalSpeed, gravity / 10, 0.5);
        } else if (isHovering && Math.abs(verticalSpeed) > 0.1) {
            // Slow to Hover Speed (0)
            verticalSpeed = lerp(verticalSpeed, verticalHoverSpeed, 0.5);
        } else if (isHovering) {
            player.resetFallDistance();
            verticalSpeed = verticalHoverSpeed;
        } else {
            // Normal Flight
            verticalSpeed = flySpeed;
        }

        // Apply the new movement velocity to the player
        if (player.isSwimming() && isJumping) {
            player.setDeltaMovement(new Vec3(player.getDeltaMovement().x() + player.getLookAngle().x * 0.08, player.getDeltaMovement().y() + player.getLookAngle().y * 0.08, player.getDeltaMovement().z() + player.getLookAngle().z * 0.08));
        } else if (player.isFallFlying() && ConfigManager.CommonConfig.ELYTRA_BOOST_ENABLED.get()) {
            // Elytra boost
            if (Math.abs(player.getDeltaMovement().x) < 1.0)
                player.setDeltaMovement(player.getDeltaMovement().x + player.getLookAngle().x * 0.04, player.getDeltaMovement().y, player.getDeltaMovement().z);
            if (Math.abs(player.getDeltaMovement().y) < 1.5)
                player.setDeltaMovement(player.getDeltaMovement().x, player.getDeltaMovement().y + player.getLookAngle().y * 0.1, player.getDeltaMovement().z);
            if (Math.abs(player.getDeltaMovement().z) < 1.0)
                player.setDeltaMovement(player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z + player.getLookAngle().z * 0.04);
        } else {
            player.setDeltaMovement(horizontalVelocity.x, verticalSpeed, horizontalVelocity.z);
        }

        if (!player.level().isClientSide) {
            ServerPlayer sp = (ServerPlayer) player;
            sp.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    private Vec3 applyMovementPhysics(@NotNull Vec3 currentVelocity, @NotNull Vec3 direction, double acceleration, double maxSpeed) {
        Vec3 targetVelocity = currentVelocity.add(direction.scale(acceleration));
        double speed = Math.sqrt(targetVelocity.x * targetVelocity.x + targetVelocity.z * targetVelocity.z);

        // If horizontal speed exceeds maxSpeed, scale it down to maxSpeed
        if (speed > maxSpeed) {
            double scale = maxSpeed / speed;
            targetVelocity = new Vec3(targetVelocity.x * scale, currentVelocity.y, targetVelocity.z * scale);  // Keep the current Y velocity (gravity)
        }

        // Return the final velocity, which may be clamped based on maxSpeed
        return targetVelocity;
    }

    private double calculateHorizontalSpeed() {
        double baseFlySpeedBoost = 0.375;
        double defaultPlayerSneakSpeed = 0.08;
        double defaultPlayerWaterSpeed = 0.08;
        double defaultPlayerSprintSpeed = 0.13;
        double defaultPlayerWalkSpeed = 0.10;
        double baseHoverSpeedBoost = 0.25;

        // Get relevant speed multipliers
        double enchantedSpeedMultiplier = EnchantmentHelper.getSneakingSpeedBonus(player);
        double mobEffectSpeedMultiplier = player.hasEffect(MobEffects.MOVEMENT_SPEED) ? Objects.requireNonNull(player.getEffect(MobEffects.MOVEMENT_SPEED)).getAmplifier() : 0.0;

        // Determine base speed based on player state
        double baseSpeed = defaultPlayerWalkSpeed;

        if (player.isInWater() && !player.isSwimming()) {
            return player.isSwimming() ? defaultPlayerWaterSpeed * 0.5 : defaultPlayerWaterSpeed;
        }

        if (player.isSprinting()) {
            baseSpeed = defaultPlayerSprintSpeed;  // Sprinting speed
        } else if (isHovering) {
            baseSpeed = defaultPlayerSneakSpeed;  // Hovering speed
        }

        // Apply boosts and effects
        double horizontalSpeed = baseSpeed + baseFlySpeedBoost + (mobEffectSpeedMultiplier / 10);

        if (isHovering) {
            horizontalSpeed = baseSpeed + baseHoverSpeedBoost + enchantedSpeedMultiplier + (mobEffectSpeedMultiplier / 10);
        }

        return horizontalSpeed;
    }

    private double calculateVerticalSpeed() {
        double thrust = 0.42;

        double currentVerticalSpeed = player.getDeltaMovement().y;
        double dampingFactor = (currentVerticalSpeed < 0 && !isJumping) ? 0.05 : 0.15;

        // Define the target vertical speed based on player status
        double verticalTarget = gravity;

        if (isJumping) {
            verticalTarget = thrust;  // Thrust value for upward movement when jumping
        } else if (player.isSwimming()) {
            verticalTarget = 0;  // No vertical movement while swimming
        } else if (player.isInWater()) {
            verticalTarget = gravity / 16;  // Custom gravity in water
        }

        // Apply damping to the vertical speed
        return (player.isSwimming()) ? 0 : currentVerticalSpeed * (1 - dampingFactor) + verticalTarget * dampingFactor;
    }

    private double calculateVerticalHoveringSpeed(double targetHeight) {
        boolean bobbingEnabled = player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG)
                .getBoolean("JetpackHoverBobbing");
        double currentHeight = player.getY();
        double heightDifference = targetHeight - currentHeight;

        double bobbing = 0;
        if (bobbingEnabled) {
            double bobbingFrequency = player.isInWater() ? 0.15 : 0.3;
            double bobbingAmplitude = 0.2;
            double cycleDuration = 8000;
            long timeInMillis = System.currentTimeMillis();
            bobbing = Math.sin(2 * Math.PI * bobbingFrequency * (timeInMillis % cycleDuration) / 200);
            bobbing *= bobbingAmplitude;
        }

        double P = 0.5;
        double D = 0.1;

        return P * heightDifference + D * (heightDifference - bobbing);
    }

    private double lerp(double start, double end, double factor) {
        return start + factor * (end - start);
    }

    public static double calculateJetPackFuel(Player player) {
        // Check if backpack is equipped, no backpack == no flight upgrade
        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        if (backpack.isEmpty()) return 0.0;

        List<ItemStack> backtanks = getBacktanksFromPlayer(player, backpack);

        return Math.round(backtanks.stream().map(BacktankUtil::getAir).reduce(0f, Float::sum));
    }

    public void depleteJetPackFuel() {
        if (player.isCreative()) return;
        doDepleteJetPackFuel(player);
    }

    private void doDepleteJetPackFuel(Player player) {
        if (player.level().isClientSide()) return;

        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        Level world = player.level();
        long currRuntime = world.getGameTime();
        int elytraMultiplier = ConfigManager.CommonConfig.ELYTRA_BOOST_MULTIPLIER.get();

        if (player.isFallFlying() && ConfigManager.CommonConfig.ELYTRA_BOOST_ENABLED.get()) {
            if (lastRuntime != 0 && currRuntime - lastRuntime < (20 / elytraMultiplier)) return;
        } else {
            // Has 1 second (20 ticks) passed in the game?
            if (lastRuntime != 0 && currRuntime - lastRuntime < 20) return;
        }
        lastRuntime = currRuntime;

        // Retrieve air tanks from chest and backpack
        List<ItemStack> backtanks = getBacktanksFromPlayer(player, backpack);

        // Sanity check
        if (backtanks.isEmpty()) return;

        // Sort tanks by remaining air pressure in ascending order
        backtanks.sort((a, b) -> Float.compare(BacktankUtil.getAir(a), BacktankUtil.getAir(b)));

        int totalAir = Math.round(backtanks.stream().map(BacktankUtil::getAir).reduce(0f, Float::sum));
        ModNetwork.sendToPlayer((ServerPlayer) player, new VisualJetpackAirPacket(totalAir));
        airGaugeCleared = false;

        // If we are here, we assume we have at least 1 tank with some air
        CompoundTag tag = backtanks.get(0).getOrCreateTag();
        int maxAir = BacktankUtil.maxAir(backtanks.get(0));
        float air = BacktankUtil.getAir(backtanks.get(0));
        float newAir = Math.max(air - 1, 0);
        tag.putFloat("Air", Math.min(newAir, maxAir));
        backtanks.get(0).setTag(tag);

        // Send warning at 90 seconds then alert at 1 second
        sendFuelWarning(player, totalAir, totalAir - 1, 90);
        sendFuelWarning(player, totalAir, totalAir - 1, 1);
    }

    private static IBackpackContainer getBackpackContainer(Player player, ItemStack backpack) {
        if (player.containerMenu instanceof BackpackMenu backpackMenu && backpackMenu.backpackType == Util.BACKPACK_ON_BACK) {
            return backpackMenu.container;
        } else {
            return new BackpackContainer(backpack, player);
        }
    }

    private static List<ItemStack> getBacktanksFromPlayer(Player player, ItemStack backpack) {
        List<ItemStack> backtanks = new ArrayList<>();

        // Check chest item
        ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestItem.isEmpty() && AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.matches(chestItem) && BacktankUtil.hasAirRemaining(chestItem)) {
            backtanks.add(chestItem);
        }

        // Check backpack items
        if (!backpack.isEmpty()) {
            IBackpackContainer container = getBackpackContainer(player, backpack);
            ItemStackHandler inventory = container.getItemHandler();

            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack slotItem = inventory.getStackInSlot(i);
                if (AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.matches(slotItem) && BacktankUtil.hasAirRemaining(slotItem)) {
                    backtanks.add(slotItem);
                }
            }
        }

        return backtanks;
    }

    private static void sendFuelWarning(Player player, float air, float newAir, float threshold) {
        if (newAir > threshold)
            return;
        if (air <= threshold)
            return;

        boolean depleted = threshold == 1;
        MutableComponent component = Component.translatable(depleted ? "item.fxntstorage.jetpack.fuel_depleted" : "item.fxntstorage.jetpack.fuel_low");

        AllSoundEvents.DENY.play(player.level(), null, player.blockPosition(), 1, 1.25f);
        AllSoundEvents.STEAM.play(player.level(), null, player.blockPosition(), .5f, .5f);

        ServerPlayer sp = (ServerPlayer) player;
        sp.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 10));
        sp.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("⚠ ").withStyle(depleted ? ChatFormatting.RED : ChatFormatting.GOLD)
                        .append(component.withStyle(ChatFormatting.GRAY))));
        sp.connection.send(new ClientboundSetTitleTextPacket(CommonComponents.EMPTY));
    }

    public static void processPlayerInputPacket(ServerPlayer player, float forward, float left) {
        if (player != null) {
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putFloat("JetpackForward", forward);
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putFloat("JetpackLeft", left);
        }
    }

    public static void flyingOnKeyPress(ServerPlayer player) {
        if (player != null) {
            if (new BackpackOnBackUpgradeHandler((Player) player).hasUpgrade(Util.FLIGHT_UPGRADE)) {
                player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("JetpackFlying", true);
            }
        }
    }

    public static void flyingOnKeyRelease(ServerPlayer player) {
        if (player != null) {
            player.getPersistentData().getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG).putBoolean("JetpackFlying", false);
        }
    }
}
