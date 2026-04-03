package com.radiance.client.util;

import java.util.Map;
import java.util.Optional;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;

/**
 * Helper for extracting texture information from RenderLayer in 1.21.11+.
 * In 1.21.11, RenderLayer.MultiPhase and RenderPhase were removed and replaced
 * by the RenderSetup system. This helper provides a centralized way to extract
 * texture identifiers from the new API.
 */
public class RenderLayerHelper {

    /**
     * Extracts the primary texture Identifier from a RenderLayer.
     * In 1.21.11, textures are stored in RenderSetup.textures as a Map.
     *
     * @param renderLayer the render layer to extract the texture from
     * @return Optional containing the texture Identifier, or empty if none found
     */
    public static Optional<Identifier> getTextureId(RenderLayer renderLayer) {
        RenderSetup renderSetup = renderLayer.renderSetup;
        if (renderSetup == null) {
            return Optional.empty();
        }
        Map<String, ?> resolved = renderSetup.resolveTextures();
        if (resolved == null || resolved.isEmpty()) {
            return Optional.empty();
        }
        // Get the first texture entry (typically "Sampler0")
        Object firstValue = resolved.values().iterator().next();
        if (firstValue instanceof Identifier id) {
            return Optional.of(id);
        }
        return Optional.empty();
    }

    /**
     * Sets up the glint texture matrix.
     * Replaces the removed RenderPhase.setupGlintTexturing() method.
     *
     * @param scale the texture scale factor
     */
    public static void setupGlintTexturing(float scale) {
        long time = net.minecraft.util.Util.getMeasuringTimeMs()
            * net.minecraft.client.MinecraftClient.getInstance().options.getGlintSpeed().getValue().longValue();
        float f = (float) (time % 110000L) / 110000.0F;
        float g = (float) (time % 30000L) / 30000.0F;
        org.joml.Matrix4f matrix4f = new org.joml.Matrix4f()
            .translation(-f, g, 0.0F);
        matrix4f.rotateZ(0.17453292F).scale(scale, scale, scale);
        com.mojang.blaze3d.systems.RenderSystem.setTextureMatrix(matrix4f);
    }
}
