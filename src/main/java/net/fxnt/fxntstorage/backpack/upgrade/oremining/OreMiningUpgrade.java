package net.fxnt.fxntstorage.backpack.upgrade.oremining;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.client.menu.button.ItemSpriteButton;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.network.packet.OreMiningPreviewPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;
import static net.fxnt.fxntstorage.util.KeybindHandler.ORE_MINE_ANY_BLOCK;

public class OreMiningUpgrade extends AbstractUpgrade {

    public OreMiningUpgrade() {
        super(UpgradeType.OREMINING);
    }

    @Override
    public List<UpgradeDataSync.Field> getSettings() {
        return List.of(UpgradeDataSync.Field.OREMINING_ORES_ONLY);
    }

    @Override
    public Map<UpgradeDataSync.Field, Boolean> getDefaultSettings() {
        return Map.of(UpgradeDataSync.Field.OREMINING_ORES_ONLY, true);
    }

    @Override
    public @Nullable UpgradePanel createPanel(UpgradeContext context) {
        return new OreMiningPanel(context);
    }

    @Override
    public boolean onBlockBreak(UpgradeContext context, BlockEvent.BreakEvent event) {
        Player player = context.player();
        Level level = player.level();
        BlockState state = event.getState();
        ItemStack tool = player.getMainHandItem();

        if (!tool.isCorrectToolForDrops(state) && !state.is(ModTags.Blocks.BREAKABLE_WITH_ANY_TOOL)) {
            return false;
        }

        boolean serverOverride = ConfigManager.ServerConfig.ORE_MINING_ORES_ONLY.get();
        boolean playerOresOnly = UpgradeDataManager.loadFromItem(context.backpack())
                .getSetting(UpgradeDataSync.Field.OREMINING_ORES_ONLY);

        if (!serverOverride && playerOresOnly && !state.is(ModTags.Blocks.ORE_MINING_BLOCK))
            return false;
        if (serverOverride)
            return false;

        boolean mineAllBlocks = player.getPersistentData()
                .getCompound(ConfigManager.FXNTSTORAGE_SETTINGS_TAG)
                .getBoolean("MineAllBlocks");

        mineVein(level, event.getPos(), player, mineAllBlocks, 64);
        return true; // cancel the original break
    }

    private void mineVein(Level level, BlockPos origin, Player player, boolean mineAllBlocks, int maxBlocks) {
        BlockState targetState = level.getBlockState(origin);
        List<BlockPos> vein = findVein(player, level, origin, targetState, mineAllBlocks, maxBlocks);

        List<ItemStack> drops = new ArrayList<>();
        ItemStack tool = player.getMainHandItem();

        int blocksMined = 0;

        for (BlockPos pos : vein) {
            if (tool.isEmpty()) break;

            BlockState state = level.getBlockState(pos);
            if (level.isEmptyBlock(pos)) continue;

            BlockEntity blockEntity = level.getBlockEntity(pos);

            LootParams.Builder lootParams = new LootParams.Builder((ServerLevel) level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.TOOL, tool)
                    .withParameter(LootContextParams.BLOCK_STATE, state)
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);

            List<ItemStack> blockDrops = state.getDrops(lootParams);
            drops.addAll(blockDrops);

            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            state.onDestroyedByPlayer(level, pos, player, true, level.getFluidState(pos));

            if (state.getDestroySpeed(level, pos) >= 0.0F) {
                tool.getItem().mineBlock(tool, level, state, pos, player);
                if (!player.getAbilities().instabuild) {
                    player.causeFoodExhaustion(0.2F);
                }
            }

            blocksMined++;
        }

        List<ItemStack> condensed = new ArrayList<>();

        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                boolean merged = false;

                for (ItemStack existing : condensed) {
                    if (ItemStack.isSameItemSameTags(existing, stack) && existing.isStackable()) {
                        int transferable = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                        if (transferable > 0) {
                            existing.grow(transferable);
                            stack.shrink(transferable);
                            if (stack.isEmpty()) {
                                merged = true;
                                break;
                            }
                        }
                    }
                }

