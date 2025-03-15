package net.fxnt.fxntstorage.init;

import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

public class ModTabs {
	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FXNTStorage.MOD_ID);

	public static final RegistryObject<CreativeModeTab> CREATE_STORAGE = TABS.register("fxntstorage",
			() -> CreativeModeTab.builder().title(Component.translatable("itemGroup.fxntstorage.main")).icon(() -> new ItemStack(ModBlocks.SIMPLE_STORAGE_BOX.get()))
					.displayItems((pParameters, tabData) -> {

				tabData.accept(ModItems.STORAGE_BOX.get());
				tabData.accept(ModItems.ANDESITE_STORAGE_BOX.get());
				tabData.accept(ModItems.COPPER_STORAGE_BOX.get());
				tabData.accept(ModItems.BRASS_STORAGE_BOX.get());
				tabData.accept(ModItems.HARDENED_STORAGE_BOX.get());

				tabData.accept(ModBlocks.STORAGE_CONTROLLER.get().asItem());
				tabData.accept(ModBlocks.STORAGE_INTERFACE.get().asItem());

				tabData.accept(ModItems.SIMPLE_STORAGE_BOX.get());
				tabData.accept(ModItems.SIMPLE_STORAGE_BOX_SPRUCE.get());
				tabData.accept(ModItems.SIMPLE_STORAGE_BOX_BIRCH.get());
				tabData.accept(ModItems.SIMPLE_STORAGE_BOX_JUNGLE.get());
				tabData.accept(ModItems.SIMPLE_STORAGE_BOX_ACACIA.get());
				tabData.accept(ModItems.SIMPLE_STORAGE_BOX_DARK_OAK.get());
				tabData.accept(ModItems.SIMPLE_STORAGE_BOX_MANGROVE.get());
				tabData.accept(ModItems.SIMPLE_STORAGE_BOX_CHERRY.get());
				tabData.accept(ModItems.SIMPLE_STORAGE_BOX_BAMBOO.get());
				tabData.accept(ModItems.SIMPLE_STORAGE_BOX_CRIMSON.get());
				tabData.accept(ModItems.SIMPLE_STORAGE_BOX_WARPED.get());

				tabData.accept(ModItems.STORAGE_BOX_VOID_UPGRADE.get());
				tabData.accept(ModItems.STORAGE_BOX_CAPACITY_UPGRADE.get());

				tabData.accept(ModItems.BACK_PACK.get());
				tabData.accept(ModItems.ANDESITE_BACK_PACK.get());
				tabData.accept(ModItems.COPPER_BACK_PACK.get());
				tabData.accept(ModItems.BRASS_BACK_PACK.get());
				tabData.accept(ModItems.HARDENED_BACK_PACK.get());

				tabData.accept(ModBlocks.PASSER_BLOCK.get().asItem());
				tabData.accept(ModBlocks.SMART_PASSER_BLOCK.get().asItem());

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

	public static void register(IEventBus eventBus) {
		TABS.register(eventBus);
	}
}
