package net.fxnt.fxntstorage.backpack.upgrade;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fxnt.fxntstorage.init.ModTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.conditions.ICondition;

public record UpgradeEnabledCondition(Item upgrade) implements ICondition {
    public static final MapCodec<UpgradeEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(builder -> builder
            .group(BuiltInRegistries.ITEM.byNameCodec().fieldOf("upgrade").forGetter(UpgradeEnabledCondition::upgrade))
            .apply(builder, UpgradeEnabledCondition::new));

    @Override
    public boolean test(IContext context) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(upgrade);
        return context.getTag(ModTags.Items.DISABLED_BACKPACK_UPGRADES).stream().noneMatch(holder -> holder.is(id));
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
