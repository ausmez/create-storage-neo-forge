package net.fxnt.fxntstorage.backpack.upgrade.workshop;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.kinetics.press.PressingBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
import net.fxnt.fxntstorage.backpack.BackpackEntity;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackMenu;
import net.fxnt.fxntstorage.backpack.client.menu.slot.WorkshopSlot;
import net.fxnt.fxntstorage.backpack.inventory.BackpackSlotLayout;
import net.fxnt.fxntstorage.backpack.inventory.IBackpackContainer;
import net.fxnt.fxntstorage.backpack.upgrade.*;
import net.fxnt.fxntstorage.config.ConfigManager;
import net.fxnt.fxntstorage.network.packet.WorkshopProcessingPacket;
import net.fxnt.fxntstorage.network.packet.WorkshopSoundPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WorkshopUpgrade extends AbstractUpgrade {
    // Slot offsets within the layout's workshop section.
    public static final int MACHINE_SLOT = 0;
    public static final int HELD_SLOT = 1;
    public static final int BACKTANK_SLOT = 2;
    public static final int INPUT_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;

    private static final int AIR_PER_OPERATION = 2;
    private static final int PROCESSING_HEARTBEAT_TICKS = 40;

    private static final int PRESS_CYCLE = PressingBehaviour.CYCLE;
    private static final int DEPLOYER_EXTEND_TIMER = 1000; // EXPANDING / RETRACTING timer
    private static final int DEPLOYER_GAP_TIMER = 176; // Time (ms) between expanding and retracting

    // Emulate the press' pause at the bottom of its stroke to match in-world pace
    private static final int PRESS_BOTTOM_HOLD = 1;

    // Deployer cycle states (mirroring DeployerBlockEntity.State)
    private static final int DEPLOYER_EXPANDING = 0;
    private static final int DEPLOYER_RETRACTING = 1;
    private static final int DEPLOYER_WAITING = 2;

    private static final float FLYWHEEL_DECEL = 0.3f;
    private static final int SPIN_UP_TICKS = 30;

    private static final BackpackSlotLayout LAYOUT = BackpackSlotLayout.createLayout();
    private static final Map<IBackpackContainer, ProcessingState> STATES = Collections.synchronizedMap(new WeakHashMap<>());

    public static int kineticSpeed() {
        return ConfigManager.ServerConfig.WORKSHOP_KINETIC_SPEED.get();
    }

    private static int pressRunningTickSpeed(float speed) {
        return Math.max(1, (int) Mth.lerp(Mth.clamp(Math.abs(speed) / 512f, 0f, 1f), 1f, 60f));
    }

    private static int deployerTimerSpeed(float speed) {
        return (int) Mth.clamp(Math.abs(speed * 2f), 8f, 512f);
    }

    private static float appliedSpeed(ProcessingState state) {
        return kineticSpeed() * state.spinFraction;
    }

    private static float spinDownRampPerTick() {
        float fullSpeed = Math.abs(kineticSpeed() * 3f / 10f);
        return fullSpeed <= 0f ? 1f : FLYWHEEL_DECEL / fullSpeed;
    }

    private static float spinUpRampPerTick() {
        return Math.max(spinDownRampPerTick(), 1f / SPIN_UP_TICKS);
    }

    private static void updateSpin(ProcessingState state, boolean shouldSpin) {
        state.spinning = shouldSpin;
        state.spinFraction = shouldSpin
                ? Math.min(1f, state.spinFraction + spinUpRampPerTick())
                : Math.max(0f, state.spinFraction - spinDownRampPerTick());
    }

    public static boolean isSpinning(@Nullable IBackpackContainer container) {
        if (container == null) return false;
        ProcessingState state = STATES.get(container);
        return state != null && state.spinning;
    }

    private static final class ProcessingState {
        boolean processing;
        int offsetMillis;    // head/hand extension * 1000, synced to drive the panel animation
        int runningTicks;    // press counter (PressingBehaviour.runningTicks)
        int pressHold;       // remaining ticks of the press's bottom-of-stroke hold
        boolean pressStruck; // whether this press cycle has already applied its recipe
        int timer;           // deployer countdown (DeployerBlockEntity.timer)
        int deployerState;   // DEPLOYER_EXPANDING / DEPLOYER_RETRACTING / DEPLOYER_WAITING

        // Flywheel spin model
        boolean spinning;
        float spinFraction;

        Boolean broadcastProcessing;
        int heartbeatTicks;

        boolean wasProcessing;
    }

    public WorkshopUpgrade() {
        super(UpgradeType.WORKSHOP);
    }

    public static int getSyncValue(@Nullable IBackpackContainer container) {
        if (container == null) return 0;
        ProcessingState state = STATES.get(container);
        if (state == null || !state.processing) return 0;
        return state.offsetMillis + 1;
    }

    @Override
    public List<Slot> createSlots(UpgradeContext context) {
        BackpackMenu menu = context.menu();
        IBackpackContainer container = menu.container;
        int start = menu.layout.workshop().getStartIndex();

        BooleanSupplier hasUpgrade = () -> menu.hasUpgrade(UpgradeType.WORKSHOP);
        BooleanSupplier expanded = () -> menu.isPanelExpanded(UpgradeType.WORKSHOP);

        Predicate<ItemStack> machinePredicate = WorkshopRecipeHelper::isMachine;
        Predicate<ItemStack> backtankPredicate = WorkshopRecipeHelper::isBacktank;
        Predicate<ItemStack> heldItemPredicate = stack -> {
            int index = menu.layout.workshop().getStartIndex() + WorkshopUpgrade.MACHINE_SLOT;
            ItemStack machine = menu.container.getItemHandler().getStackInSlot(index);
            return machine.isEmpty() || machine.getItem().equals(AllBlocks.DEPLOYER.asItem());
        };
        Predicate<ItemStack> anyPredicate = stack -> true;

        List<Slot> slots = new ArrayList<>(5);
        slots.add(new WorkshopSlot(container, start + MACHINE_SLOT, 0, 0, hasUpgrade, expanded, machinePredicate) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        });
        slots.add(new WorkshopSlot(container, start + HELD_SLOT, 0, 0, hasUpgrade, expanded, heldItemPredicate));
        slots.add(new WorkshopSlot(container, start + BACKTANK_SLOT, 0, 0, hasUpgrade, expanded, backtankPredicate));
        slots.add(new WorkshopSlot(container, start + INPUT_SLOT, 0, 0, hasUpgrade, expanded, anyPredicate));
        slots.add(new WorkshopSlot(container, start + OUTPUT_SLOT, 0, 0, hasUpgrade, expanded, null));
        return slots;
    }

    @Override
    public @Nullable UpgradePanel createPanel(UpgradeContext context) {
        return new WorkshopPanel(context.menu());
    }

    @Override
    protected void tickActive(UpgradeContext context) {
        if (context.isClientSide()) return;

        IBackpackContainer container = context.container();
        if (container == null) return;
        if (!(context.itemHandler() instanceof ItemStackHandler handler)) return;

        ProcessingState state = STATES.computeIfAbsent(container, k -> new ProcessingState());
        int start = LAYOUT.workshop().getStartIndex();
        if (handler.getSlots() <= start + OUTPUT_SLOT) {
            updateSpin(state, false);
            idle(state);
            syncProcessing(context, container, state);
            return;
        }

        ItemStack machine = handler.getStackInSlot(start + MACHINE_SLOT);
        ItemStack backtank = handler.getStackInSlot(start + BACKTANK_SLOT);

        if (!WorkshopRecipeHelper.isMachine(machine) || !WorkshopRecipeHelper.isBacktank(backtank)) {
            updateSpin(state, false);
            idle(state);
        } else if (WorkshopRecipeHelper.isDeployer(machine)) {
            tickDeployer(context, container, handler, start, state);
        } else {
            tickPress(context, container, handler, start, state);
        }

        syncProcessing(context, container, state);
    }

    private void syncProcessing(UpgradeContext context, IBackpackContainer container, ProcessingState state) {
        boolean spinning = state.spinning;

        if (state.wasProcessing && !state.processing) {
            spawnCompletionParticles(context);
        }
        state.wasProcessing = state.processing;

        if (context.player() instanceof ServerPlayer player) {
            boolean changed = state.broadcastProcessing == null || state.broadcastProcessing != spinning;
            boolean heartbeat = spinning && ++state.heartbeatTicks >= PROCESSING_HEARTBEAT_TICKS;
            if (changed || heartbeat) {
                state.broadcastProcessing = spinning;
                state.heartbeatTicks = 0;
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                        WorkshopProcessingPacket.forEntity(player.getId(), spinning));
            }
        } else if (container instanceof BackpackEntity blockBackpack) {
            blockBackpack.setWorkshopProcessing(spinning);
        }
    }

    private void tickPress(UpgradeContext context, IBackpackContainer container,
                           ItemStackHandler handler, int start, ProcessingState state) {
        Level level = context.level();
        ItemStack machine = handler.getStackInSlot(start + MACHINE_SLOT);
        ItemStack backtank = handler.getStackInSlot(start + BACKTANK_SLOT);
        ItemStack input = handler.getStackInSlot(start + INPUT_SLOT);
        ItemStack output = handler.getStackInSlot(start + OUTPUT_SLOT);

        boolean shouldSpin = state.processing || canStartPress(level, machine, input, output, backtank);
        updateSpin(state, shouldSpin);
        if (!shouldSpin) {
            idle(state);
            return;
        }

        if (!state.processing) {
            state.processing = true;
            state.runningTicks = 0;
            state.pressHold = 0;
            state.pressStruck = false;
        }

        if (state.pressHold > 0) {
            state.pressHold--;
            state.offsetMillis = 1000;
            return;
        }

        if (state.runningTicks == PRESS_CYCLE / 2 && !state.pressStruck) {
            WorkshopRecipeHelper.Result result = WorkshopRecipeHelper.resolve(level, machine, input, ItemStack.EMPTY);
            if (result != null && canInsert(output, result.output())) {
                produce(handler, start, input, ItemStack.EMPTY, output, result);
                consumeAir(handler, start);
                container.setDataChanged();
                playStrikeSound(context, level, machine, false);
            }
            state.pressStruck = true;
            if (PRESS_BOTTOM_HOLD > 0) {
                state.pressHold = PRESS_BOTTOM_HOLD;
                state.offsetMillis = 1000;
                return;
            }
        }

        // The press finishes (and loops to the next item) once runningTicks exceeds CYCLE
        if (state.runningTicks > PRESS_CYCLE) {
            state.runningTicks = 0;
            state.pressStruck = false;
            if (!canStartPress(level, machine, input, output, backtank)) {
                idle(state);
                return;
            }
        }

        int prev = state.runningTicks;
        state.runningTicks += pressRunningTickSpeed(appliedSpeed(state));
        if (prev < PRESS_CYCLE / 2 && state.runningTicks >= PRESS_CYCLE / 2) {
            state.runningTicks = PRESS_CYCLE / 2;
        }

        state.offsetMillis = Math.round(pressHeadOffset(state.runningTicks) * 1000f);
    }

    private void tickDeployer(UpgradeContext context, IBackpackContainer container,
                              ItemStackHandler handler, int start, ProcessingState state) {
        Level level = context.level();
        ItemStack machine = handler.getStackInSlot(start + MACHINE_SLOT);
        ItemStack held = handler.getStackInSlot(start + HELD_SLOT);
        ItemStack backtank = handler.getStackInSlot(start + BACKTANK_SLOT);
        ItemStack input = handler.getStackInSlot(start + INPUT_SLOT);
        ItemStack output = handler.getStackInSlot(start + OUTPUT_SLOT);

        boolean shouldSpin = state.processing || canStartDeployer(level, machine, input, held, output, backtank);
        updateSpin(state, shouldSpin);
        if (!shouldSpin) {
            idle(state);
            return;
        }

        if (!state.processing) {
            state.processing = true;
            state.deployerState = DEPLOYER_EXPANDING;
            state.timer = DEPLOYER_EXTEND_TIMER;
        }

        int ts = deployerTimerSpeed(appliedSpeed(state));
        state.timer -= ts;
        if (state.timer <= 0) {
            switch (state.deployerState) {
                case DEPLOYER_EXPANDING -> {
                    // Hand fully extended: apply the recipe, then begin retracting.
                    WorkshopRecipeHelper.Result result = WorkshopRecipeHelper.resolve(level, machine, input, held);
                    if (result != null && canInsert(output, result.output())) {
                        produce(handler, start, input, held, output, result);
                        consumeAir(handler, start);
                        container.setDataChanged();
                        playStrikeSound(context, level, machine, result.damageHeld());
                    }
                    state.deployerState = DEPLOYER_RETRACTING;
                    state.timer += DEPLOYER_EXTEND_TIMER;
                }
                case DEPLOYER_RETRACTING -> {
                    state.deployerState = DEPLOYER_WAITING;
                    state.timer += DEPLOYER_GAP_TIMER;
                }
                default -> { // WAITING: loop into the next item if one is available.
                    if (!canStartDeployer(level, machine, input, held, output, backtank)) {
                        idle(state);
                        return;
                    }
                    state.deployerState = DEPLOYER_EXPANDING;
                    state.timer += DEPLOYER_EXTEND_TIMER;
                }
            }
        }
        state.offsetMillis = Math.round(deployerHand(state) * 1000f);
    }

    private static boolean canStartPress(Level level, ItemStack machine, ItemStack input, ItemStack output,
                                         ItemStack backtank) {
        if (!BacktankUtil.hasAirRemaining(backtank)) return false;
        WorkshopRecipeHelper.Result result = WorkshopRecipeHelper.resolve(level, machine, input, ItemStack.EMPTY);
        return result != null && canInsert(output, result.output());
    }

    private static boolean canStartDeployer(Level level, ItemStack machine, ItemStack input,
                                            ItemStack held, ItemStack output, ItemStack backtank) {
        if (!BacktankUtil.hasAirRemaining(backtank)) return false;
        WorkshopRecipeHelper.Result result = WorkshopRecipeHelper.resolve(level, machine, input, held);
        return result != null && canInsert(output, result.output());
    }

    private static void consumeAir(ItemStackHandler handler, int start) {
        ItemStack backtank = handler.getStackInSlot(start + BACKTANK_SLOT);
        if (!WorkshopRecipeHelper.isBacktank(backtank)) return;
        int air = BacktankUtil.getAir(backtank);
        backtank.set(AllDataComponents.BACKTANK_AIR, Math.max(air - AIR_PER_OPERATION, 0));
        handler.setStackInSlot(start + BACKTANK_SLOT, backtank);
    }

    private static float pressHeadOffset(int runningTicks) {
        int rt = Math.abs(runningTicks);
        if (rt < PRESS_CYCLE * 2 / 3) {
            float t = rt / (PRESS_CYCLE / 2f);
            return Mth.clamp(t * t * t, 0f, 1f);
        }
        return Mth.clamp((PRESS_CYCLE - rt) / (PRESS_CYCLE / 3f), 0f, 1f);
    }

    private static float deployerHand(ProcessingState state) {
        if (state.deployerState == DEPLOYER_EXPANDING) {
            return Mth.clamp(1f - state.timer / 1000f, 0f, 1f);
        }
        if (state.deployerState == DEPLOYER_RETRACTING) {
            return Mth.clamp(state.timer / 1000f, 0f, 1f);
        }
        return 0f;
    }

    private static void produce(ItemStackHandler handler, int start, ItemStack input, ItemStack held,
                                ItemStack output, WorkshopRecipeHelper.Result result) {
        input.shrink(1);
        handler.setStackInSlot(start + INPUT_SLOT, input.isEmpty() ? ItemStack.EMPTY : input);

        if (result.damageHeld() && !held.isEmpty()) {
            // Sandpaper: damage by one and break it (remove) once worn out.
            int damage = held.getDamageValue() + 1;
            if (held.isDamageableItem() && damage >= held.getMaxDamage()) {
                handler.setStackInSlot(start + HELD_SLOT, ItemStack.EMPTY);
            } else {
                held.setDamageValue(damage);
                handler.setStackInSlot(start + HELD_SLOT, held);
            }
        } else if (result.consumeHeld() && !held.isEmpty()) {
            held.shrink(1);
            handler.setStackInSlot(start + HELD_SLOT, held.isEmpty() ? ItemStack.EMPTY : held);
        }

        ItemStack produced = result.output();
        if (output.isEmpty()) {
            handler.setStackInSlot(start + OUTPUT_SLOT, produced);
        } else {
            output.grow(produced.getCount());
            handler.setStackInSlot(start + OUTPUT_SLOT, output);
        }
    }

    private static void idle(ProcessingState state) {
        state.processing = false;
        state.offsetMillis = 0;
    }

    private static void playStrikeSound(UpgradeContext context, Level level, ItemStack machine, boolean polishing) {
        BlockPos pos = context.player() != null ? context.player().blockPosition() : context.blockPos();
        if (pos == null) return;

        WorkshopSound sound = WorkshopRecipeHelper.isPress(machine) ? WorkshopSound.PRESS
                : polishing ? WorkshopSound.SANDING
                  : WorkshopSound.DEPLOY;
        emitSound(level, pos, sound);
    }

    private static void emitSound(Level level, BlockPos pos, WorkshopSound sound) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(pos),
                new WorkshopSoundPacket(sound, pos));
    }

    private static void spawnCompletionParticles(UpgradeContext context) {
        if (!(context.level() instanceof ServerLevel server)) return;

        double x;
        double y;
        double z;
        Player player = context.player();
        if (player != null) {
            // Behind the player, towards the top of the pack on their back.
            double back = 0.35;
            double yaw = Math.toRadians(player.yBodyRot);
            x = player.getX() + Math.sin(yaw) * back;
            y = player.getY() + 1.5;
            z = player.getZ() - Math.cos(yaw) * back;
        } else {
            BlockPos pos = context.blockPos();
            if (pos == null) return;
            x = pos.getX() + 0.5;
            y = pos.getY() + 1.0;
            z = pos.getZ() + 0.5;
        }

        server.sendParticles(ParticleTypes.DUST_PLUME, x, y, z, 4, 0.1, 0.2, 0.1, 0.02);
        emitSound(server, BlockPos.containing(x, y, z), WorkshopSound.STEAM);
    }

    private static boolean canInsert(ItemStack output, ItemStack produced) {
        if (produced.isEmpty()) return false;
        if (output.isEmpty()) return produced.getCount() <= produced.getMaxStackSize();
        if (!ItemStack.isSameItemSameComponents(output, produced)) return false;
        return output.getCount() + produced.getCount() <= output.getMaxStackSize();
    }

    @Override
    public void onRemoved(UpgradeContext context) {
        if (context.isClientSide()) return;
        if (!(context.menu() instanceof BackpackMenu menu)) return;

        STATES.remove(menu.container);

        // Stop any spinning flywheels on tracking clients now the upgrade is gone
        if (context.player() instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer,
                    WorkshopProcessingPacket.forEntity(serverPlayer.getId(), false));
        }

        ItemStackHandler handler = menu.container.getItemHandler();
        Player player = context.player();

        boolean changed = false;
        for (int i : menu.layout.workshop().range()) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            ItemStack toReturn = stack.copy();
            handler.setStackInSlot(i, ItemStack.EMPTY);
            changed = true;

            if (player != null) {
                menu.moveStackToPlayerInventory(toReturn);
                if (!toReturn.isEmpty()) {
                    player.drop(toReturn, false);
                }
            }
        }

        if (changed) {
            menu.container.setDataChanged();
        }
    }

    public static class WorkshopPanel implements UpgradePanel {
        private static final int PANEL_WIDTH = 68;
        private static final int PANEL_HEIGHT = 128;
        private static final int TEXTURE_U = 0;
        private static final int TEXTURE_V = 71;

        // Slot positions inside the panel background
        // Order = machine, held, flywheel, input, output.
        private static final int[] SLOT_X = {8, 26, 44, 7, 45};
        private static final int[] SLOT_Y = {24, 24, 24, 105, 105};

        // Anchor for the animated machine
        private static final int DEPLOYER_ANCHOR_X = PANEL_WIDTH / 2 - 13;
        private static final int PRESS_ANCHOR_X = PANEL_WIDTH / 2 - 17;
        private static final int MACHINE_ANCHOR_Y = 48;

        private final BackpackMenu menu;
        private final WorkshopMachineRenderer machineRenderer = new WorkshopMachineRenderer();

        private int leftPos;
        private int imageWidth;
        private int tabY;

        public WorkshopPanel(BackpackMenu menu) {
            this.menu = menu;
        }

        @Override
        public int getExpandedWidth() {
            return PANEL_WIDTH;
        }

        @Override
        public int getExpandedHeight() {
            return PANEL_HEIGHT;
        }

        @Override
        public int getTextureU() {
            return TEXTURE_U;
        }

        @Override
        public int getTextureV() {
            return TEXTURE_V;
        }

        @Override
        public void layoutSlots(List<Slot> slots, int imageWidth, int relativeTabY) {
            int panelLeft = imageWidth - 3;
            int panelTop = relativeTabY - 2;
            for (int i = 0; i < slots.size() && i < SLOT_X.length; i++) {
                Slot slot = slots.get(i);
                slot.x = panelLeft + SLOT_X[i];
                slot.y = panelTop + SLOT_Y[i];
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY) {
            ItemStack machine = machineStack();
            boolean deployer = WorkshopRecipeHelper.isDeployer(machine);
            boolean press = WorkshopRecipeHelper.isPress(machine);
            if (!deployer && !press) return;

            // Sync value: 0 = idle, otherwise progress+1. The renderer interpolates this across server
            // ticks (with render partial-ticks) so the stroke is smooth yet locked to the server cycle
            int sync = menu.getUpgradeSyncValue(UpgradeDataSync.Field.WORKSHOP_PROCESSING);

            int originX = leftPos + imageWidth - 3;
            int originY = tabY - 2;
            int anchorX = originX + (deployer ? DEPLOYER_ANCHOR_X : PRESS_ANCHOR_X);
            int anchorY = originY + MACHINE_ANCHOR_Y;
            machineRenderer.draw(graphics, anchorX, anchorY, deployer, sync);
        }

        private ItemStack machineStack() {
            int index = menu.layout.workshop().getStartIndex() + WorkshopUpgrade.MACHINE_SLOT;
            return menu.container.getItemHandler().getStackInSlot(index);
        }

        // Lang key suffixes for each workshop slot, indexed by the slot's offset within the workshop section
        private static final String[] SLOT_TOOLTIP_KEYS = {
                "machine_slot", "held_slot", "backtank_slot", "input_slot", "output_slot"
        };

        @Override
        public void renderTooltip(Font font, GuiGraphics graphics, int mouseX, int mouseY, Slot hoveredSlot) {
            if (!(hoveredSlot instanceof WorkshopSlot) || hoveredSlot.hasItem()) return;

            int offset = hoveredSlot.getContainerSlot() - menu.layout.workshop().getStartIndex();
            if (offset < 0 || offset >= SLOT_TOOLTIP_KEYS.length) return;

            String base = "tooltip.fxntstorage.backpack_workshop_upgrade.panel." + SLOT_TOOLTIP_KEYS[offset];
            List<Component> text = new ArrayList<>(TooltipHelper.cutTextComponent(Component.translatable(base), FontHelper.Palette.BLUE));
            if (offset == HELD_SLOT && machineStack().getItem().equals(AllBlocks.MECHANICAL_PRESS.asItem())) {
                text.add(Component.translatable("tooltip.fxntstorage.upgrade_inactive").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            }
            text.addAll(TooltipHelper.cutTextComponent(Component.translatable(base + ".description"), FontHelper.Palette.GRAY_AND_WHITE));
            graphics.renderTooltip(
                    font,
                    text,
                    Optional.empty(),
                    mouseX,
                    mouseY
            );
        }

        @Override
        public void createWidgets(Consumer<AbstractWidget> widgetAdder) {
        }

        @Override
        public List<AbstractWidget> getWidgets() {
            return List.of();
        }

        @Override
        public void clearWidgets() {
        }

        @Override
        public void setPanelPosition(int leftPos, int imageWidth, int tabY) {
            this.leftPos = leftPos;
            this.imageWidth = imageWidth;
            this.tabY = tabY;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        @Override
        public void tick() {
        }
    }

    public enum WorkshopSound {
        PRESS,
        SANDING,
        DEPLOY,
        STEAM;

        public void playLocal(Level level, BlockPos pos) {
            switch (this) {
                case PRESS -> {
                    float pitch = 0.75f + ConfigManager.ServerConfig.WORKSHOP_KINETIC_SPEED.get() / 1024f;
                    AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.playAt(level, pos, 0.5f, pitch, false);
                }
                case SANDING -> AllSoundEvents.SANDING_SHORT.playAt(level, pos, 0.25f, 1f, false);
                case DEPLOY -> level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.25f, 0.75f, false);
                case STEAM -> AllSoundEvents.STEAM.playAt(level, pos, 0.15f, 2.75f, false);
            }
        }
    }
}
