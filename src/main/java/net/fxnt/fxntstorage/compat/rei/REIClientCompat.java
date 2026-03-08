package net.fxnt.fxntstorage.compat.rei;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import net.fxnt.fxntstorage.backpack.client.menu.BackpackScreen;

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
    public void registerScreens(ScreenRegistry registry) {
        registry.registerDraggableStackVisitor(new REIDraggableStackVisitorHandler());
    }
}
