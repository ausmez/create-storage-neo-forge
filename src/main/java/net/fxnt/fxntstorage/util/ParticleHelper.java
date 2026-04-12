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

    public static void jetpackParticles(Player player) {
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
        double yaw = player.yBodyRot * (Math.PI / 180);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);

        // Local-space spawn position on the player's back.
        // x: bounded random across backpack width (~0.28 blocks)
        // y: random within the torso/backpack height range
        // z: back surface of the player body, behind the center
        double localX = (player.level().random.nextDouble() - 0.5) * 0.28;
        double localY = 1.05 + player.level().random.nextDouble() * 0.3;
        double localZ = -0.38;

        // Rotate the horizontal offset to world space using body yaw
        double worldX = localX * cosYaw - localZ * sinYaw;
        double worldZ = localX * sinYaw + localZ * cosYaw;

        Vec3 particlePos = player.position().add(worldX, localY, worldZ);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.NOTE,
                    particlePos.x(), particlePos.y(), particlePos.z(),
                    1, 0.0, 0.05, 0.0,
                    serverLevel.random.nextDouble());
        }
    }
}
