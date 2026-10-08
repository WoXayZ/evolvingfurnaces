package com.evolvingfurnaces.client.render;

import java.util.function.Function;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

/**
 * Cutout block pipeline: vertex shade times the lightmap, the same lighting as the furnace model
 * in the world and on the item. Entity face light was darkening the front overlay at night.
 */
public final class EFOverlayRenderType {

    private static final Function<Identifier, RenderType> FLUSH = Util.memoize(texture -> create(texture));
    private static final Function<Identifier, RenderType> OFFSET = Util.memoize(
            texture -> create(texture, LayeringTransform.VIEW_OFFSET_Z_LAYERING));

    private EFOverlayRenderType() {
    }

    public static RenderType flush(Identifier texture) {
        return FLUSH.apply(texture);
    }

    public static RenderType offset(Identifier texture) {
        return OFFSET.apply(texture);
    }

    private static RenderType create(Identifier texture) {
        return create(texture, LayeringTransform.NO_LAYERING);
    }

    private static RenderType create(Identifier texture, LayeringTransform layering) {
        return RenderType.create(
                "evolvingfurnaces_overlay",
                RenderSetup.builder(RenderPipelines.CUTOUT_BLOCK)
                        .withTexture("Sampler0", texture)
                        .useLightmap()
                        .setLayeringTransform(layering)
                        .createRenderSetup());
    }
}
