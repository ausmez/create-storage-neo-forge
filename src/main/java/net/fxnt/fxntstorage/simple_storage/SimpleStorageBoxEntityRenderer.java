package net.fxnt.fxntstorage.simple_storage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.fxnt.fxntstorage.compat.sable.SableCompat;
import net.fxnt.fxntstorage.init.ModItems;
import net.fxnt.fxntstorage.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static net.fxnt.fxntstorage.util.RendererHelper.*;

public class SimpleStorageBoxEntityRenderer implements BlockEntityRenderer<SimpleStorageBoxEntity> {

    protected final BlockEntityRendererProvider.Context context;

    public SimpleStorageBoxEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    public static void renderFromContraptionContext(MovementContext context, ContraptionMatrices matrices, MultiBufferSource buffer) {
        BlockState state = context.state;
        CompoundTag tag = context.blockEntityData;
        if (tag == null || state == null) return;

        boolean hasVoidUpgrade = tag.getBoolean("VoidUpgrade");
        boolean hasCompUpgrade = tag.getBoolean("CompactingUpgrade");

        int amount = tag.getInt("StoredAmount");
        int totalSpace = tag.getInt("MaxItemCapacity");
        int percentUsed = (int) Math.round(((double) amount / totalSpace) * 100);

        String line1 = Util.formatNumber(amount);
        String line2 = percentUsed + "% Used";

        Direction side = state.getValue(HorizontalDirectionalBlock.FACING);

        PoseStack poseStack = matrices.getModelViewProjection();

        poseStack.pushPose();
        poseStack.translate(context.localPos.getX() + 0.5f, context.localPos.getY() + 0.5f, context.localPos.getZ() + 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-side.toYRot()));
        poseStack.translate(0f, 0f, 0.5f - 0.95f / 16f);

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            poseStack.popPose();
            return;
        }

        double distance = context.position != null
                ? Math.sqrt(player.distanceToSqr(context.position))
                : Math.sqrt(player.distanceToSqr(context.contraption.entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1f)));

        if (distance >= getMaxDistance()) {
            poseStack.popPose();
            return;
        }

        ItemRenderer itemRenderer = mc.getItemRenderer();
        int color = getColorForDistance(distance);

        BlockPos lightPos = context.contraption.entity.blockPosition().offset(context.localPos).relative(side);
        int envLight = LevelRenderer.getLightColor(context.world, lightPos);
        int textLight = emissiveLight(envLight);
        int itemLight = emissiveItemLight(envLight);

        ItemStack filterItem = ItemStack.EMPTY;
        if (tag.contains("FilterItem")) {
            filterItem = tag.getCompound("FilterItem").isEmpty()
                    ? ItemStack.EMPTY
                    : ItemStack.parseOptional(context.contraption.entity.level().registryAccess(), tag.getCompound("FilterItem"));
        }

        if (hasCompUpgrade) {
            CompactingChain chain = null;
            if (!filterItem.isEmpty()) {
                if (CompactingRecipeHelper.isEmpty() && mc.level != null) {
                    CompactingRecipeHelper.rebuild(mc.level.getRecipeManager(), mc.level.registryAccess());
                }
                chain = CompactingRecipeHelper.buildChain(filterItem.getItem());
            }
            if (chain != null) {
                int selected = Math.min(tag.getInt("CompactingSelectedTier"), chain.tiers() - 1);
                String displayCount = getCountForSlot(chain, selected, amount);
                renderLine(displayCount, -1f, poseStack, buffer, color, textLight);
                renderLine(line2, -4f, poseStack, buffer, color, textLight);
                renderPips(chain.tiers(), selected, poseStack, buffer, textLight - 32);
                renderItem(itemRenderer, chain.itemForSlot(selected), poseStack, buffer, itemLight,
                        ModItems.STORAGE_BOX_COMPACTING_UPGRADE.asStack());
            } else {
                renderLine(line1, -1f, poseStack, buffer, color, textLight);
                renderLine(line2, -4f, poseStack, buffer, color, textLight);
                if (!filterItem.isEmpty()) {
                    renderItem(itemRenderer, filterItem, poseStack, buffer, itemLight,
                            ModItems.STORAGE_BOX_COMPACTING_UPGRADE.asStack());
                }
            }
        } else {
            renderLine(line1, -1f, poseStack, buffer, color, textLight);
            renderLine(line2, -4f, poseStack, buffer, color, textLight);

            if (!filterItem.isEmpty() || hasVoidUpgrade) {
                ItemStack upgradeIcon = hasVoidUpgrade
                        ? ModItems.STORAGE_BOX_VOID_UPGRADE.asStack() : ItemStack.EMPTY;
                renderItem(itemRenderer, filterItem, poseStack, buffer, itemLight, upgradeIcon);
            }
        }

        poseStack.popPose();
    }

    @Override
    public void render(SimpleStorageBoxEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = mc.screen;
        boolean isPonderScene = currentScreen instanceof AbstractSimiScreen;
        boolean inSubLevel = SableCompat.isInPlotGrid(blockEntity);

        Player player = mc.player;
        if (player == null) return;

        Level level = blockEntity.getLevel();
        if (level == null) return;

        int amount = blockEntity.getStoredAmount();
        int totalSpace = blockEntity.getMaxItemCapacity();
        int percentUsed = (int) Math.round(((double) amount / totalSpace) * 100);

        String line1 = Util.formatNumber(amount);
        String line2 = percentUsed + "% Used";

        float distance = (float) Math.sqrt(blockEntity.getBlockPos().distToCenterSqr(player.position()));

        if (distance > getMaxDistance() && !isPonderScene && !inSubLevel) return;
        if (isPonderScene || inSubLevel) distance = 5f;

        BlockState blockState = blockEntity.getBlockState();
        Direction side = blockState.getValue(HorizontalDirectionalBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-side.toYRot()));
        poseStack.translate(0f, 0f, 0.5f - 0.95f / 16f);

        int color = getColorForDistance(distance);
        int envLight = LevelRenderer.getLightColor(level, blockEntity.getBlockPos());
        int textLight = emissiveLight(envLight);
        int itemLight = emissiveItemLight(envLight);

        if (blockEntity.compactingUpgrade) {
            ItemStack displayItem;
            String displayCount;

            if (blockEntity.compactingChain != null) {
                CompactingChain chain = blockEntity.compactingChain;
                int selected = Math.min(blockEntity.compactingSelectedTier, chain.tiers() - 1);
                displayItem = chain.itemForSlot(selected);
                displayCount = getCountForSlot(chain, selected, amount);
                renderPips(chain.tiers(), selected, poseStack, buffer, textLight-32);
            } else {
                displayItem = blockEntity.getFilterItem();
                displayCount = line1;
            }

            renderLine(displayCount, -1.4f, poseStack, buffer, color, textLight);
            renderLine(line2, -4.1f, poseStack, buffer, color, textLight);
            renderItem(Minecraft.getInstance().getItemRenderer(), displayItem, poseStack, buffer, itemLight,
                    ModItems.STORAGE_BOX_COMPACTING_UPGRADE.asStack());
        } else {
            renderLine(line1, -1.4f, poseStack, buffer, color, textLight);
            renderLine(line2, -4.1f, poseStack, buffer, color, textLight);

            ItemStack filterItem = blockEntity.getFilterItem();
            if (!filterItem.isEmpty() || blockEntity.voidUpgrade || blockEntity.compactingUpgrade) {
                ItemStack upgradeIcon = blockEntity.voidUpgrade
                        ? ModItems.STORAGE_BOX_VOID_UPGRADE.asStack()
                        : ItemStack.EMPTY;
                renderItem(Minecraft.getInstance().getItemRenderer(), filterItem, poseStack, buffer, itemLight, upgradeIcon);
            }
        }

        poseStack.popPose();
    }

    private static void renderPips(int tiers, int selected, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Font font = Minecraft.getInstance().font;
        String pip = "■"; // ■ BLACK SQUARE
        int pipPx = font.width(pip);
        int gapPx = 3;
        float totalPx = tiers * pipPx + (tiers - 1) * gapPx;

        float scale = 0.55f / 64f;

        poseStack.pushPose();
        poseStack.translate(0f, 0.4f / 16f, 0f);
        poseStack.scale(scale, -scale, 1f);

        float x = -totalPx / 2f;
        for (int i = 0; i < tiers; i++) {
            int pipColor = i == selected ? 0xFFFFFF : 0x444444;
            font.drawInBatch(pip, x, 0, pipColor, false,
                    poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);
            x += pipPx + gapPx;
        }
        poseStack.popPose();
    }

    private static String getCountForSlot(CompactingChain chain, int slotIdx, int t0Stored) {
        if (chain.tiers() == 2) {
            return slotIdx == 0 ? Util.formatNumber(chain.t1Count(t0Stored)) : Util.formatNumber(t0Stored);
        }
        return switch (slotIdx) {
            case 0 -> Util.formatNumber(chain.t2Count(t0Stored));
            case 1 -> Util.formatNumber(chain.t1Count(t0Stored));
            default -> Util.formatNumber(t0Stored);
        };
    }

}
