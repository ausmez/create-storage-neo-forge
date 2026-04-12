package net.fxnt.fxntstorage.compat.everycomp;

import com.simibubi.create.AllTags;
import com.simibubi.create.CreateClient;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.foundation.block.connected.*;
import com.simibubi.create.impl.contraption.storage.FallbackMountedStorageType;
import net.fxnt.fxntstorage.FXNTStorage;
import net.fxnt.fxntstorage.init.ModBlockEntities;
import net.fxnt.fxntstorage.init.ModCompats;
import net.fxnt.fxntstorage.init.ModMountedStorageTypes;
import net.fxnt.fxntstorage.init.ModTags;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBox;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxItem;
import net.fxnt.fxntstorage.simple_storage.mounted.SimpleStorageBoxMovementBehaviour;
import net.mehvahdjukaar.every_compat.api.PaletteStrategies;
import net.mehvahdjukaar.every_compat.api.SimpleEntrySet;
import net.mehvahdjukaar.every_compat.modules.EveryCompatModule;
import net.mehvahdjukaar.moonlight.api.resources.pack.ResourceGenTask;
import net.mehvahdjukaar.moonlight.api.set.wood.VanillaWoodTypes;
import net.mehvahdjukaar.moonlight.api.set.wood.WoodType;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WoodGoodModule extends EveryCompatModule {

    public final SimpleEntrySet<WoodType, Block> simple_storage_boxes;
    public final SimpleEntrySet<WoodType, Block> storage_trims;

    public WoodGoodModule(String modId) {
        super(modId, "cs", FXNTStorage.MOD_ID);

        // Using spruce because most textures re-color better than oak
        simple_storage_boxes = SimpleEntrySet.builder(WoodType.class, "simple_storage_box",
                        getModBlock("spruce_simple_storage_box"), () -> VanillaWoodTypes.SPRUCE,
                        w -> new SimpleStorageBox(Utils.copyPropertySafe(w.planks))
                )
                .addTextureM(modRes("block/casings/spruce_casing"), modRes("block/casings/spruce_casing_m"), PaletteStrategies.PLANKS_STANDARD)
                .addTile(ModBlockEntities.SIMPLE_STORAGE_BOX_ENTITY)
                .addTag(ModTags.Blocks.SIMPLE_STORAGE_BOX, Registries.BLOCK)
                .addTag(AllTags.AllBlockTags.WRENCH_PICKUP.tag)
                .addRecipe(modRes("crafting_shaped/simple_storage_box/spruce_simple_storage_box"))
                .addCustomItem(((woodType, block, properties) -> new SimpleStorageBoxItem(block, properties)))
                .copyParentDrop()
                .setTab(FXNTStorage.REGISTRATE.getCreativeTab())
                .build();
        this.addEntry(simple_storage_boxes);

        storage_trims = SimpleEntrySet.builder(WoodType.class, "storage_trim",
                        getModBlock("spruce_storage_trim"), () -> VanillaWoodTypes.SPRUCE,
                        w -> new CasingBlock(Utils.copyPropertySafe(w.planks))
                )
                .addTextureM(modRes("block/casings/spruce_casing_connected"), modRes("block/casings/spruce_casing_connected_m"), PaletteStrategies.PLANKS_STANDARD)
                .addTag(ModTags.Blocks.STORAGE_TRIM, Registries.BLOCK)
                .addTag(BlockTags.MINEABLE_WITH_AXE, Registries.BLOCK)
                .addTag(AllTags.AllBlockTags.CASING.tag, Registries.BLOCK)
                .addTag(AllTags.AllItemTags.CASING.tag, Registries.ITEM)
                .addRecipe(modRes("crafting_shaped/storage_trim/spruce_storage_trim"))
                .copyParentDrop()
                .setTab(FXNTStorage.REGISTRATE.getCreativeTab())
                .build();
        this.addEntry(storage_trims);
    }

    @Override
    public void addDynamicClientResources(Consumer<ResourceGenTask> executor) {
        super.addDynamicClientResources(executor);

        storage_trims.blocks.forEach(((woodType, block) -> {
            Supplier<CTSpriteShiftEntry> shift = () -> CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
                    ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "block/cs/" + woodType.getNamespace() + "/casings/" + woodType.getTypeName() + "_casing"),
                    ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "block/cs/" + woodType.getNamespace() + "/casings/" + woodType.getTypeName() + "_casing_connected"));
            registerCTModel(block, shift);
        }));
    }

    @Override
    public void onModSetup() {
        super.onModSetup();

        simple_storage_boxes.blocks.forEach((woodType, block) -> {
            if (MountedItemStorageType.REGISTRY.get(block) instanceof FallbackMountedStorageType)
                MountedItemStorageType.REGISTRY.register(block, ModMountedStorageTypes.SIMPLE_STORAGE_BOX_MOUNTED.get());
            if (MovementBehaviour.REGISTRY.get(block) == null)
                MovementBehaviour.REGISTRY.register(block, new SimpleStorageBoxMovementBehaviour());
        });
    }

    @Override
    public List<String> getAlreadySupportedMods() {
        return List.of(ModCompats.VANILLA_BACKPORT);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerCTModel(Block block, Supplier<CTSpriteShiftEntry> spriteShift) {
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(
                Utils.getID(block),
                model -> new CTModel(model, new SimpleCTBehaviour(spriteShift.get()))
        );
        CreateClient.CASING_CONNECTIVITY.makeCasing(block, spriteShift.get());
        FXNTStorage.LOGGER.debug("Registered CTModel for {}", Utils.getID(block));
    }
}
