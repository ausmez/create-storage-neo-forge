package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModTabs {
    private static final DeferredRegister<CreativeModeTab> REGISTER;
    public static final RegistryObject<CreativeModeTab> CREATIVE_MODE_TAB;

    static {
        REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FXNTStorage.MOD_ID);
        CREATIVE_MODE_TAB = REGISTER.register("fxntstorage", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.fxntstorage.main"))
                .icon(ModBlocks.SIMPLE_STORAGE_BOX::asStack)
                .displayItems((pParameters, tabData) -> {

                    tabData.accept(ModBlocks.STORAGE_BOX);
                    tabData.accept(ModBlocks.ANDESITE_STORAGE_BOX);
                    tabData.accept(ModBlocks.COPPER_STORAGE_BOX);
                    tabData.accept(ModBlocks.BRASS_STORAGE_BOX);
                    tabData.accept(ModBlocks.HARDENED_STORAGE_BOX);

                    tabData.accept(ModBlocks.STORAGE_CONTROLLER);
                    tabData.accept(ModBlocks.STORAGE_INTERFACE);

                    tabData.accept(ModBlocks.SIMPLE_STORAGE_BOX);
                    tabData.accept(ModBlocks.SIMPLE_STORAGE_BOX_SPRUCE);
                    tabData.accept(ModBlocks.SIMPLE_STORAGE_BOX_BIRCH);
                    tabData.accept(ModBlocks.SIMPLE_STORAGE_BOX_JUNGLE);
                    tabData.accept(ModBlocks.SIMPLE_STORAGE_BOX_ACACIA);
                    tabData.accept(ModBlocks.SIMPLE_STORAGE_BOX_DARK_OAK);
                    tabData.accept(ModBlocks.SIMPLE_STORAGE_BOX_MANGROVE);
                    tabData.accept(ModBlocks.SIMPLE_STORAGE_BOX_CHERRY);
                    tabData.accept(ModBlocks.SIMPLE_STORAGE_BOX_BAMBOO);
                    tabData.accept(ModBlocks.SIMPLE_STORAGE_BOX_CRIMSON);
                    tabData.accept(ModBlocks.SIMPLE_STORAGE_BOX_WARPED);

                    tabData.accept(ModItems.STORAGE_BOX_VOID_UPGRADE.get());
                    tabData.accept(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get());

                    tabData.accept(ModBlocks.BACK_PACK);
                    tabData.accept(ModBlocks.ANDESITE_BACK_PACK);
                    tabData.accept(ModBlocks.COPPER_BACK_PACK);
                    tabData.accept(ModBlocks.BRASS_BACK_PACK);
                    tabData.accept(ModBlocks.HARDENED_BACK_PACK);

                    tabData.accept(ModBlocks.PASSER_BLOCK);
                    tabData.accept(ModBlocks.SMART_PASSER_BLOCK);

                    tabData.accept(ModItems.BACK_PACK_BLANK_UPGRADE.get());
                    tabData.accept(ModItems.BACK_PACK_MAGNET_UPGRADE.get());
                    tabData.accept(ModItems.BACK_PACK_PICKBLOCK_UPGRADE.get());
                    tabData.accept(ModItems.BACK_PACK_ITEMPICKUP_UPGRADE.get());
                    tabData.accept(ModItems.BACK_PACK_FLIGHT_UPGRADE.get());
                    tabData.accept(ModItems.BACK_PACK_REFILL_UPGRADE.get());
                    tabData.accept(ModItems.BACK_PACK_FEEDER_UPGRADE.get());
                    tabData.accept(ModItems.BACK_PACK_TOOLSWAP_UPGRADE.get());
                    tabData.accept(ModItems.BACK_PACK_FALLDAMAGE_UPGRADE.get());

                    tabData.accept(ModBlocks.STORAGE_TRIM);
                    tabData.accept(ModBlocks.STORAGE_TRIM_SPRUCE);
                    tabData.accept(ModBlocks.STORAGE_TRIM_BIRCH);
                    tabData.accept(ModBlocks.STORAGE_TRIM_JUNGLE);
                    tabData.accept(ModBlocks.STORAGE_TRIM_ACACIA);
                    tabData.accept(ModBlocks.STORAGE_TRIM_DARK_OAK);
                    tabData.accept(ModBlocks.STORAGE_TRIM_MANGROVE);
                    tabData.accept(ModBlocks.STORAGE_TRIM_CHERRY);
                    tabData.accept(ModBlocks.STORAGE_TRIM_BAMBOO);
                    tabData.accept(ModBlocks.STORAGE_TRIM_CRIMSON);
                    tabData.accept(ModBlocks.STORAGE_TRIM_WARPED);
                }).build());
    }

    public static void register(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }
}
