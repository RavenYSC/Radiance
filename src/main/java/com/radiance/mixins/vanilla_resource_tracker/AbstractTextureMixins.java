package com.radiance.mixins.vanilla_resource_tracker;

import net.minecraft.client.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;

/**
 * In 1.21.11, AbstractTexture.bindTexture() and getGlId() were removed.
 * The Vulkan renderer provides getGlId() via IAbstractTextureExt interface.
 */
@Mixin(AbstractTexture.class)
public abstract class AbstractTextureMixins {
    // bindTexture() and getGlId() were removed in 1.21.11
    // getGlId() is now provided by the vulkan_render_integration.AbstractTextureMixins
    // via the IAbstractTextureExt interface
}
