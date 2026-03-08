package net.fxnt.fxntstorage.backpack.upgrade.jukebox;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.client.menu.button.SpriteButton;
import net.fxnt.fxntstorage.backpack.client.menu.slot.JukeboxDiscSlot;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.backpack.util.BackpackHelper;
import net.fxnt.fxntstorage.config.ClientSettings;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.network.packet.JukeboxServerPacket;
import net.fxnt.fxntstorage.util.ParticleHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static net.fxnt.fxntstorage.FXNTStorage.modLoc;


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
    public boolean clicked(UpgradeContext context) {
        BackpackSlotLayout layout = BackpackSlotLayout.createLayout();

        if (layout.jukeboxDiscs().contains(context.slotId())) {
            Slot slot = context.player().containerMenu.slots.get(context.slotId());
            ItemStack existing = slot.getItem();

            if (!existing.isEmpty() && existing.has(DataComponents.JUKEBOX_PLAYABLE)) {
                onRemoved(context);
            }
        }
        return false;
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
        } else {
            stopPlaybackServer(context);

            if (context.menu() instanceof BackpackMenu menu) {
                int discSlotIndex = menu.layout.jukeboxDiscs().getStartIndex();
                Slot discSlot = menu.slots.get(discSlotIndex);
                ItemStack disc = discSlot.getItem();

                if (!disc.isEmpty() && disc.has(DataComponents.JUKEBOX_PLAYABLE)) {
                    boolean moved = menu.moveStackToPlayerInventory(disc);

                    if (!moved) {
                        context.player().drop(disc.copy(), false);
                    }

                    discSlot.set(ItemStack.EMPTY);
                    discSlot.setChanged();
                }
            }
        }
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

            if (context.level().isClientSide()) {
                stopPlayback(context);
            } else {
                stopPlaybackServer(context);
                menu.updateBackpackDataFromContainer();
            }

            return Optional.of(ItemStack.EMPTY);
        }

        if (slotItem.has(DataComponents.JUKEBOX_PLAYABLE) && !menu.layout.jukeboxDiscs().contains(slotIndex)) {
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
        boolean buffsEnabled = ConfigManager.ServerConfig.JUKEBOX_BUFFS_ENABLED.get();

        if (context.backpackType() == BackpackMenu.BackpackType.BLOCK) {
            if (context.level().getGameTime() % 40 != 0)
                return;

            JukeboxUpgradeHelper.getMusicDisc(null, context.level(), context.blockPos())
                    .ifPresent(musicDisc -> {
                        Level level = context.level();
                        BlockPos pos = context.blockPos();

                        boolean isPlaying = JukeboxHandler.isBlockPlaying(level, pos);
                        boolean notesEnabled = ConfigManager.ServerConfig.JUKEBOX_NOTES_ENABLED.get();

                        if (!isPlaying) return;

                        if (notesEnabled)
                            ParticleHelper.jukeboxParticles(level, pos);

                        if (!buffsEnabled) return;

                        JukeboxPlayable playable = musicDisc.get(DataComponents.JUKEBOX_PLAYABLE);
                        if (playable == null) return;

                        int range = ConfigManager.ServerConfig.JUKEBOX_BUFFS_RANGE.getAsInt();
                        AABB area = new AABB(pos).inflate(range);

                        for (Player player : level.getEntitiesOfClass(Player.class, area)) {
                            JukeboxBuffHandler.applyMusicBuffsFromBlock(player, playable.song().key().location());
                        }
                    });
        } else if (context.backpackType() == BackpackMenu.BackpackType.WORN) {
            if (context.player().getRandom().nextInt(20) != 0)
                return;

            Player player = context.player();
            boolean isPlaying = JukeboxHandler.isPlayerPlaying((ServerPlayer) player);
            boolean isBackpackVisible = BackpackHelper.isWearingBackpack(player, true);

            if (!isBackpackVisible || !isPlaying) return;
            boolean notesEnabled = ClientSettings.getBoolean(player.getUUID(), "JukeboxNotesEnabled");

            if (notesEnabled)
                ParticleHelper.jukeboxParticles(player);
        }
    }

    // ======= HELPER METHODS =======
    private void stopPlayback(UpgradeContext context) {
        if (!context.isClientSide()) return;

        if (context.backpackType().equals(BackpackMenu.BackpackType.WORN)) {
            ClientJukeboxHandler.stopPlayer(context.player().getUUID());
            PacketDistributor.sendToServer(new JukeboxServerPacket(
                    JukeboxServerPacket.Action.STOP,
                    JukeboxServerPacket.Source.PLAYER,
                    Optional.empty()
            ));
        } else {
            if (context.blockPos() != null) {
                ClientJukeboxHandler.stopBlock(context.blockPos());
                PacketDistributor.sendToServer(new JukeboxServerPacket(
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

        private static final WidgetSprites JUKEBOX_PLAY = new WidgetSprites(
                modLoc("play"), modLoc("play_disabled"), modLoc("play_highlight"), modLoc("play_disabled")
        );
        private static final WidgetSprites JUKEBOX_STOP = new WidgetSprites(modLoc("stop"), modLoc("stop_highlight"));
        private static final WidgetSprites JUKEBOX_UNMUTED = new WidgetSprites(
                modLoc("unmuted"), modLoc("unmuted_disabled"), modLoc("unmuted_highlight"), modLoc("unmuted_disabled")
        );
        private static final WidgetSprites JUKEBOX_MUTED = new WidgetSprites(modLoc("muted"), modLoc("muted_highlight"));

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
                    PacketDistributor.sendToServer(new JukeboxServerPacket(
                            JukeboxServerPacket.Action.TOGGLE_PLAYING,
                            JukeboxServerPacket.Source.PLAYER,
                            Optional.empty()
                    ));
                }
                case BLOCK -> {
                    if (context.blockPos() != null) {
                        ClientJukeboxHandler.stopBlock(context.blockPos());
                        PacketDistributor.sendToServer(new JukeboxServerPacket(
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
                PacketDistributor.sendToServer(new JukeboxServerPacket(
                        JukeboxServerPacket.Action.TOGGLE_MUTED,
                        JukeboxServerPacket.Source.PLAYER,
                        Optional.empty()
                ));
            } else {
                if (context.blockPos() != null) {
                    ClientJukeboxHandler.toggleMuteBlock(context.blockPos());
                    PacketDistributor.sendToServer(new JukeboxServerPacket(
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
