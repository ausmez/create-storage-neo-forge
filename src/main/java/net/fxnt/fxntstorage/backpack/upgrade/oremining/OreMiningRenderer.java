package net.fxnt.fxntstorage.backpack.upgrade.oremining;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fxnt.fxntstorage.init.ModRenderTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.*;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT)
public class OreMiningRenderer {
    private static final int SOLID_R = 31;
    private static final int SOLID_G = 111;
    private static final int SOLID_B = 255;
    private static final int SOLID_A = 255;

    private static final int XRAY_A = 60;

    private static final Set<BlockPos> previewPositions = new HashSet<>();
    private static VoxelShape cachedShape = Shapes.empty();
    private static BlockPos cachedAnchor = null;

    private static final long FADE_DURATION_MS = 500;
    private static boolean fading = false;
    private static long fadeStartTime = 0;

    public static void setPreview(Collection<BlockPos> positions) {
        previewPositions.clear();
        previewPositions.addAll(positions);

        fading = false;
        fadeStartTime = 0;

        if (!previewPositions.isEmpty()) {
            cachedAnchor = previewPositions.iterator().next();
            cachedShape = buildShape(cachedAnchor);
        } else {
            cachedAnchor = null;
            cachedShape = Shapes.empty();
        }
    }

    public static void clear() {
        if (!previewPositions.isEmpty()) {
            fading = true;
            fadeStartTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (cachedShape.isEmpty() || cachedAnchor == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int alpha = SOLID_A;
        int xrayAlpha = XRAY_A;

        if (fading) {
            long elapsed = System.currentTimeMillis() - fadeStartTime;
            float progress = Math.min(1f, elapsed / (float) FADE_DURATION_MS);

            // Fade both passes toward fully transparent
            alpha = (int) (SOLID_A * (1f - progress));
            xrayAlpha = (int) (XRAY_A * (1f - progress));

            if (progress >= 1f) {
                fading = false;
                previewPositions.clear();
                cachedAnchor = null;
                cachedShape = Shapes.empty();
                return;
            }
        }

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack ms = event.getPoseStack();

        ms.pushPose();
        ms.translate(cachedAnchor.getX() - camPos.x,
                cachedAnchor.getY() - camPos.y,
                cachedAnchor.getZ() - camPos.z);
        Matrix4f matrix = ms.last().pose();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // Pass 1: solid lines (depth-tested)
        VertexConsumer solidBuffer = bufferSource.getBuffer(ModRenderTypes.ORE_LINES_SOLID);
        writeEdges(solidBuffer, matrix, cachedShape, SOLID_R, SOLID_G, SOLID_B, alpha);
        bufferSource.endBatch(ModRenderTypes.ORE_LINES_SOLID);

        // Pass 2: X-ray lines (ignores depth)
        VertexConsumer xrayBuffer = bufferSource.getBuffer(ModRenderTypes.ORE_LINES_XRAY);
        writeEdges(xrayBuffer, matrix, cachedShape, SOLID_R, SOLID_G, SOLID_B, xrayAlpha);
        bufferSource.endBatch(ModRenderTypes.ORE_LINES_XRAY);

        ms.popPose();
    }

    private static VoxelShape buildShape(BlockPos anchor) {
        Collection<VoxelShape> shapes = new HashSet<>();
        for (AABB aabb : SimpleShapeMerger.merge(anchor)) {
            shapes.add(Shapes.create(aabb.inflate(0.001)));
        }

        VoxelShape merged = Shapes.empty();
        for (VoxelShape shape : shapes) {
            merged = Shapes.joinUnoptimized(merged, shape, BooleanOp.OR);
        }
        return merged;
    }

    private static void writeEdges(VertexConsumer consumer, Matrix4f matrix,
                                   VoxelShape shape, int r, int g, int b, int a) {
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            float dx = (float) (x2 - x1);
            float dy = (float) (y2 - y1);
            float dz = (float) (z2 - z1);
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len == 0) return;

            float nx = dx / len, ny = dy / len, nz = dz / len;

            consumer.addVertex(matrix, (float) x1, (float) y1, (float) z1)
                    .setColor(r, g, b, a)
                    .setNormal(nx, ny, nz);

            consumer.addVertex(matrix, (float) x2, (float) y2, (float) z2)
                    .setColor(r, g, b, a)
                    .setNormal(nx, ny, nz);
        });
    }

    private static class SimpleShapeMerger {
        static List<AABB> merge(BlockPos anchor) {
            List<AABB> aabbs = new ArrayList<>();
            for (BlockPos pos : previewPositions) {
                BlockPos relative = pos.subtract(anchor);
                aabbs.add(new AABB(relative.getX(), relative.getY(), relative.getZ(),
                        relative.getX() + 1, relative.getY() + 1, relative.getZ() + 1));
            }
            return aabbs;
        }
    }
}
