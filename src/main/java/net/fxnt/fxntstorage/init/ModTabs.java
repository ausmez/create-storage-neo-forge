package net.fxnt.fxntstorage.init;

import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.fxnt.fxntstorage.FXNTStorage.REGISTRATE;

public class ModTabs {
    private static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FXNTStorage.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_MODE_TAB =
            REGISTER.register("creative_mode_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fxntstorage.main"))
                    .icon(ModBlocks.SIMPLE_STORAGE_BOX::asStack)
                    .withTabsBefore(AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getId())
                    .displayItems(new DisplayItemsGenerator())
                    .build());

    public static void register(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }

    // Shamelessly adapted from AllCreativeModeTabs
    private static class DisplayItemsGenerator implements CreativeModeTab.DisplayItemsGenerator {
        private static final Set<Item> EXCLUDED_ITEMS = Set.of(
                ModItems.BACKPACK_MAGNET_UPGRADE_DEACTIVATED.asItem(),
                ModItems.BACKPACK_PICKBLOCK_UPGRADE_DEACTIVATED.asItem(),
                ModItems.BACKPACK_ITEMPICKUP_UPGRADE_DEACTIVATED.asItem(),
                ModItems.BACKPACK_FLIGHT_UPGRADE_DEACTIVATED.asItem(),
                ModItems.BACKPACK_REFILL_UPGRADE_DEACTIVATED.asItem(),
                ModItems.BACKPACK_FEEDER_UPGRADE_DEACTIVATED.asItem(),
                ModItems.BACKPACK_TOOLSWAP_UPGRADE_DEACTIVATED.asItem(),
                ModItems.BACKPACK_FALLDAMAGE_UPGRADE_DEACTIVATED.asItem(),
                ModItems.BACKPACK_OREMINING_UPGRADE_DEACTIVATED.asItem(),
                ModItems.BACKPACK_TORCHDEPLOYER_UPGRADE_DEACTIVATED.asItem()
        );

        private static Predicate<Item> exclusionPredicate() {
            return EXCLUDED_ITEMS::contains;
        }

        private static List<ItemOrdering> makeOrdering() {
            List<ItemOrdering> orderings = new ReferenceArrayList<>();

            Map<ItemProviderEntry<?, ?>, ItemProviderEntry<?, ?>> simpleBeforeOrderings = Map.of(
                    ModItems.STORAGE_BOX_VOID_UPGRADE, ModBlocks.BACKPACK,
                    ModItems.STORAGE_BOX_CAPACITY_UPGRADE, ModBlocks.BACKPACK,
                    ModItems.BACKPACK_BLANK_UPGRADE, ModBlocks.STORAGE_TRIM
            );

            List<Item> upgradeItems = REGISTRATE.getAll(Registries.ITEM).stream()
                    .filter(entry -> entry.is(ModTags.Items.BACKPACK_UPGRADE))
                    .filter(entry -> CreateRegistrate.isInCreativeTab(entry, CREATIVE_MODE_TAB))
                    .map(entry -> entry.get().asItem())
                    .filter(item -> !exclusionPredicate().test(item))
                    .distinct().toList();

            simpleBeforeOrderings.forEach((entry, otherEntry) ->
                    orderings.add(ItemOrdering.before(entry.asItem(), otherEntry.asItem())));

            // Match ordering of upgrade items from previous versions
            Item prevItem = ModItems.BACKPACK_BLANK_UPGRADE.asItem();
            for (Item item : upgradeItems) {
                orderings.add(ItemOrdering.after(item, prevItem));
                prevItem = item;
            }

            return orderings;
        }

        private static final Map<Item, Function<Item, ItemStack>> ITEM_FACTORIES = Map.of();

        private static Function<Item, ItemStack> stackFunc() {
            return item -> ITEM_FACTORIES.getOrDefault(item, ItemStack::new).apply(item);
        }

        private static final Map<Item, CreativeModeTab.TabVisibility> ITEM_VISIBILITIES = Map.of();

        private static Function<Item, CreativeModeTab.TabVisibility> visibilityFunc() {
            return item -> ITEM_VISIBILITIES.getOrDefault(item, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        @Override
        public void accept(CreativeModeTab.@NotNull ItemDisplayParameters parameters, CreativeModeTab.@NotNull Output output) {
            List<ItemOrdering> orderings = makeOrdering();
            List<Item> items = new ArrayList<>(collectBlocks());
            items.addAll(collectItems());

            applyOrderings(items, orderings);
            outputAll(output, items);
        }

        private List<Item> collectBlocks() {
            return REGISTRATE.getAll(Registries.BLOCK).stream()
                    .filter(entry -> CreateRegistrate.isInCreativeTab(entry, CREATIVE_MODE_TAB))
                    .map(entry -> entry.get().asItem())
                    .filter(item -> item != Items.AIR && !exclusionPredicate().test(item))
                    .distinct()
                    .toList();
        }

        private List<Item> collectItems() {
            return REGISTRATE.getAll(Registries.ITEM).stream()
                    .filter(entry -> CreateRegistrate.isInCreativeTab(entry, CREATIVE_MODE_TAB))
                    .map(RegistryEntry::get)
                    .filter(item -> !(item instanceof BlockItem) && !exclusionPredicate().test(item))
                    .toList();
        }

        private static void applyOrderings(List<Item> items, List<ItemOrdering> orderings) {
            for (ItemOrdering ordering : orderings) {
                int anchorIndex = items.indexOf(ordering.anchor());
                if (anchorIndex != -1) {
                    Item item = ordering.item();
                    int itemIndex = items.indexOf(item);
                    if (itemIndex != -1) {
                        items.remove(itemIndex);
                        if (itemIndex < anchorIndex) {
                            anchorIndex--;
                        }
                    }
                    if (ordering.type() == ItemOrdering.Type.AFTER) {
                        items.add(anchorIndex + 1, item);
                    } else {
                        items.add(anchorIndex, item);
                    }
                }
            }
        }

        private static void outputAll(CreativeModeTab.Output output, List<Item> items) {
            items.forEach(item -> output.accept(stackFunc().apply(item), visibilityFunc().apply(item)));
        }

        private record ItemOrdering(Item item, Item anchor, Type type) {
            public static ItemOrdering before(Item item, Item anchor) {
                return new ItemOrdering(item, anchor, Type.BEFORE);
            }

            public static ItemOrdering after(Item item, Item anchor) {
                return new ItemOrdering(item, anchor, Type.AFTER);
            }

            public enum Type {
                BEFORE,
                AFTER
            }
        }
    }

}
