package net.fxnt.fxntstorage.compat.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.forge.REIPluginCommon;
import net.fxnt.fxntstorage.init.ModTags;

@SuppressWarnings("unused")
@REIPluginCommon
public class REICompat implements REIClientPlugin {

    @Override
    public void registerEntries(EntryRegistry registry) {
        registry.removeEntryIf(entryStack -> entryStack.getTagsFor().toList().contains(ModTags.Items.BACKPACK_UPGRADE_DEACTIVATED));
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register(new REIStonecuttingTransferHandler());
    }
}
