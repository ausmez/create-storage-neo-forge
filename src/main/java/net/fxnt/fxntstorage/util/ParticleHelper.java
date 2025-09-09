package net.fxnt.fxntstorage.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
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
}
