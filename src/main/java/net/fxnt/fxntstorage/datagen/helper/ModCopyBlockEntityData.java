package net.fxnt.fxntstorage.datagen.helper;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.logistics.filter.FilterItem;
import net.fxnt.fxntstorage.container.StorageBoxEntity;
import net.fxnt.fxntstorage.init.ModLootFunctionTypes;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModCopyBlockEntityData extends LootItemConditionalFunction {

    public static final MapCodec<ModCopyBlockEntityData> CODEC = MapCodec.unit(new ModCopyBlockEntityData(List.of()));

    ModCopyBlockEntityData(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType<ModCopyBlockEntityData> getType() {
        return ModLootFunctionTypes.COPY_BLOCK_ENTITY_DATA.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.BLOCK_ENTITY);
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        BlockEntity blockEntity = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        if (blockEntity != null) {
            blockEntity.saveToItem(stack, context.getLevel().registryAccess());
            // Strip FilterItem from BLOCK_ENTITY_DATA to avoid duplication
            if (blockEntity instanceof StorageBoxEntity sbe) {
                ItemStack filterItem = sbe.getFilter().getFilter();
                if (filterItem.getItem() instanceof FilterItem) {
                    CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
                    if (data != null) {
                        CompoundTag tag = data.copyTag();
                        tag.remove("Filter");
                        tag.remove("FilterAmount");
                        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
                    }
                }
            }
        }
        return stack;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends LootItemConditionalFunction.Builder<Builder> {
        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new ModCopyBlockEntityData(getConditions());
        }
    }
}
