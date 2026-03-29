package net.fxnt.fxntstorage.backpack.upgrade.jetpack;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.inventory.BackpackContainer;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataManager;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeDataSync;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.network.packet.JetpackFuelSyncPacket;
import net.fxnt.fxntstorage.network.packet.VisualJetpackAirPacket;
import net.fxnt.fxntstorage.util.ParticleHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JetpackHandler {
    private static final int MAX_ALLOWED_HEIGHT = 32;
    private static final double GRAVITY = -0.44;
    private static final long FUEL_SYNC_INTERVAL = 1000; // 1 sec
    private static final long MAX_INTERPOLATION_TIME = 200; // 200ms
    private static final int MAX_MISSED_PACKETS = 5;

    private final Player player;
    private double hoverHeight = 0;
    private float jetPackFuelRemaining;
    private long lastRuntime = 0;
    private long airGaugeLastCleared = 0;
    private boolean airGaugeCleared = false;
    private boolean isJumping = false;
    private boolean isHovering = false;
    private float forward = 0f;
    private float left = 0f;
    private boolean playedSoundThisJump = false;

    private float predictedFuelRemaining;
    private long lastFuelSync = 0;
    private Vec3 lastValidVelocity = Vec3.ZERO;
    private long lastServerUpdate = 0;
    private double lastKnownY = 0;
    private static final double TELEPORT_THRESHOLD = 5.0; // Blocks

    private long lastPacketTime = 0;
    private int missedPackets = 0;

    private IBackpackContainer itemHandler;

    public JetpackHandler(Player player) {
        this.player = player;
    }

    public void execute() {
        if (player == null) return;

        if (player.isPassenger()) {
            isJumping = false;
            endHovering(false);
            fadeOutVisualAirOverlay();
            return;
        }

        if (player.level().isClientSide) {
            executeClient();
        } else {
            executeServer();
        }
    }

    private void executeClient() {
        updatePredictedFuel();

        if (isHovering && wasTeleported()) {
            hoverHeight = player.getY();
        }

        if (isJumping) {
            if (predictedFuelRemaining <= 0) {
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

            updateClientMovementWithInterpolation();

            if (player.onGround() && isHovering) {
                endHovering(true);
            }

            if (!player.isInWater() && !player.isInLava()) {
                handleThrustSound();
            }

        } else {
            playedSoundThisJump = !player.onGround();

            if (isHovering) {
                if (player.onGround()) {
                    endHovering(true);
                }

                if (predictedFuelRemaining <= 0) {
                    endHovering(false);
                    return;
                }

                updateClientMovementWithInterpolation();
            } else {
                player.setNoGravity(false);
                fadeOutVisualAirOverlay();
            }
        }
    }

    private void executeServer() {
        if (isHovering && wasTeleported()) {
            hoverHeight = player.getY();
        }

        jetPackFuelRemaining = (float) calculateJetPackFuel(player);

        if ((isJumping || isHovering) && jetPackFuelRemaining > 0) {
            player.setNoGravity(true);
            depleteJetPackFuel(player);
            validatePlayerMovement();
            player.resetFallDistance();

            if (player.tickCount % 5 == 0) {
                validateAndCorrectPosition();
            }

            if (BackpackHelper.isWearingBackpack(player, true)) {
                ParticleHelper.jetpackParticles(player);
            }
        } else {
            player.setNoGravity(false);
            if (isHovering && jetPackFuelRemaining <= 0) {
                endHovering(false);
            }
        }

        syncFuelToClient();
        fadeOutVisualAirOverlay();
    }

    private boolean wasTeleported() {
        double yDifference = Math.abs(player.getY() - lastKnownY);
        boolean teleported = yDifference > TELEPORT_THRESHOLD && lastKnownY != 0;
        lastKnownY = player.getY();
        return teleported;
    }

    private void updatePredictedFuel() {
        long currentTime = player.level().getGameTime();

        if (lastFuelSync == 0 || currentTime - lastFuelSync > FUEL_SYNC_INTERVAL * 2) {
            // Haven't received fuel update in a while, use last known value
            predictedFuelRemaining = jetPackFuelRemaining;
        } else if (isJumping) {
            // Predict fuel consumption based on time since last sync
            long timeSinceSync = currentTime - lastFuelSync;
            float estimatedConsumption = (float) (timeSinceSync / 1000.0); // 1 sec
            predictedFuelRemaining = Math.max(jetPackFuelRemaining - estimatedConsumption, 0);
        }
    }

    public void onFuelSync(float serverFuel, long serverTime) {
        jetPackFuelRemaining = serverFuel;
        predictedFuelRemaining = serverFuel;
        lastFuelSync = player.level().getGameTime();

        // Calculate latency
        long clientTime = player.level().getGameTime();
        long estimatedLatency = Math.max(0, clientTime - serverTime);

        // Adjust prediction based on latency
        if (isJumping && estimatedLatency > 0) {
            float latencyConsumption = estimatedLatency / 1000.0f;
            predictedFuelRemaining = Math.max(serverFuel - latencyConsumption, 0);
        }

        // Reset packet loss counter
        missedPackets = 0;
        lastPacketTime = clientTime;
    }

    private void validatePlayerMovement() {
        Vec3 velocity = player.getDeltaMovement();

        double maxHorizontalSpeed = 2.0;
        double maxVerticalSpeed = 1.0;

        boolean needsCorrection = false;
        double newX = velocity.x;
        double newY = velocity.y;
        double newZ = velocity.z;

        // Check horizontal speed
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed > maxHorizontalSpeed) {
            double scale = maxHorizontalSpeed / horizontalSpeed;
            newX *= scale;
            newZ *= scale;
            needsCorrection = true;
        }

        // Check vertical speed
        if (Math.abs(velocity.y) > maxVerticalSpeed) {
            newY = Math.copySign(maxVerticalSpeed, velocity.y);
            needsCorrection = true;
        }

        if (needsCorrection) {
            player.setDeltaMovement(newX, newY, newZ);
        }
    }

    private void validateAndCorrectPosition() {
        double distanceToGround = getDistanceToGround(player);
        if (distanceToGround > MAX_ALLOWED_HEIGHT + 5) { // 5 block buffer
            Vec3 velocity = player.getDeltaMovement();
            player.setDeltaMovement(velocity.x, Math.min(velocity.y, -0.1), velocity.z); // Gradually pull player down
        }
    }

    private void handleThrustSound() {
        if (player.onGround()) {
            playedSoundThisJump = false;
        } else if (!playedSoundThisJump) {
            if (player.level().isClientSide) {
                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                        AllSoundEvents.STEAM.getMainEvent(), SoundSource.PLAYERS, 0.1f, 1.0f, false);
            }
            playedSoundThisJump = true;
        }
    }

    public void fadeOutVisualAirOverlay() {
        boolean grounded = player.onGround() || player.isPassenger();
        if (grounded && !airGaugeCleared) {
            if (airGaugeLastCleared == 0) {
                airGaugeLastCleared = player.level().getGameTime();
            }
            long currentTime = player.level().getGameTime();
            if (currentTime - airGaugeLastCleared >= 25) {
                if (!player.level().isClientSide) {
                    // Send network packet to clear the air gauge, after ~1.25 secs
                    PacketDistributor.sendToPlayer((ServerPlayer) player, new VisualJetpackAirPacket(-1));
                }
                airGaugeCleared = true;
            }
        } else if (!grounded || !isJumping && !isHovering) {
            airGaugeLastCleared = 0;
        }
    }

    private static double getDistanceToGround(@NotNull Player player) {
        Vec3 start = player.position();
        Vec3 end = start.add(0, -player.getBlockY() - 256, 0);

        BlockHitResult hitResult = player.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                player
        ));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return start.y - hitResult.getLocation().y;
        }
        return (ConfigManager.ServerConfig.JETPACK_ALLOW_VOID_FLIGHT.get()) ? -1 : Double.MAX_VALUE;
    }

    public void toggleHover() {
        if (player.onGround()) {
            endHovering(false);
            return;
        }
        if (!isHovering) {
            startHovering(true);
        } else {
            player.setNoGravity(false);
            endHovering(true);
        }
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
        if (player.onGround()) airGaugeLastCleared = 0;
    }

    private void displayHoverMessage(boolean isStarting) {
        boolean isElytraBoost = player.getItemBySlot(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA)
                && player.isFallFlying() && ConfigManager.ServerConfig.ELYTRA_BOOST_ENABLED.get();
        String messageKey = isStarting
                ? (isElytraBoost ? "item.fxntstorage.jetpack.elytra_boost_enabled" : "item.fxntstorage.jetpack.hover_enabled")
                : (isElytraBoost ? "item.fxntstorage.jetpack.elytra_boost_disabled" : "item.fxntstorage.jetpack.hover_disabled");

        player.displayClientMessage(Component.translatable(messageKey), true);
    }

    private void updateClientMovementWithInterpolation() {
        long currentTime = player.level().getGameTime();
        if (lastPacketTime > 0 && currentTime - lastPacketTime > 100) { // 100ms threshold
            missedPackets++;
        }

        // Calculate base movement
        Vec3 calculatedVelocity = calculateMovementVelocity();

        // Apply interpolation if we have recent server data
        if (shouldUseInterpolation()) {
            calculatedVelocity = applyVelocityInterpolation(calculatedVelocity);
        }

        applyVelocityToPlayer(calculatedVelocity);
    }

    private boolean shouldUseInterpolation() {
        long timeSinceServerUpdate = player.level().getGameTime() - lastServerUpdate;
        return timeSinceServerUpdate < MAX_INTERPOLATION_TIME &&
                missedPackets < MAX_MISSED_PACKETS &&
                !lastValidVelocity.equals(Vec3.ZERO);
    }

    private Vec3 applyVelocityInterpolation(Vec3 calculatedVelocity) {
        long currentTime = player.level().getGameTime();
        long timeSinceUpdate = currentTime - lastServerUpdate;

        if (timeSinceUpdate < MAX_INTERPOLATION_TIME) {
            float interpolationAlpha = Math.min(1.0f, timeSinceUpdate / (float) MAX_INTERPOLATION_TIME);

            Vec3 interpolatedVelocity = lastValidVelocity.lerp(calculatedVelocity, interpolationAlpha);

            // Add some smoothing for sudden changes
            double velocityDifference = calculatedVelocity.distanceTo(lastValidVelocity);
            if (velocityDifference > 0.5) { // Large change, apply gradual transition
                float smoothingFactor = Math.min(0.3f, 1.0f / (missedPackets + 1));
                return lastValidVelocity.lerp(calculatedVelocity, smoothingFactor);
            }

            return interpolatedVelocity;
        }

        return calculatedVelocity;
    }

    private Vec3 calculateMovementVelocity() {
        Vec3 lookVec = player.getLookAngle();
        Vec3 flatLookDirection = new Vec3(lookVec.x, 0, lookVec.z);
        Vec3 strafeDirection = flatLookDirection.cross(new Vec3(0, 1, 0)).normalize();

        double forwardWeight = isHovering ? 1.0 : 1.5;
        double leftWeight = isHovering ? 0.2 : 0.6;
        Vec3 movementDirection = flatLookDirection.scale(forward * forwardWeight).add(strafeDirection.scale(left * leftWeight));

        double horizontalSpeed = calculateHorizontalSpeed();
        double acceleration = isHovering ? 0.02 * (horizontalSpeed / (0.08 + 0.25)) : 0.25;
        Vec3 horizontalVelocity = applyMovementPhysics(player.getDeltaMovement(), movementDirection.normalize(), acceleration, horizontalSpeed);

        double verticalSpeed;
        if (isHovering) {
            verticalSpeed = calculateVerticalHoveringSpeed(hoverHeight);
        } else {
            verticalSpeed = calculateVerticalSpeed();
        }

        return new Vec3(horizontalVelocity.x, verticalSpeed, horizontalVelocity.z);
    }

    private void applyVelocityToPlayer(Vec3 velocity) {
        float fuelToCheck = player.level().isClientSide ? predictedFuelRemaining : jetPackFuelRemaining;
        double distanceToGround = getDistanceToGround(player);

        if ((fuelToCheck < 10.0f || distanceToGround > MAX_ALLOWED_HEIGHT) && isJumping) {
            velocity = new Vec3(velocity.x, Mth.lerp(velocity.y, GRAVITY / 10, 0.5), velocity.z);
        }

        // Store last valid velocity for interpolation
        if (!player.level().isClientSide) {
            lastValidVelocity = velocity;
            lastServerUpdate = player.level().getGameTime();
        }

        // Apply to player
        if (player.isSwimming() && isJumping) {
            Vec3 lookDirection = player.getLookAngle();
            Vec3 deltaMovement = player.getDeltaMovement();
            player.setDeltaMovement(
                    deltaMovement.x + lookDirection.x * 0.08,
                    deltaMovement.y + lookDirection.y * 0.08,
                    deltaMovement.z + lookDirection.z * 0.08
            );
        } else if (player.isFallFlying() && ConfigManager.ServerConfig.ELYTRA_BOOST_ENABLED.get()) {
            applyElytraBoost();
        } else {
            player.setDeltaMovement(velocity);
        }
    }

    private void applyElytraBoost() {
        Vec3 motion = player.getDeltaMovement();
        Vec3 look = player.getLookAngle();

        // Scale factors
        double horizontalBoost = 0.12;
        double verticalBoost = 0.1;

        double newX = motion.x + look.x * horizontalBoost;
        double newY = motion.y + look.y * verticalBoost;
        double newZ = motion.z + look.z * horizontalBoost;

        // Clamp so we don’t reach insane speeds
        double maxSpeed = ConfigManager.ServerConfig.ELYTRA_BOOST_SPEED_MULTIPLIER.get();
        Vec3 newMotion = new Vec3(newX, newY, newZ);
        if (newMotion.length() > maxSpeed) {
            newMotion = newMotion.normalize().scale(maxSpeed);
        }

        player.setDeltaMovement(newMotion);
    }

    private void syncFuelToClient() {
        if (player.tickCount % 20 != 0) return; // Every second

        ServerPlayer serverPlayer = (ServerPlayer) player;
        int totalAir = (int) jetPackFuelRemaining;

        // Send visual air packet
        if (!player.onGround() && jetPackFuelRemaining > 0) {
            PacketDistributor.sendToPlayer(serverPlayer, new VisualJetpackAirPacket(totalAir));
            airGaugeCleared = false;
        } else {
            fadeOutVisualAirOverlay();
        }

        // Send fuel sync packet
        long serverTime = player.level().getGameTime();
        PacketDistributor.sendToPlayer(serverPlayer, new JetpackFuelSyncPacket(jetPackFuelRemaining, serverTime));
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
        ItemEnchantments enchantments = player.getItemBySlot(EquipmentSlot.LEGS).get(DataComponents.ENCHANTMENTS);
        Holder<Enchantment> holder = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.SWIFT_SNEAK).orElse(null);

        double enchantedSpeedMultiplier = (enchantments == null || holder == null) ? 0 : enchantments.getLevel(holder) * 0.15F;
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
            horizontalSpeed = (baseSpeed + baseHoverSpeedBoost) * (1.0 + enchantedSpeedMultiplier) + (mobEffectSpeedMultiplier / 10);
        }

        return horizontalSpeed;
    }

    private double calculateVerticalSpeed() {
        double currentVerticalSpeed = player.getDeltaMovement().y;
        double dampingFactor = (currentVerticalSpeed < 0 && !isJumping) ? 0.05 : 0.15;

        double verticalTarget = GRAVITY;

        if (isJumping) {
            verticalTarget = 0.42;  // Thrust value for upward movement when jumping
        } else if (player.isInWater()) {
            verticalTarget = GRAVITY / 16;  // Custom gravity in water
        } else if (player.isSwimming()) {
            return 0;  // No vertical adjustments while swimming
        }

        return currentVerticalSpeed * (1 - dampingFactor) + verticalTarget * dampingFactor;
    }

    private double calculateVerticalHoveringSpeed(double targetHeight) {
        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        UpgradeDataManager manager = UpgradeDataManager.loadFromItem(backpack);
        boolean bobbingEnabled = manager.getSetting(UpgradeDataSync.Field.JETPACK_BOBBING, true);

        double currentY = player.getY();
        double yDifference = targetHeight - currentY;

        double adjustmentForce = yDifference * 0.3;
        double newYVelocity = player.getDeltaMovement().y * 0.8 + adjustmentForce;

        if (bobbingEnabled && player.level().isClientSide()) {
            float time = player.level().getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            double period = 20.0; // ~ 1 second
            double angle = (time % period) / period * (2 * Math.PI);
            double bob = Math.sin(angle) * 0.008;
            newYVelocity += bob;
        }

        return Mth.clamp(newYVelocity, -0.2, 0.2);
    }

    public double calculateJetPackFuel(Player player) {
        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        if (backpack.isEmpty()) return 0.0; // No backpack == No flight upgrade

        List<ItemStack> backtanks = getBacktanksFromPlayer(player, backpack);

        return backtanks.stream().map(BacktankUtil::getAir).reduce(0, Integer::sum);
    }

    private void depleteJetPackFuel(Player player) {
        if (player.level().isClientSide || player.isCreative()) return;

        ItemStack backpack = BackpackHelper.getEquippedBackpackStack(player);
        Level world = player.level();
        long currRuntime = world.getGameTime();
        int elytraMultiplier = ConfigManager.ServerConfig.ELYTRA_BOOST_MULTIPLIER.get();

        if (player.isFallFlying() && ConfigManager.ServerConfig.ELYTRA_BOOST_ENABLED.get()) {
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

        int totalAir = backtanks.stream().map(BacktankUtil::getAir).reduce(0, Integer::sum);
        PacketDistributor.sendToPlayer((ServerPlayer) player, new VisualJetpackAirPacket(totalAir));
        airGaugeCleared = false;

        // If we are here, we assume we have at least 1 tank with some air
        int air = BacktankUtil.getAir(backtanks.getFirst());
        int newAir = Math.max(air - 1, 0);

        backtanks.getFirst().set(AllDataComponents.BACKTANK_AIR, newAir);
        itemHandler.setDataChanged();

        sendFuelWarning(player, totalAir, totalAir - 1, 90);
        sendFuelWarning(player, totalAir, totalAir - 1, 1);
    }

    private static IBackpackContainer getBackpackContainer(Player player, ItemStack backpack) {
        if (player.containerMenu instanceof BackpackMenu menu && menu.getBackpackType() == BackpackMenu.BackpackType.WORN) {
            return menu.container;
        } else {
            return BackpackContainer.Cache.getOrCreateWornBackpack(player, backpack);
        }
    }

    private List<ItemStack> getBacktanksFromPlayer(Player player, ItemStack backpack) {
        List<ItemStack> backtanks = new ArrayList<>();

        // Check chest item
        ItemStack chestItem = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestItem.isEmpty() && AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.matches(chestItem) && BacktankUtil.hasAirRemaining(chestItem)) {
            backtanks.add(chestItem);
        }

        // Check backpack items
        if (!backpack.isEmpty()) {
            IBackpackContainer container = getBackpackContainer(player, backpack);
            IItemHandler inventory = container.getItemHandler();
            itemHandler = container;

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

    public void processPlayerInputPacket(float forward, float left) {
        this.forward = forward;
        this.left = left;
    }

    public void processPlayerFlyingPacket(boolean flying, boolean hovering) {
        this.isJumping = flying;
        this.isHovering = hovering;
    }

    public void flyingOnKeyPress() {
        this.isJumping = true;
    }

    public void flyingOnKeyRelease() {
        this.isJumping = false;
    }

    public void resetState() {
        isJumping = false;
        isHovering = false;
        hoverHeight = 0;
        playedSoundThisJump = false;
        lastRuntime = 0;
        airGaugeLastCleared = 0;
        airGaugeCleared = false;
        lastValidVelocity = Vec3.ZERO;
        predictedFuelRemaining = 0;
        lastFuelSync = 0;
    }
}
