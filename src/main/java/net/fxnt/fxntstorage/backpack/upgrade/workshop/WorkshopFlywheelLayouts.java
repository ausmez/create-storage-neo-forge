package net.fxnt.fxntstorage.backpack.upgrade.workshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class WorkshopFlywheelLayouts {
    private static final ResourceLocation DEFAULT_ID = FXNTStorage.modLoc("default");

    // Used when the default layout file is missing or broken
    private static final Layout FALLBACK = new Layout(List.of(
            new WorkshopFlywheelPlacement(new Vec3(1.92, 6.4, 8), WorkshopFlywheelPlacement.DEFAULT_SIZE, Direction.Axis.X, false),
            new WorkshopFlywheelPlacement(new Vec3(14.08, 6.4, 8), WorkshopFlywheelPlacement.DEFAULT_SIZE, Direction.Axis.X, false)
    ), Optional.empty());

    private static volatile Map<ResourceLocation, Layout> layouts = Map.of();

    private WorkshopFlywheelLayouts() {}

    public static List<WorkshopFlywheelPlacement> get(Item backpack, boolean worn) {
        Map<ResourceLocation, Layout> current = layouts;
        Layout layout = current.get(BuiltInRegistries.ITEM.getKey(backpack));
        if (layout == null) layout = current.getOrDefault(DEFAULT_ID, FALLBACK);
        return worn ? layout.worn().orElse(layout.flywheels()) : layout.flywheels();
    }

    private record Layout(List<WorkshopFlywheelPlacement> flywheels, Optional<List<WorkshopFlywheelPlacement>> worn) {
        static final Codec<Layout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                WorkshopFlywheelPlacement.CODEC.listOf().fieldOf("flywheels").forGetter(Layout::flywheels),
                WorkshopFlywheelPlacement.CODEC.listOf().optionalFieldOf("worn").forGetter(Layout::worn)
        ).apply(instance, Layout::new));
    }

    public static class ReloadListener extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new GsonBuilder().create();

        public ReloadListener() {
            super(GSON, "backpack_flywheels");
        }

        @Override
        protected void apply(@NotNull Map<ResourceLocation, JsonElement> files, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
            Map<ResourceLocation, Layout> loaded = new HashMap<>();
            files.forEach((id, json) -> {
                // A broken file is skipped whole (no partial layouts), so the type falls back to the default
                DataResult<Layout> result = Layout.CODEC.parse(JsonOps.INSTANCE, json);
                result.error().ifPresent(error -> FXNTStorage.LOGGER.error("Invalid backpack flywheel layout '{}': {}", id, error.message()));
                result.result().ifPresent(layout -> loaded.put(id, layout));
            });
            layouts = Map.copyOf(loaded);
        }
    }
}
