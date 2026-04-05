package com.radiance.mixins.vulkan_render_integration;

/**
 * Stub: TextureUtil.generateTextureId() and prepareImage() were removed in 1.21.11.
 * The texture system now uses GpuTexture objects instead of GL texture IDs.
 * The Vulkan renderer texture management is handled via TextureProxy directly
 * through AbstractTextureMixins instead.
 */
// Disabled: TextureUtil methods targeted by this mixin no longer exist in 1.21.11
// @Mixin(TextureUtil.class)
public class TextureUtilMixins {
}
