package net.fxnt.fxntstorage.backpack.upgrade.workshop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public record WorkshopFlywheelPlacement(Vec3 position, float size, Direction.Axis axis, boolean reverse) {
    public static final float DEFAULT_SIZE = 2.4f;

    public static final Codec<WorkshopFlywheelPlacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Vec3.CODEC.fieldOf("position").forGetter(WorkshopFlywheelPlacement::position),
            Codec.floatRange(0f, 64f).optionalFieldOf("size", DEFAULT_SIZE).forGetter(WorkshopFlywheelPlacement::size),
            Direction.Axis.CODEC.optionalFieldOf("axis", Direction.Axis.X).forGetter(WorkshopFlywheelPlacement::axis),
            Codec.BOOL.optionalFieldOf("reverse", false).forGetter(WorkshopFlywheelPlacement::reverse)
    ).apply(instance, WorkshopFlywheelPlacement::new));
}
