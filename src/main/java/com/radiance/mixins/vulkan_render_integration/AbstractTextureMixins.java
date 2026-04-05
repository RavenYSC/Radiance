package com.radiance.mixins.vulkan_render_integration;

import com.radiance.client.constant.VulkanConstants;
import com.radiance.client.proxy.vulkan.TextureProxy;
import com.radiance.mixin_related.extensions.vulkan_render_integration.IAbstractTextureExt;
import net.minecraft.client.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * In 1.21.11, AbstractTexture was overhauled:
 * - glId field removed (replaced by glTexture: GpuTexture)
 * - getGlId() removed (replaced by getGlTexture(): GpuTexture)
 * - bindTexture() removed
 * - clearGlId() removed
 * - setFilter()/setClamp() may have been removed
 *
 * This mixin provides a synthetic integer texture ID system for the Vulkan renderer,
 * which needs integer IDs to identify textures.
 */
@Mixin(AbstractTexture.class)
public class AbstractTextureMixins implements IAbstractTextureExt {

    @Unique
    private static final AtomicInteger NEXT_TEXTURE_ID = new AtomicInteger(1);

    @Unique
    private int syntheticGlId = -1;

    @Override
    public int neoVoxelRT$getGlIDUnsafe() {
        if (this.syntheticGlId < 0) {
            throw new IllegalStateException("syntheticGlId is not initialized");
        }
        return this.syntheticGlId;
    }

    @Override
    public int getGlId() {
        synchronized (AbstractTextureMixins.class) {
            if (this.syntheticGlId == -1) {
                this.syntheticGlId = NEXT_TEXTURE_ID.getAndIncrement();
                TextureProxy.generateTextureId();
            }
            return this.syntheticGlId;
        }
    }
}
