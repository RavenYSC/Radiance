package com.radiance.mixins.vulkan_render_integration;

import com.mojang.blaze3d.systems.RenderSystem;
import com.radiance.client.proxy.vulkan.RendererProxy;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.tracy.TracyFrameCapturer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixins {

    @Inject(method = "maxSupportedTextureSize()I", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private static void redirectMaxSupportedTextureSize(CallbackInfoReturnable<Integer> cir) {
        int maxImageSize = RendererProxy.maxSupportedTextureSize();
        cir.setReturnValue(maxImageSize);
    }

    // Disabled: setShader(ShaderProgramKey) was removed in 1.21.11.
    // The new rendering pipeline uses RenderPipeline/RenderPass instead of ShaderProgramKey.
    // TODO: Implement equivalent overlay pipeline binding via the new RenderPass system.

    @Redirect(method = "flipFrame(Lnet/minecraft/client/util/Window;Lnet/minecraft/client/util/tracy/TracyFrameCapturer;)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V", remap = false))
    private static void cancelSwapBuffers(long window) {

    }

    // Disabled: renderCrosshair was removed in 1.21.11.
    // @Redirect(method = "renderCrosshair(I)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GLX;_renderCrosshair(IZZZ)V"))
    // private static void cancelDrawCrossAirForNow(int size, boolean drawX, boolean drawY,
    //     boolean drawZ) {
    // }
}
