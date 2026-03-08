package net.fxnt.fxntstorage.ponder;

import com.simibubi.create.AllSpecialTextures;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;

public class CsOutlineInstruction extends TickingInstruction {

    private final PonderPalette color;
    private final Object slot;
    private final Selection selection;

    public CsOutlineInstruction(PonderPalette color, Object slot, Selection selection, int ticks) {
        super(false, ticks);
        this.color = color;
        this.slot = slot;
        this.selection = selection;
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);

        selection.makeOutline(scene.getOutliner(), slot)
                .lineWidth(1 / 16f)
                .withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.HIGHLIGHT_CHECKERED)
                .colored(color.getColor());
    }
}
