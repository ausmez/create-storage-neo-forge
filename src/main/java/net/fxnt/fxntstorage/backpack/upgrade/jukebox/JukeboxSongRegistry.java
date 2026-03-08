package net.fxnt.fxntstorage.backpack.upgrade.jukebox;

import com.google.gson.*;
import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.fxnt.fxntstorage.FXNTStorage.MAX_EFFECTS_PER_SONG;

@EventBusSubscriber(modid = FXNTStorage.MOD_ID)
public class JukeboxSongRegistry {
    private static final Map<ResourceLocation, JukeboxSongData> SONGS = new HashMap<>();

    public static void clear() {
        SONGS.clear();
    }

    public static void register(JukeboxSongData data) {
        SONGS.put(data.song(), data);
    }

    @Nullable
    public static JukeboxSongData get(ResourceLocation songId) {
        return SONGS.get(songId);
    }

    public record JukeboxSongData(ResourceLocation song, boolean playerOnly, List<SongEffectData> effects) {
    }

    public record SongEffectData(ResourceLocation effect, int amplifier) {
    }

    public static class JukeboxSongReloadListener extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new GsonBuilder().create();

        public JukeboxSongReloadListener() {
            super(GSON, "jukebox_songs");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
            JukeboxSongRegistry.clear();

            jsons.forEach((id, json) -> {
                try {
                    JukeboxSongData data = parse(json.getAsJsonObject());
                    JukeboxSongRegistry.register(data);
                } catch (Exception e) {
                    FXNTStorage.LOGGER.error("Failed to load jukebox song data {}", id, e);
                }
            });
        }

        private static JukeboxSongData parse(JsonObject obj) {
            ResourceLocation song = ResourceLocation.parse(obj.get("song").getAsString());
            boolean playerOnly = obj.has("player_only") && obj.get("player_only").getAsBoolean();

            JsonArray effectsArray = obj.getAsJsonArray("effects");

            if (effectsArray.size() > MAX_EFFECTS_PER_SONG) {
                FXNTStorage.LOGGER.warn(
                        "[Jukebox] Song '{}' defines {} effects (max {}). Extra effects will be ignored.",
                        song, effectsArray.size(), MAX_EFFECTS_PER_SONG);
            }

            List<SongEffectData> effects = new ArrayList<>();
            Set<ResourceLocation> seen = new HashSet<>();

            for (int i = 0; i < Math.min(effectsArray.size(), MAX_EFFECTS_PER_SONG); i++) {
                JsonObject effectObj = effectsArray.get(i).getAsJsonObject();
                ResourceLocation effectId = ResourceLocation.parse(effectObj.get("effect").getAsString());

                if (!seen.add(effectId)) {
                    FXNTStorage.LOGGER.warn("[Jukebox] Duplicate effect '{}' in song '{}'. Skipping.", effectId, song);
                    continue;
                }

                effects.add(new SongEffectData(
                        ResourceLocation.parse(effectObj.get("effect").getAsString()),
                        effectObj.has("amplifier")
                                ? effectObj.get("amplifier").getAsInt()
                                : 0
                ));
            }

            return new JukeboxSongData(song, playerOnly, effects);
        }
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new JukeboxSongReloadListener());
    }
}
