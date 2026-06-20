package net.fxnt.fxntstorage.util;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public final class ContraptionInteractionContext {

    public static final ThreadLocal<@Nullable Direction> INTERACTION_DIRECTION = ThreadLocal.withInitial(() -> null);

    private ContraptionInteractionContext() {}
}