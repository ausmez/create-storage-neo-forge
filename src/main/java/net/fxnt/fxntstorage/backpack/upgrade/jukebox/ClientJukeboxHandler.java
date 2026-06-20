package net.fxnt.fxntstorage.backpack.upgrade.jukebox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class ClientJukeboxHandler {
    private static final Map<UUID, PlayerPlayback> playerSounds = new ConcurrentHashMap<>();
    private static final Map<BlockPos, BlockPlayback> blockSounds = new ConcurrentHashMap<>();
    private static final Map<Integer, EntityContraptionPlayback> entitySounds = new ConcurrentHashMap<>();
    private static final float FADE_IN_SPEED = 1f / (10.0f * 20f);
    private static final float FADE_OUT_SPEED = 1f / (2.5f * 20f);

    public record EntityContraptionPlayback(int entityId, @Nullable EntityContraptionSoundInstance sound,
                                            ResourceLocation song, boolean muted) {
        public EntityContraptionPlayback toggleMuted() {
            if (sound != null) sound.setMuted(!muted);
            return new EntityContraptionPlayback(entityId, sound, song, !muted);
        }
    }

    public record PlayerPlayback(UUID playerId, @Nullable EntitySoundInstance sound, ResourceLocation song,
                                 boolean muted) {
        public PlayerPlayback toggleMuted() {
            if (sound != null) {
                sound.setMuted(!muted);
            }
            return new PlayerPlayback(playerId, sound, song, !muted);
        }
    }

    public record BlockPlayback(BlockPos pos, @Nullable BlockSoundInstance sound, ResourceKey<Level> dimension,
                                ResourceLocation song, boolean muted) {
        public BlockPlayback toggleMuted() {
            if (sound != null) {
                sound.setMuted(!muted);
            }
            return new BlockPlayback(pos, sound, dimension, song, !muted);
        }
    }

    public static void init() {
        playerSounds.clear();
        blockSounds.clear();
        entitySounds.clear();
    }

    public static void playPlayer(PlayerPlayback player) {
        UUID playerId = player.playerId;

        Minecraft mc = Minecraft.getInstance();
        SoundManager soundManager = mc.getSoundManager();
        PlayerPlayback activeSound = playerSounds.get(playerId);

        if (activeSound == null || !soundManager.isActive(Objects.requireNonNull(activeSound.sound))) {
            stopPlayer(playerId);

            ClientLevel level = mc.level;
            if (level == null) return;

            Optional<Holder.Reference<JukeboxSong>> song = getSong(player.song(), level);
            song.ifPresent(holder -> {
                JukeboxSong jukeboxSong = holder.value();
                EntitySoundInstance newSound = new EntitySoundInstance(jukeboxSong, mc.player, player.muted);
                if (isInPaleGarden(level, mc.player.blockPosition()) && !player.muted)
                    newSound.setBiomeVolume(0.0f);
                soundManager.play(newSound);
                mc.gui.setNowPlaying(jukeboxSong.description());
                PlayerPlayback newPP = new PlayerPlayback(playerId, newSound, player.song, player.muted);
                playerSounds.put(playerId, newPP);
            });

        }
    }

    public static void stopPlayer(UUID id) {
        PlayerPlayback removed = playerSounds.remove(id);
        if (removed != null) {
            Minecraft.getInstance().getSoundManager().stop(Objects.requireNonNull(removed.sound));
        }
    }

    public static void toggleMutePlayer(Player player) {
        playerSounds.computeIfPresent(player.getUUID(), (uuid, playerPlayback) -> playerPlayback.toggleMuted());
    }

    public static boolean isPlayerPlaying(Player player) {
        return playerSounds.containsKey(player.getUUID());
    }

    public static boolean isPlayerMuted(Player player) {
        PlayerPlayback pb = playerSounds.get(player.getUUID());
        return pb != null && pb.muted();
    }

    public static void playBlock(BlockPlayback block) {
        Minecraft mc = Minecraft.getInstance();
        SoundManager soundManager = mc.getSoundManager();
        BlockPlayback activeSound = blockSounds.get(block.pos());

        if (activeSound == null || !soundManager.isActive(Objects.requireNonNull(activeSound.sound))) {
            ClientLevel level = mc.level;
            if (level == null) return;

            Optional<Holder.Reference<JukeboxSong>> song = getSong(block.song(), level);
            song.ifPresent(holder -> {
                stopBlock(block.pos());
                JukeboxSong jukeboxSong = holder.value();
                BlockSoundInstance newSound = new BlockSoundInstance(jukeboxSong, block.pos(), block.muted);
                if (isInPaleGarden(level, mc.player.blockPosition()) && !block.muted)
                    newSound.setBiomeVolume(0.0f);
                soundManager.play(newSound);
                mc.gui.setNowPlaying(jukeboxSong.description());
                BlockPlayback newBP = new BlockPlayback(block.pos, newSound, block.dimension, block.song, block.muted);
                blockSounds.put(block.pos(), newBP);
            });
        }
    }

    public static void stopBlock(BlockPos pos) {
        BlockPlayback removed = blockSounds.remove(pos);
        if (removed != null)
            Minecraft.getInstance().getSoundManager().stop(Objects.requireNonNull(removed.sound));
    }

    public static void toggleMuteBlock(BlockPos pos) {
        blockSounds.computeIfPresent(pos, (uuid, blockPlayback) -> blockPlayback.toggleMuted());
    }

    public static boolean isBlockPlaying(BlockPos pos) {
        return blockSounds.containsKey(pos);
    }

    public static boolean isBlockMuted(BlockPos pos) {
        BlockPlayback pb = blockSounds.get(pos);
        return pb != null && pb.muted();
    }

    // ENTITY (contraption-mounted backpack)
    public static void playEntity(EntityContraptionPlayback entity) {
        int entityId = entity.entityId();
        Minecraft mc = Minecraft.getInstance();
        SoundManager sm = mc.getSoundManager();
        EntityContraptionPlayback active = entitySounds.get(entityId);

        if (active == null || active.sound() == null || !sm.isActive(active.sound())) {
            stopEntity(entityId);
            ClientLevel level = mc.level;
            if (level == null) return;

            Optional<Holder.Reference<JukeboxSong>> song = getSong(entity.song(), level);
            song.ifPresent(holder -> {
                JukeboxSong jukeboxSong = holder.value();
                EntityContraptionSoundInstance newSound = new EntityContraptionSoundInstance(jukeboxSong, entityId, entity.muted());
                sm.play(newSound);
                mc.gui.setNowPlaying(jukeboxSong.description());
                entitySounds.put(entityId, new EntityContraptionPlayback(entityId, newSound, entity.song(), entity.muted()));
            });
        }
    }

    public static void stopEntity(int entityId) {
        EntityContraptionPlayback removed = entitySounds.remove(entityId);
        if (removed != null && removed.sound() != null)
            Minecraft.getInstance().getSoundManager().stop(removed.sound());
    }

    public static boolean isEntityPlaying(int entityId) {
        return entitySounds.containsKey(entityId);
    }

    public static boolean isEntityMuted(int entityId) {
        EntityContraptionPlayback pb = entitySounds.get(entityId);
        return pb != null && pb.muted();
    }

    public static void toggleMuteEntity(int entityId) {
        entitySounds.computeIfPresent(entityId, (id, pb) -> pb.toggleMuted());
    }

    public static void stopAllMusic() {
        for (Map.Entry<UUID, PlayerPlayback> player : playerSounds.entrySet()) {
            stopPlayer(player.getKey());
        }
        for (Map.Entry<BlockPos, BlockPlayback> block : blockSounds.entrySet()) {
            stopBlock(block.getKey());
        }
        for (int entityId : entitySounds.keySet()) {
            stopEntity(entityId);
        }
    }

    public static Optional<Holder.Reference<JukeboxSong>> getSong(ResourceLocation id, Level level) {
        return level.registryAccess()
                .registryOrThrow(Registries.JUKEBOX_SONG)
                .getHolder(ResourceKey.create(Registries.JUKEBOX_SONG, id));
    }

    private static boolean isInPaleGarden(Level level, BlockPos pos) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        return level.getBiome(pos)
                .is(ResourceKey.create(Registries.BIOME,
                        ResourceLocation.withDefaultNamespace("pale_garden")));
    }

    private static class EntitySoundInstance extends AbstractTickableSoundInstance {
        private final Player player;
        private boolean muted;

        private float biomeVolumeMultiplier = 1.0f;

        protected EntitySoundInstance(JukeboxSong song, Player player, boolean muted) {
            super(song.soundEvent().value(), SoundSource.RECORDS, RandomSource.create());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.muted = muted;
            this.volume = muted ? 0.0F : 1.0F;
            this.pitch = 1.0F;

            this.attenuation = Attenuation.NONE;
            this.relative = true;
        }

        public void setMuted(boolean muted) {
            this.muted = muted;
            this.volume = this.muted ? 0.0f : 1.0f;
        }

        public void setBiomeVolume(float volume) {
            this.biomeVolumeMultiplier = volume;
            applyVolume();
        }

        private void applyVolume() {
            if (!muted) {
                float baseVolume = 1.0f;
                this.volume = baseVolume * biomeVolumeMultiplier;
            }
        }

        @Override
        public void tick() {
            if (isStopped() || !player.isAlive() || player.isRemoved()) {
                this.stop();
                return;
            }

            boolean inPaleGarden = isInPaleGarden(player.level(), player.blockPosition());

            if (!muted) {
                float target = inPaleGarden ? 0.0f : 1.0f;

                if (biomeVolumeMultiplier < target) {
                    biomeVolumeMultiplier = Math.min(target, biomeVolumeMultiplier + FADE_IN_SPEED);
                } else if (biomeVolumeMultiplier > target) {
                    biomeVolumeMultiplier = Math.max(target, biomeVolumeMultiplier - FADE_OUT_SPEED);
                }

                applyVolume();
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }

    public static class EntityContraptionSoundInstance extends AbstractTickableSoundInstance {
        private final int entityId;
        private boolean muted;
        private float biomeVolumeMultiplier = 1.0f;

        public EntityContraptionSoundInstance(JukeboxSong song, int entityId, boolean muted) {
            super(song.soundEvent().value(), SoundSource.RECORDS, RandomSource.create());
            this.entityId = entityId;
            this.looping = true;
            this.delay = 0;
            this.muted = muted;
            this.volume = muted ? 0.0f : 1.0f;
            this.pitch = 1.0f;
            this.attenuation = Attenuation.LINEAR;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Entity e = mc.level.getEntity(entityId);
                if (e != null) {
                    this.x = e.getX();
                    this.y = e.getY();
                    this.z = e.getZ();
                }
            }
        }

        public void setMuted(boolean muted) {
            this.muted = muted;
            this.volume = muted ? 0.0f : biomeVolumeMultiplier;
        }

        private void applyVolume() {
            if (!muted) this.volume = biomeVolumeMultiplier;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) { stop(); return; }

            Entity e = mc.level.getEntity(entityId);
            if (e == null || e.isRemoved()) { stop(); return; }

            this.x = e.getX();
            this.y = e.getY();
            this.z = e.getZ();

            if (!muted) {
                boolean inPaleGarden = isInPaleGarden(mc.level, e.blockPosition());
                float target = inPaleGarden ? 0.0f : 1.0f;
                if (biomeVolumeMultiplier < target)
                    biomeVolumeMultiplier = Math.min(target, biomeVolumeMultiplier + FADE_IN_SPEED);
                else if (biomeVolumeMultiplier > target)
                    biomeVolumeMultiplier = Math.max(target, biomeVolumeMultiplier - FADE_OUT_SPEED);
                applyVolume();
            }
        }

        @Override
        public boolean canStartSilent() { return true; }
    }

    private static class BlockSoundInstance extends AbstractTickableSoundInstance {
        private final BlockPos pos;
        private boolean muted;

        private float biomeVolumeMultiplier = 1.0f;

        public BlockSoundInstance(JukeboxSong song, BlockPos pos, boolean muted) {
            super(song.soundEvent().value(), SoundSource.RECORDS, RandomSource.create());
            this.pos = pos;
            this.looping = true;
            this.attenuation = Attenuation.LINEAR;
            this.muted = muted;
            this.volume = muted ? 0.0f : 1.0f;
            this.pitch = 1.0f;
            updatePos();
        }

        public void setMuted(boolean muted) {
            this.muted = muted;
            this.volume = this.muted ? 0.0f : 1.0f;
        }

        public void setBiomeVolume(float volume) {
            this.biomeVolumeMultiplier = volume;
            applyVolume();
        }

        private void updatePos() {
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
        }

        private void applyVolume() {
            if (!muted) {
                float baseVolume = 1.0f;
                this.volume = baseVolume * biomeVolumeMultiplier;
            }
        }

        @Override
        public void tick() {
            if (isStopped()) stop();

            Minecraft mc = Minecraft.getInstance();
            boolean inPaleGarden = isInPaleGarden(mc.level, mc.player.blockPosition());

            if (!muted) {
                float target = inPaleGarden ? 0.0f : 1.0f;

                if (biomeVolumeMultiplier < target) {
                    biomeVolumeMultiplier = Math.min(target, biomeVolumeMultiplier + FADE_IN_SPEED);
                } else if (biomeVolumeMultiplier > target) {
                    biomeVolumeMultiplier = Math.max(target, biomeVolumeMultiplier - FADE_OUT_SPEED);
                }

                applyVolume();
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
