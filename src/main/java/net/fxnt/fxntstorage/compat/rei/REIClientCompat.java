package net.fxnt.fxntstorage.compat.rei;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackScreen;
import net.fxnt.fxntstorage.backpack.upgrade.UpgradeType;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
@REIPluginClient
public class REIClientCompat implements REIClientPlugin {

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(BackpackScreen.class, screen ->
                screen.getExclusionZones()
                        .stream()
                        .map(r -> new Rectangle(r.getX(), r.getY(), r.getWidth(), r.getHeight()))
                        .toList()
        );
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        registry.removeEntryIf(entry -> entry.getValue() instanceof ItemStack stack && UpgradeType.isDisabled(stack));
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerDraggableStackVisitor(new REIDraggableStackVisitorHandler());
        registry.registerDraggableStackVisitor(new REIReserveStorageVisitorHandler());
    }
}