                if (!merged && !stack.isEmpty()) {
                    condensed.add(stack.copy());
                }
            }
        }

        for (ItemStack drop : condensed) {
            Block.popResource(level, origin, drop);
        }

        if (!FMLEnvironment.production) {
            Component msg = Component.literal("Successfully mined §a" + blocksMined + "§r out of §a" + vein.size() + "§r");
            player.displayClientMessage(msg, false);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendToPlayer(serverPlayer, new OreMiningPreviewPacket(List.of()));
        }
    }

    public static List<BlockPos> findVein(Player player, Level level, BlockPos start, BlockState targetState, boolean mineAllBlocks, int maxBlocks) {
        if (!mineAllBlocks) return List.of(start);

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toVisit = new ArrayDeque<>();
        toVisit.add(start);

        while (!toVisit.isEmpty() && visited.size() < maxBlocks) {
            BlockPos current = toVisit.poll();
            if (!visited.add(current)) continue; // skip if already visited

            // Check all 26 surrounding positions in a 3x3x3 cube
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue; // Skip the center block

                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (visited.contains(neighbor)) continue;

                        BlockState neighborState = level.getBlockState(neighbor);
                        if (!neighborState.getBlock().equals(targetState.getBlock())) continue;

                        toVisit.add(neighbor);
                    }
                }
            }
        }

        // Sort positions by distance from player
        return visited.stream()
                .sorted(Comparator.comparingDouble(blockPos -> blockPos.distSqr(player.blockPosition())))
                .toList();
    }


    public static class OreMiningPanel implements UpgradePanel {
        private final List<ItemSpriteButton<OreMiningState>> stateButtons = new ArrayList<>();

        public record OreMiningState(boolean oresOnly, boolean oresPreview) {
        }

        private int panelX;
        private int panelY;

        private final UpgradeContext context;
        private final List<AbstractWidget> widgets = new ArrayList<>();

        public OreMiningPanel(UpgradeContext context) {
            this.context = context;
        }

        public void setPanelPosition(int leftPos, int imageWidth, int tabY) {
            int oldY = this.panelY;

            this.panelX = leftPos + imageWidth;
            this.panelY = tabY - 10;

            int deltaY = this.panelY - oldY;

            for (AbstractWidget widget : widgets) {
                widget.setPosition(widget.getX(), widget.getY() + deltaY);
            }
        }

        private OreMiningState getState() {
            BackpackMenu menu = context.menu();
            return new OreMiningState(
                    menu.isUpgradeSettingEnabled(UpgradeDataSync.Field.OREMINING_ORES_ONLY),
                    menu.isUpgradeSettingEnabled(UpgradeDataSync.Field.OREMINING_PREVIEW_ORE_VEIN)
            );
        }

        @Override
        public void createWidgets(Consumer<AbstractWidget> widgetAdder) {
            BackpackMenu menu = context.menu();

            OreMiningState initialState = getState();
            boolean serverMineOresOnly = ConfigManager.ServerConfig.ORE_MINING_ORES_ONLY.get();
            boolean serverPreviewOreVein = ConfigManager.ServerConfig.ORE_MINING_PREVIEW_ORE_VEIN.get();

            if (!serverPreviewOreVein) {
                menu.setUpgradeSetting(UpgradeDataSync.Field.OREMINING_PREVIEW_ORE_VEIN, false);
            }

            Component mineOresOnlyTooltip = serverMineOresOnly
                    ? Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.mine_any_block").withStyle(ChatFormatting.WHITE).append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.server_override").withStyle(ChatFormatting.DARK_RED).withStyle(ChatFormatting.BOLD)).append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.mine_any_block.description").withStyle(ChatFormatting.DARK_GRAY))
                    : Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.mine_any_block").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.mine_any_block.description").withStyle(ChatFormatting.DARK_GRAY));

            Component previewOreVeinTooltip = serverPreviewOreVein
                    ? Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.hide_ore_vein_preview").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.hide_ore_vein_preview.description").withStyle(ChatFormatting.DARK_GRAY))
                    : Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.hide_ore_vein_preview").withStyle(ChatFormatting.WHITE).append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.server_override").withStyle(ChatFormatting.DARK_RED).withStyle(ChatFormatting.BOLD)).append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.hide_ore_vein_preview.description").withStyle(ChatFormatting.DARK_GRAY));

            stateButtons.add(
                    new ItemSpriteButton<>(
                            panelX + 4, panelY + 32, 18, 18,
                            initialState,
                            state -> state.oresOnly()
                                    ? modLoc("textures/gui/sprites/check.png")
                                    : serverMineOresOnly
                                    ? modLoc("textures/gui/sprites/cross.png")
                                    : modLoc("textures/gui/sprites/tilde.png"),
                            state -> state.oresOnly()
                                    ? Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.mine_ores_only").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.mine_ores_only.description").withStyle(ChatFormatting.DARK_GRAY))
                                    : mineOresOnlyTooltip,
                            state -> state.oresOnly() ? new ItemStack(Blocks.COAL_ORE) : new ItemStack(Blocks.STONE), 14,
                            button -> menu.toggleUpgradeSetting(UpgradeDataSync.Field.OREMINING_ORES_ONLY)
                    )
            );

            stateButtons.add(
                    new ItemSpriteButton<>(
                            panelX + 26, panelY + 32, 18, 18,
                            initialState,
                            state -> state.oresPreview()
                                    ? modLoc("textures/gui/sprites/check.png")
                                    : modLoc("textures/gui/sprites/cross.png"),
                            state -> state.oresPreview()
                                    ? Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.preview_ore_veins").append("\n").append(Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.preview_ore_veins.description_before_key").withStyle(ChatFormatting.DARK_GRAY)).append(Component.literal(ORE_MINE_ANY_BLOCK.getKey().getDisplayName().getString()).withStyle(ChatFormatting.GRAY)).append(Component.translatable("tooltip.fxntstorage.backpack_oremining_upgrade.panel.preview_ore_veins.description_after_key").withStyle(ChatFormatting.DARK_GRAY))
                                    : previewOreVeinTooltip,
                            state -> new ItemStack(Items.ENDER_EYE), 14,
                            button -> {
                                if (serverPreviewOreVein) {
                                    menu.toggleUpgradeSetting(UpgradeDataSync.Field.OREMINING_PREVIEW_ORE_VEIN);
                                }
                            }
                    )
            );

            stateButtons.forEach(button -> {
                widgetAdder.accept(button);
                widgets.add(button);
            });
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(
                    PANEL_TEXTURE,
                    panelX - 3, panelY + 8,
                    0, 0,
                    PANEL_EXPANDED_WIDTH,
                    PANEL_EXPANDED_HEIGHT,
                    128, 128
            );
        }

        @Override
        public void renderTooltip(Font font, GuiGraphics graphics, int mouseX, int mouseY, Slot hoveredSlot) {
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false; // Handled by widgets
        }

        @Override
        public void tick() {
            stateButtons.forEach(button -> button.updateState(getState()));
        }

        public List<AbstractWidget> getWidgets() {
            return widgets;
        }

        public void clearWidgets() {
            widgets.clear();
            stateButtons.clear();
        }
    }
}
