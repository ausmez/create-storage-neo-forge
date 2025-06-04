package net.fxnt.fxntstorage.registry;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.resources.ResourceLocation;

public class SpriteShifts {
    public static final CTSpriteShiftEntry
            OAK_CASING = ct("oak_casing"),
            SPRUCE_CASING = ct("spruce_casing"),
            BIRCH_CASING = ct("birch_casing"),
            JUNGLE_CASING = ct("jungle_casing"),
            ACACIA_CASING = ct("acacia_casing"),
            DARK_OAK_CASING = ct("dark_oak_casing"),
            MANGROVE_CASING = ct("mangrove_casing"),
            CHERRY_CASING = ct("cherry_casing"),
            BAMBOO_CASING = ct("bamboo_casing"),
            CRIMSON_CASING = ct("crimson_casing"),
            WARPED_CASING = ct("warped_casing"),
            PALE_OAK_CASING = ct("pale_oak_casing");

    private static CTSpriteShiftEntry ct(String name) {
        return CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
                ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "block/casings/" + name),
                ResourceLocation.fromNamespaceAndPath(FXNTStorage.MOD_ID, "block/casings/" + name + "_connected"));
    }

}
