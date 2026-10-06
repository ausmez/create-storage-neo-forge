package net.fxnt.fxntstorage.backpack.client.renderer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.createmod.catnip.math.VoxelShaper;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.backpack.BackpackBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class BackpackModelShapes {
    private static final int MAX_PARENT_DEPTH = 16;

    private BackpackModelShapes() {}

    public static class ReloadListener extends SimplePreparableReloadListener<Map<Block, VoxelShaper>> {
        @Override
        protected @NotNull Map<Block, VoxelShaper> prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
            Map<Block, VoxelShaper> shapes = new HashMap<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                if (!(block instanceof BackpackBlock)) continue;
                ResourceLocation model = BuiltInRegistries.BLOCK.getKey(block).withPrefix("block/");
                VoxelShape shape = loadShape(resourceManager, model);
                if (shape != null) shapes.put(block, VoxelShaper.forHorizontal(shape, Direction.NORTH));
            }
            return shapes;
        }

        @Override
        protected void apply(@NotNull Map<Block, VoxelShaper> shapes, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
            BackpackBlock.setModelShapes(shapes);
        }
    }

    @Nullable
    private static VoxelShape loadShape(ResourceManager resourceManager, ResourceLocation model) {
        ResourceLocation current = model;
        for (int depth = 0; current != null && depth < MAX_PARENT_DEPTH; depth++) {
            Optional<Resource> resource = resourceManager.getResource(current.withPath(path -> "models/" + path + ".json"));
            if (resource.isEmpty()) return null;

            JsonObject json;
            try (Reader reader = resource.get().openAsReader()) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                FXNTStorage.LOGGER.error("Failed to read backpack model '{}' for its outline", current, e);
                return null;
            }

            if (json.has("elements")) return toShape(current, json.getAsJsonArray("elements"));
            current = json.has("parent") ? ResourceLocation.tryParse(json.get("parent").getAsString()) : null;
        }
        return null;
    }

    @Nullable
    private static VoxelShape toShape(ResourceLocation model, JsonArray elements) {
        VoxelShape shape = Shapes.empty();
        try {
            for (JsonElement element : elements) {
                JsonArray from = element.getAsJsonObject().getAsJsonArray("from");
                JsonArray to = element.getAsJsonObject().getAsJsonArray("to");
                double x1 = from.get(0).getAsDouble(), y1 = from.get(1).getAsDouble(), z1 = from.get(2).getAsDouble();
                double x2 = to.get(0).getAsDouble(), y2 = to.get(1).getAsDouble(), z2 = to.get(2).getAsDouble();
                if (x1 == x2 || y1 == y2 || z1 == z2) continue;
                shape = Shapes.or(shape, Block.box(
                        Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                        Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)));
            }
        } catch (Exception e) {
            FXNTStorage.LOGGER.error("Invalid elements in backpack model '{}'; using the default outline", model, e);
            return null;
        }
        return shape.isEmpty() ? null : shape.optimize();
    }
}
