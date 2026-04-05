package com.radiance.mixins.vulkan_render_integration;

import net.minecraft.client.util.ScreenshotRecorder;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Stub: ScreenshotRecorder API changed significantly in 1.21.11.
 * - takeScreenshot no longer returns NativeImage
 * - loadFromTextureImage was removed from NativeImage
 * - Internal lambda methods changed
 * TODO: Reimplement screenshot capture using the new ScreenshotRecorder API.
 */
@Mixin(ScreenshotRecorder.class)
public class ScreenshotRecorderMixins {
}
