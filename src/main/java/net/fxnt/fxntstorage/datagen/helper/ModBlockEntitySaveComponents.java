package net.fxnt.fxntstorage.datagen.helper;

import com.mojang.serialization.MapCodec;
import net.fxnt.fxntstorage.init.ModLootConditionTypes;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModBlockEntitySaveComponents implements LootItemCondition {

    public static final ModBlockEntitySaveComponents INSTANCE = new ModBlockEntitySaveComponents();
    public static final MapCodec<ModBlockEntitySaveComponents> CODEC = MapCodec.unit(INSTANCE);

    private ModBlockEntitySaveComponents() {
    }

    @Override
    public LootItemConditionType getType() {
        return ModLootConditionTypes.BLOCK_ENTITY_SAVE_COMPONENTS.get();
    }

    @Override
    public boolean test(LootContext lootContext) {
        BlockEntity blockEntity = lootContext.getParamOrNull(LootContextParams.BLOCK_ENTITY);

        if (blockEntity == null) {
            return false;
        }

        // Check if the block entity has a custom name
        CompoundTag tag = blockEntity.saveWithoutMetadata(lootContext.getLevel().registryAccess());
        if (tag.contains("CustomName")) {
            return true;
        }

        // Check if the block entity has items
        if (tag.contains("Items", Tag.TAG_COMPOUND)) {
            CompoundTag itemsCompound = tag.getCompound("Items");
            if (itemsCompound.contains("Items", Tag.TAG_LIST)) {
                ListTag itemList = itemsCompound.getList("Items", ListTag.TAG_COMPOUND);
                return !itemList.isEmpty();
            }
        }
        return false;
    }

    public static Builder builder() {
        return () -> INSTANCE;
    }
}
