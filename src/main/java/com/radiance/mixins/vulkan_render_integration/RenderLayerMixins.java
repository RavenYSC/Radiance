package com.radiance.mixins.vulkan_render_integration;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLayer.class)
public class RenderLayerMixins {

    @Shadow
    @Final
    @Mutable
    private static RenderLayer LIGHTNING;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void replaceLightning(CallbackInfo ci) {
        RenderSetup renderSetup = RenderSetup.builder(RenderPipelines.RENDERTYPE_LIGHTNING)
            .texture("Sampler0", Identifier.ofVanilla("textures/block/lightning.png"))
            .translucent()
            .build();
        LIGHTNING = RenderLayer.of("lightning", renderSetup);
    }
}
