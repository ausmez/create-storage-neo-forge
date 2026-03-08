package net.fxnt.fxntstorage.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class ParticleHelper {

    public static void jetPackParticles(Player player) {
        LevelAccessor world = player.level();
        ParticleOptions particleType = player.isInWater() ? ParticleTypes.BUBBLE_COLUMN_UP
                : player.isInLava() ? ParticleTypes.FLAME
                : ParticleTypes.CLOUD;

        Vec3 position = player.position();
        double posX = position.x;
        double posY = position.y + 0.55;
        double posZ = position.z;

        // Get the backward direction based on the player's yaw
        float yawRadians = (float) Math.toRadians(player.getYRot());
        double backwardX =  Math.sin(yawRadians) * 0.35; // Offset to spawn particles slightly behind the player
        double backwardZ = -Math.cos(yawRadians) * 0.35; // Offset to spawn particles slightly behind the player

        double finalPosX = posX + backwardX;
        double finalPosZ = posZ + backwardZ;

        double speed = -(Math.random() / 10);

        // Spawn the particles (server-side so all players can see them)
        if (world instanceof ServerLevel level) {
            level.sendParticles(particleType, finalPosX, posY, finalPosZ, 2, 0.15, 0.15, 0.0, speed);
        }
    }

    public static void jukeboxParticles(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            Vec3 vec3 = pos.getCenter();

            double xOffset = level.random.nextGaussian() * 0.15;
            double yOffset = level.random.nextGaussian() * 0.20;
            double zOffset = level.random.nextGaussian() * 0.15;

            serverLevel.sendParticles(ParticleTypes.NOTE, vec3.x(), vec3.y() + 0.5, vec3.z(), 1, xOffset, yOffset, zOffset, level.random.nextDouble());
        }
    }

    public static void jukeboxParticles(Player player) {
        Vec3 localOffset = new Vec3(0, 0.5, -0.3);

        float yaw = player.yBodyRot * ((float) Math.PI / 180); // Convert to radians
        Vec3 rotatedOffset = new Vec3(
                localOffset.x * Math.cos(yaw) - localOffset.z * Math.sin(yaw),
                localOffset.y + 0.65,
                localOffset.x * Math.sin(yaw) + localOffset.z * Math.cos(yaw)
        );

        double xOffset = player.level().random.nextGaussian() * 0.15;
        double yOffset = player.level().random.nextGaussian() * 0.20;
        double zOffset = player.level().random.nextGaussian() * 0.15;

        Vec3 particlePos = player.position().add(rotatedOffset);
        if (player.level() instanceof ServerLevel serverLevel)
            serverLevel.sendParticles(ParticleTypes.NOTE, particlePos.x(), particlePos.y() + 0.5, particlePos.z(), 1, xOffset, yOffset, zOffset, serverLevel.random.nextDouble());
    }
}
