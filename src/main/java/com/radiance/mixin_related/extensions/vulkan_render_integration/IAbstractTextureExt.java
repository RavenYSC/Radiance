package com.radiance.mixin_related.extensions.vulkan_render_integration;

import net.minecraft.client.texture.AbstractTexture;

public interface IAbstractTextureExt {

    int neoVoxelRT$getGlIDUnsafe();

    /**
     * Returns a synthetic GL-style texture ID for Vulkan renderer use.
     * In 1.21.11, AbstractTexture.getGlId() was removed and replaced with
     * getGlTexture() returning GpuTexture. This method provides a compatible
     * integer ID for the Vulkan rendering pipeline.
     */
    int getGlId();

    /**
     * Helper to get the GL ID from any AbstractTexture instance.
     * Casts to IAbstractTextureExt which is injected via mixin.
     */
    static int getGlId(AbstractTexture texture) {
        return ((IAbstractTextureExt) texture).getGlId();
    }
}
