package net.fxnt.fxntstorage.datagen.helper;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
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
public class ModBlockEntitySaveTag implements LootItemCondition {

    public static final ModBlockEntitySaveTag INSTANCE = new ModBlockEntitySaveTag();

    private ModBlockEntitySaveTag() {
    }

    @Override
    public LootItemConditionType getType() {
        return ModLootConditionTypes.BLOCK_ENTITY_SAVE_TAG.get();
    }

    @Override
    public boolean test(LootContext lootContext) {
        BlockEntity blockEntity = lootContext.getParamOrNull(LootContextParams.BLOCK_ENTITY);

        if (blockEntity == null) {
            return false;
        }

        // Check if the block entity has a custom name
        CompoundTag tag = blockEntity.saveWithoutMetadata();
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

    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<ModBlockEntitySaveTag> {

        @Override
        public void serialize(JsonObject pJson, ModBlockEntitySaveTag pValue, JsonSerializationContext pSerializationContext) {
            pJson.addProperty("condition", "fxntstorage:block_entity_save_tag");
        }

        @Override
        public ModBlockEntitySaveTag deserialize(JsonObject pJson, JsonDeserializationContext pSerializationContext) {
            return INSTANCE;
        }
    }
}
