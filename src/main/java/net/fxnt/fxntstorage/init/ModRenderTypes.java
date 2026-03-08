package net.fxnt.fxntstorage.init;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fxnt.fxntstorage.FXNTStorage;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

public class ModRenderTypes extends RenderType {
    public ModRenderTypes(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
        super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
    }

    public static final RenderType ORE_LINES_SOLID = RenderType.create(
            FXNTStorage.MOD_ID + ":ore_lines_solid",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            256, false, false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getRendertypeLinesShader))
                    .setLineState(new LineStateShard(OptionalDouble.of(6.0)))
                    .setLayeringState(NO_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
    );

    public static final RenderType ORE_LINES_XRAY = RenderType.create(
            FXNTStorage.MOD_ID + ":ore_lines_xray",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            256, false, false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getRendertypeLinesShader))
                    .setLineState(new LineStateShard(OptionalDouble.of(6.0)))
                    .setLayeringState(NO_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .createCompositeState(false)
    );
}
