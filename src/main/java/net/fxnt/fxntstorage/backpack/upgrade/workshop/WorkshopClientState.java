package net.fxnt.fxntstorage.backpack.upgrade.workshop;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class WorkshopClientState {
    private static final Map<Integer, Boolean> PROCESSING = new ConcurrentHashMap<>();
    private static final Map<Integer, FlywheelSpin> ANIMATION = new ConcurrentHashMap<>();

    private WorkshopClientState() {
    }

    public static void setProcessing(int entityId, boolean processing) {
        PROCESSING.put(entityId, processing);
    }

    public static boolean isProcessing(int entityId) {
        return PROCESSING.getOrDefault(entityId, false);
    }

    public static float advanceAngle(int entityId, float targetSpeed) {
        return ANIMATION.computeIfAbsent(entityId, k -> new FlywheelSpin()).advance(targetSpeed);
    }
}
