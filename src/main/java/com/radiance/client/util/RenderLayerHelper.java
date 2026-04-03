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
     * In 1.21.11, textures are stored in RenderSetup as TextureSpec entries.
     * We iterate through the textures map and try to find an Identifier.
     *
     * @param renderLayer the render layer to extract the texture from
     * @return Optional containing the texture Identifier, or empty if none found
     */
    public static Optional<Identifier> getTextureId(RenderLayer renderLayer) {
        try {
            RenderSetup renderSetup = renderLayer.renderSetup;
            if (renderSetup == null) {
                return Optional.empty();
            }
            // textures is Map<String, TextureSpec> where TextureSpec contains an Identifier
            Map<String, ?> textures = renderSetup.textures;
            if (textures == null || textures.isEmpty()) {
                return Optional.empty();
            }
            // Iterate the textures to find any TextureSpec that has an id field
            for (Object value : textures.values()) {
                if (value instanceof RenderSetup.TextureSpec textureSpec) {
                    // TextureSpec is a record-like class that wraps an Identifier
                    // Try to get the id from it via reflection or direct field access
                    return extractIdentifierFromTextureSpec(textureSpec);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static Optional<Identifier> extractIdentifierFromTextureSpec(RenderSetup.TextureSpec textureSpec) {
        try {
            // TextureSpec likely has an 'id' field of type Identifier
            // We use reflection to be safe since the exact field name might differ
            for (java.lang.reflect.Field field : textureSpec.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object fieldValue = field.get(textureSpec);
                if (fieldValue instanceof Identifier id) {
                    return Optional.of(id);
                }
            }
        } catch (Exception e) {
            // Fall through
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
