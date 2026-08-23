package net.fxnt.fxntstorage.backpack.upgrade.jukebox;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.client.menu.button.SpriteButton;
import net.fxnt.fxntstorage.backpack.client.menu.button.WidgetSprites;
import net.fxnt.fxntstorage.backpack.client.menu.slot.JukeboxDiscSlot;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.init.ModNetwork;
import net.fxnt.fxntstorage.network.packet.JukeboxServerPacket;
import net.fxnt.fxntstorage.util.ParticleHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class JukeboxUpgrade extends AbstractUpgrade {

    public JukeboxUpgrade() {
        super(UpgradeType.JUKEBOX);
    }

    @Override
    public List<UpgradeDataSync.Field> getSettings() {
        return List.of(
                UpgradeDataSync.Field.JUKEBOX_PLAYING,
                UpgradeDataSync.Field.JUKEBOX_MUTED
        );
    }

    @Override
    public Map<UpgradeDataSync.Field, Boolean> getDefaultSettings() {
        return Map.of(
                UpgradeDataSync.Field.JUKEBOX_PLAYING, false,
                UpgradeDataSync.Field.JUKEBOX_MUTED, false
        );
    }

    @Override
    public List<Slot> createSlots(UpgradeContext context) {
        BackpackMenu menu = context.menu();
        return List.of(
                new JukeboxDiscSlot(
                        menu.container,
                        menu.layout.jukeboxDiscs().getStartIndex(),
                        275, 34,
                        context.player(),
                        context.backpackType(),
                        context.blockPos(),
                        () -> menu.hasUpgrade(UpgradeType.JUKEBOX),
                        () -> menu.isPanelExpanded(UpgradeType.JUKEBOX),
                        menu::updateBackpackDataFromContainer,
                        () -> stopPlayback(context)
                )
        );
    }

    @Override
    public @Nullable UpgradePanel createPanel(UpgradeContext context) {
        return new JukeboxPanel(context);
    }

    @Override
    public void onRemoved(UpgradeContext context) {
        if (context.isClientSide()) {
            stopPlayback(context);
            return;
        }

        stopPlaybackServer(context);
        returnOrphanedDisc(context);
    }

    @Override
    public void validateContents(UpgradeContext context) {
        if (context.isClientSide()) return;

        stopOrphanedPlayback(context);
        returnOrphanedDisc(context);
    }

    @Override
    public Optional<ItemStack> onQuickMove(UpgradeContext context) {
        if (!(context.menu() instanceof BackpackMenu menu)
                || !menu.hasActiveUpgrade(UpgradeType.JUKEBOX)
                || !menu.isPanelExpanded(UpgradeType.JUKEBOX)
        ) return Optional.empty();

        int discSlotIndex = menu.layout.jukeboxDiscs().getStartIndex();
        int slotIndex = context.slotId();
        ItemStack slotItem = menu.slots.get(slotIndex).getItem();

        if (slotIndex == discSlotIndex) {
            boolean moved = menu.moveStackToPlayerInventory(slotItem);
            if (!moved) return Optional.empty();

            Slot discSlot = menu.getSlot(discSlotIndex);
            discSlot.set(ItemStack.EMPTY);

            return Optional.of(ItemStack.EMPTY);
        }

        if (slotItem.getItem() instanceof RecordItem && !menu.layout.jukeboxDiscs().contains(slotIndex)) {
            Slot discSlot = menu.getSlot(discSlotIndex);

            if (discSlot.getItem().isEmpty()) {
                discSlot.safeInsert(slotItem.split(1));
                if (slotItem.isEmpty()) menu.getSlot(slotIndex).set(ItemStack.EMPTY);
                menu.getSlot(slotIndex).setChanged();
                return Optional.of(ItemStack.EMPTY);
            }
        }

        return Optional.empty();
    }

    @Override
    protected void tickActive(UpgradeContext context) {
        if (context.backpackType() == BackpackMenu.BackpackType.BLOCK) {
            JukeboxUpgradeHelper.getMusicDisc(null, context.level(), context.blockPos())
                    .ifPresent(musicDisc -> {
                        Level level = context.level();
                        BlockPos pos = context.blockPos();
                        long gameTime = level.getGameTime();

                        boolean isPlaying = JukeboxHandler.isBlockPlaying(level, pos);

                        if (!isPlaying) return;

                        if (ConfigManager.ServerConfig.JUKEBOX_NOTES_ENABLED.get()) {
                            int particleInterval = level.getRandom().nextInt(31) + 10;
                            if (gameTime % particleInterval == 0)
                                ParticleHelper.jukeboxParticles(level, pos);
                        }

                        if (!ConfigManager.ServerConfig.JUKEBOX_BUFFS_ENABLED.get()) return;
                        if (gameTime % 40 != 0) return;

                        if (!(musicDisc.getItem() instanceof RecordItem discItem)) return;
                        var soundEvent = discItem.getSound();

                        int range = ConfigManager.ServerConfig.JUKEBOX_BUFFS_RANGE.get();
                        AABB area = new AABB(pos).inflate(range);

                        for (Player player : level.getEntitiesOfClass(Player.class, area)) {
                            JukeboxBuffHandler.applyMusicBuffsFromBlock(player, soundEvent.getLocation());
                        }
                    });
        } else if (context.backpackType() == BackpackMenu.BackpackType.WORN) {
            Player player = context.player();
            boolean isPlaying = JukeboxHandler.isPlayerPlaying((ServerPlayer) player);
            boolean isBackpackVisible = BackpackHelper.isWearingBackpack(player, true);

            if (!isBackpackVisible || !isPlaying) return;

            if (ConfigManager.ServerConfig.JUKEBOX_NOTES_ENABLED.get()) {
                int particleInterval = player.getRandom().nextInt(31) + 10;
                if (player.level().getGameTime() % particleInterval == 0)
                    ParticleHelper.jukeboxParticles(player);
            }
        }
    }

    // ======= HELPER METHODS =======
    private void returnOrphanedDisc(UpgradeContext context) {
        IBackpackContainer container = resolveContainer(context);
        if (container == null) return;

        ItemStackHandler itemHandler = container.getItemHandler();
        if (itemHandler == null || UpgradeHelper.hasUpgrade(itemHandler, UpgradeType.JUKEBOX)) return;

        int discSlotIndex = BackpackSlotLayout.createLayout().jukeboxDiscs().getStartIndex();
        ItemStack disc = itemHandler.getStackInSlot(discSlotIndex);
        if (disc.isEmpty()) return;

        // Never clear the slot without somewhere to put the disc
        Player player = context.player();
        boolean canDropAtBlock = context.level() != null && context.blockPos() != null;
        if (player == null && !canDropAtBlock) return;

        ItemStack returnedDisc = disc.copy();
        itemHandler.setStackInSlot(discSlotIndex, ItemStack.EMPTY);
        container.setDataChanged();

        if (player != null) {
            player.getInventory().add(returnedDisc);
            if (!returnedDisc.isEmpty()) {
                player.drop(returnedDisc, false);
            }
        } else {
            BlockPos pos = context.blockPos();
            Containers.dropItemStack(context.level(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, returnedDisc);
        }
    }

    private void stopOrphanedPlayback(UpgradeContext context) {
        IBackpackContainer container = resolveContainer(context);
        if (container == null) return;

        ItemStackHandler itemHandler = container.getItemHandler();
        if (itemHandler == null || UpgradeHelper.hasUpgrade(itemHandler, UpgradeType.JUKEBOX)) return;

        if (context.backpackType() == BackpackMenu.BackpackType.WORN) {
            if (context.player() instanceof ServerPlayer serverPlayer && JukeboxHandler.isPlayerPlaying(serverPlayer)) {
                JukeboxHandler.stopPlayer(serverPlayer);
            }
        } else if (context.backpackType() == BackpackMenu.BackpackType.BLOCK
                && context.level() != null && context.blockPos() != null
                && JukeboxHandler.isBlockPlaying(context.level(), context.blockPos())) {
            JukeboxHandler.stopBlock(context.level(), context.blockPos());
        }
    }

    @Nullable
    private IBackpackContainer resolveContainer(UpgradeContext context) {
        if (context.container() != null) return context.container();
        if (context.menu() instanceof BackpackMenu menu) return menu.container;
        return null;
    }

    private void stopPlayback(UpgradeContext context) {
        if (!context.isClientSide()) return;

        if (context.backpackType().equals(BackpackMenu.BackpackType.WORN)) {
            ClientJukeboxHandler.stopPlayer(context.player().getUUID());
            ModNetwork.sendToServer(new JukeboxServerPacket(
                    JukeboxServerPacket.Action.STOP,
                    JukeboxServerPacket.Source.PLAYER,
                    Optional.empty()
            ));
        } else {
            if (context.blockPos() != null) {
                ClientJukeboxHandler.stopBlock(context.blockPos());
                ModNetwork.sendToServer(new JukeboxServerPacket(
                        JukeboxServerPacket.Action.STOP,
                        JukeboxServerPacket.Source.BLOCK,
                        Optional.ofNullable(context.blockPos())
                ));
            }
        }
    }

    private void stopPlaybackServer(UpgradeContext context) {
        if (context.isClientSide()) return;

        Player player = context.player();
        if (context.backpackType() == BackpackMenu.BackpackType.WORN
                && player instanceof ServerPlayer sp) {
            JukeboxHandler.stopPlayer(sp);
        } else if (context.backpackType() == BackpackMenu.BackpackType.BLOCK
                && context.blockPos() != null) {
            JukeboxHandler.stopBlock(context.level(), context.blockPos());
        }
    }

    public static class JukeboxPanel implements UpgradePanel {
        private final List<SpriteButton<JukeboxState>> stateButtons = new ArrayList<>();

        public record JukeboxState(boolean playing, boolean muted) {
        }

        private static final WidgetSprites JUKEBOX_PLAY = UpgradePanel.createWidgetSprites(
                "play", "play_disabled", "play_highlight", "play_disabled"
        );
        private static final WidgetSprites JUKEBOX_STOP = UpgradePanel.createWidgetSprites("stop");
        private static final WidgetSprites JUKEBOX_UNMUTED = UpgradePanel.createWidgetSprites(
                "unmuted", "unmuted_disabled", "unmuted_highlight", "unmuted_disabled"
        );
        private static final WidgetSprites JUKEBOX_MUTED = UpgradePanel.createWidgetSprites("muted");

        private final UpgradeContext context;
        private final List<AbstractWidget> widgets = new ArrayList<>();

        private int panelX;
        private int panelY;

        public JukeboxPanel(UpgradeContext context) {
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

        private JukeboxState getState() {
            boolean playing = switch (context.backpackType()) {
                case WORN -> ClientJukeboxHandler.isPlayerPlaying(context.player());
                case BLOCK -> context.blockPos() != null
                        && ClientJukeboxHandler.isBlockPlaying(context.blockPos());
                default -> false;
            };
            boolean muted = switch (context.backpackType()) {
                case WORN -> ClientJukeboxHandler.isPlayerMuted(context.player());
                case BLOCK -> context.blockPos() != null
                        && ClientJukeboxHandler.isBlockMuted(context.blockPos());
                default -> false;
            };

            return new JukeboxState(playing, muted);
        }

        @Override
        public void createWidgets(Consumer<AbstractWidget> widgetAdder) {
            JukeboxState initialState = getState();

            stateButtons.add(
                    new SpriteButton<>(
                            panelX + 26, panelY + 32, 18, 18,
                            initialState,
                            state -> state.playing() ? JUKEBOX_STOP : JUKEBOX_PLAY,
                            state -> state.playing()
                                    ? Component.translatable("tooltip.fxntstorage.backpack_jukebox_upgrade.panel.stop")
                                    : Component.translatable("tooltip.fxntstorage.backpack_jukebox_upgrade.panel.play"),
                            button -> togglePlaying()
                    )
            );

            stateButtons.add(
                    new SpriteButton<>(
                            panelX + 48, panelY + 32, 18, 18,
                            initialState,
                            state -> state.muted() ? JUKEBOX_MUTED : JUKEBOX_UNMUTED,
                            state -> state.muted()
                                    ? Component.translatable("tooltip.fxntstorage.backpack_jukebox_upgrade.panel.muted")
                                    : Component.translatable("tooltip.fxntstorage.backpack_jukebox_upgrade.panel.unmuted"),
                            button -> toggleMuted()
                    )
            );

            boolean isActive = UpgradeHelper.hasActiveUpgrade(context.itemHandler(), UpgradeType.JUKEBOX);
            stateButtons.forEach(button -> {
                button.active = isActive;
                if (!isActive) button.setTooltip(Tooltip.create(Component.empty()));
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
            return false;
        }

        @Override
        public void tick() {
            stateButtons.forEach(button -> button.updateState(getState()));
        }

        // Action methods
        private void togglePlaying() {
            if (!context.isClientSide()) return;

            BackpackMenu menu = context.menu();
            if (!menu.hasActiveUpgrade(UpgradeType.JUKEBOX)) return;

            menu.toggleUpgradeSetting(UpgradeDataSync.Field.JUKEBOX_PLAYING);

            switch (context.backpackType()) {
                case WORN -> {
                    ClientJukeboxHandler.stopPlayer(context.player().getUUID());
                    ModNetwork.sendToServer(new JukeboxServerPacket(
                            JukeboxServerPacket.Action.TOGGLE_PLAYING,
                            JukeboxServerPacket.Source.PLAYER,
                            Optional.empty()
                    ));
                }
                case BLOCK -> {
                    if (context.blockPos() != null) {
                        ClientJukeboxHandler.stopBlock(context.blockPos());
                        ModNetwork.sendToServer(new JukeboxServerPacket(
                                JukeboxServerPacket.Action.TOGGLE_PLAYING,
                                JukeboxServerPacket.Source.BLOCK,
                                Optional.ofNullable(context.blockPos())
                        ));
                    }
                }
            }
        }

        private void toggleMuted() {
            if (!context.isClientSide()) return;

            BackpackMenu menu = context.menu();
            if (!menu.hasActiveUpgrade(UpgradeType.JUKEBOX)) return;

            menu.toggleUpgradeSetting(UpgradeDataSync.Field.JUKEBOX_MUTED);

            if (context.backpackType().equals(BackpackMenu.BackpackType.WORN)) {
                ClientJukeboxHandler.toggleMutePlayer(context.player());
                ModNetwork.sendToServer(new JukeboxServerPacket(
                        JukeboxServerPacket.Action.TOGGLE_MUTED,
                        JukeboxServerPacket.Source.PLAYER,
                        Optional.empty()
                ));
            } else {
                if (context.blockPos() != null) {
                    ClientJukeboxHandler.toggleMuteBlock(context.blockPos());
                    ModNetwork.sendToServer(new JukeboxServerPacket(
                            JukeboxServerPacket.Action.TOGGLE_MUTED,
                            JukeboxServerPacket.Source.BLOCK,
                            Optional.ofNullable(context.blockPos())
                    ));
                }
            }
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
