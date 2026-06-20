package net.fxnt.fxntstorage.backpack.upgrade.workshop;

import net.createmod.catnip.animation.AnimationTickHolder;

public class FlywheelSpin {

    // Deceleration applied while coasting (degrees per tick, per tick) Lower = more momentum
    private static final float DECELERATION = 0.3f;
    private static final float SPIN_UP_TICKS = 30f;
    // Once coasting slows to this speed, switch to a steady glide onto the nearest SNAP_INCREMENT
    private static final float GLIDE_SPEED = 3.0f;
    // Flywheels rest on multiples of this angle
    private static final float SNAP_INCREMENT = 45f;

    private float angle;
    private float speed;
    private float lastDir = 1f;
    private boolean gliding;
    private float glideTarget;
    private float lastRenderTime = Float.NaN;

    public float advance(float targetSpeed) {
        float now = AnimationTickHolder.getRenderTime();
        float delta = 0f;
        if (!Float.isNaN(lastRenderTime)) {
            float d = now - lastRenderTime;
            // Guard against large jumps (pauses, lag spikes) so the motion stays smooth.
            if (d > 0f && d < 100f) delta = d;
        }
        lastRenderTime = now;
        if (delta == 0f) return angle;

        if (targetSpeed != 0f) {
            // Powered: accelerate toward the target speed
            float dir = Math.signum(targetSpeed);
            lastDir = dir;
            gliding = false;
            // At least the coast-down rate, but fast enough to reach full speed within SPIN_UP_TICKS
            float accel = Math.max(DECELERATION, Math.abs(targetSpeed) / SPIN_UP_TICKS);
            float step = accel * delta;
            if (Math.abs(speed) + step < Math.abs(targetSpeed)) {
                speed += dir * step;
            } else {
                speed = targetSpeed;
            }
            angle = wrap(angle + speed * delta);
            return angle;
        }

        // Unpowered: coast down, then glide onto SNAP_INCREMENT and stop
        if (gliding) {
            advanceGlide(delta);
        } else if (speed != 0f) {
            if (Math.abs(speed) <= GLIDE_SPEED) {
                gliding = true;
                glideTarget = nextIncrement(angle, lastDir);
                advanceGlide(delta);
            } else {
                speed -= Math.signum(speed) * DECELERATION * delta;
                angle = wrap(angle + speed * delta);
            }
        }
        return angle;
    }

    private void advanceGlide(float delta) {
        float step = GLIDE_SPEED * delta;
        float remaining = forwardDistance(angle, glideTarget, lastDir);
        if (remaining <= step) {
            angle = wrap(glideTarget);
            speed = 0f;
            gliding = false;
        } else {
            angle = wrap(angle + lastDir * step);
        }
    }

    private static float wrap(float a) {
        a %= 360f;
        return a < 0f ? a + 360f : a;
    }

    private static float nextIncrement(float angle, float dir) {
        if (dir >= 0f) {
            return (float) (Math.floor(angle / SNAP_INCREMENT) + 1) * SNAP_INCREMENT;
        }
        return (float) (Math.ceil(angle / SNAP_INCREMENT) - 1) * SNAP_INCREMENT;
    }

    private static float forwardDistance(float from, float to, float dir) {
        float d = ((to - from) * Math.signum(dir)) % 360f;
        return d < 0f ? d + 360f : d;
    }
}
