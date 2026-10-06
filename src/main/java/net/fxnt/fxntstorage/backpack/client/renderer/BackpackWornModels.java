package net.fxnt.fxntstorage.backpack.client.renderer;

import net.fxnt.fxntstorage.backpack.BackpackItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class BackpackWornModels {

    private static final Map<Item, ModelResourceLocation> LOCATIONS = new ConcurrentHashMap<>();

    private BackpackWornModels() {}

    public static ModelResourceLocation locationFor(Item item) {
        return LOCATIONS.computeIfAbsent(item, i -> ModelResourceLocation.standalone(
                BuiltInRegistries.ITEM.getKey(i).withPath(path -> "block/" + path + "_worn")));
    }

    public static void register(ModelEvent.RegisterAdditional event) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof BackpackItem) event.register(locationFor(item));
        }
    }

    public static BakedModel get(ItemStack backpack) {
        Minecraft mc = Minecraft.getInstance();
        ModelManager models = mc.getModelManager();
        BakedModel model = models.getModel(locationFor(backpack.getItem()));
        // Fall back to the placed block model if a worn model is missing
        if (model == models.getMissingModel() && backpack.getItem() instanceof BlockItem blockItem) {
            return mc.getBlockRenderer().getBlockModel(blockItem.getBlock().defaultBlockState());
        }
        return model;
    }
}
