package net.fxnt.fxntstorage.backpack.upgrade.jukebox;

import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;

@Mod.EventBusSubscriber(modid = FXNTStorage.MOD_ID)
public class JukeboxBuffHandler {
    private static final int INFINITE = -1;
    private static final int BLOCK_BUFF_DURATION = 120;

    public static void applyMusicBuffsFromPlayer(Player player, ResourceLocation song) {
        if (!ConfigManager.ServerConfig.JUKEBOX_BUFFS_ENABLED.get()) return;

        JukeboxSongRegistry.JukeboxSongData data = JukeboxSongRegistry.get(song);
        if (data == null) return;

        for (JukeboxSongRegistry.SongEffectData effect : data.effects()) {
            MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(effect.effect());
            if (mobEffect == null) continue;

            addPlayerBuff(player, mobEffect, effect.amplifier(), INFINITE);
        }
    }

    public static void applyMusicBuffsFromBlock(Player player, ResourceLocation song) {
        if (!ConfigManager.ServerConfig.JUKEBOX_BUFFS_ENABLED.get()) return;

        JukeboxSongRegistry.JukeboxSongData data = JukeboxSongRegistry.get(song);
        if (data == null) return;

        if (data.playerOnly()) return;

        for (JukeboxSongRegistry.SongEffectData effect : data.effects()) {
            MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(effect.effect());
            if (mobEffect == null) continue;

            // Check if player already has this effect with longer duration (from worn backpack)
            MobEffectInstance existing = player.getEffect(mobEffect);
            if (existing != null && (existing.getDuration() == INFINITE || existing.getDuration() > BLOCK_BUFF_DURATION * 2)) {
                // Don't override longer-duration buffs from worn backpacks
                continue;
            }

            addPlayerBuff(player, mobEffect, effect.amplifier(), BLOCK_BUFF_DURATION);
        }
    }

    private static void addPlayerBuff(Player player, MobEffect effect, int amplifier, int duration) {
        player.addEffect(new MobEffectInstance(
                effect,
                duration,
                amplifier,
                true,
                true
        ));
    }

    public static void removePlayerBuff(Player player, ResourceLocation song) {
        JukeboxSongRegistry.JukeboxSongData data = JukeboxSongRegistry.get(song);
        if (data == null) return;

        for (JukeboxSongRegistry.SongEffectData effectData : data.effects()) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectData.effect());
            if (effect != null) {
                player.removeEffect(effect);
            }
        }
    }

    @SubscribeEvent
    public static void onPiglinTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof AbstractPiglin piglin)) return;
        if (!(event.getOriginalTarget() instanceof Player player)) return;

        if (player.hasEffect(ModEffects.PACIFY_PIGLINS.get())) {
            event.setCanceled(true);
            piglin.setTarget(null);
        }
    }

    @SubscribeEvent
    public static void onCreeperTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        if (!(event.getOriginalTarget() instanceof Player player)) return;

        if (player.hasEffect(ModEffects.REPEL_CREEPERS.get())) {
            event.setCanceled(true);
            creeper.setTarget(null);
        }
    }

    @SubscribeEvent
    public static void onCreeperTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        if (creeper.level().isClientSide) return;

        // Check every 10 ticks
        if (creeper.tickCount % 10 != 0) return;

        Player nearestCatPlayer = creeper.level().getNearbyPlayers(
                        TargetingConditions.DEFAULT,
                        creeper,
                        creeper.getBoundingBox().inflate(12.0)
                ).stream()
                .filter(p -> p.hasEffect(ModEffects.REPEL_CREEPERS.get()))
                .min(Comparator.comparingDouble(creeper::distanceToSqr))
                .orElse(null);

        if (nearestCatPlayer != null) {
            Vec3 awayFromPlayer = creeper.position()
                    .subtract(nearestCatPlayer.position())
                    .normalize();

            Vec3 fleePos = creeper.position().add(awayFromPlayer.scale(3.5));
            double speed = creeper.distanceTo(nearestCatPlayer) < 8.0 ? 1.4 : 1.0;

            creeper.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, speed);
        }
    }
}
