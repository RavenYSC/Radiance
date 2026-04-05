package com.radiance.mixins.vulkan_render_integration;

import com.radiance.mixin_related.extensions.vulkan_render_integration.ILightMapManagerExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixins implements ILightMapManagerExt {

    @Unique
    private float ambientLightFactor = 0;
    @Unique
    private float skyFactor = 0;
    @Unique
    private float blockFactor = 0;
    @Unique
    private boolean useBrightLightmap = false;
    @Unique
    private Vector3f skyLightColor = new Vector3f(0.0f, 0.0f, 0.0f);
    @Unique
    private float nightVisionFactor = 0;
    @Unique
    private float darknessScale = 0;
    @Unique
    private float darkenWorldFactor = 0;
    @Unique
    private float brightnessFactor = 0;

    // In 1.21.11, lightmapFramebuffer was replaced with glTexture (GpuTexture) and glTextureView
    // SimpleFramebuffer is no longer used for the lightmap.
    // The enable(), disable(), close() methods were removed.
    // getDarknessFactor() was removed.

    @Shadow
    private boolean dirty;
    @Shadow
    private float flickerIntensity;
    @Final
    @Shadow
    private GameRenderer renderer;
    @Final
    @Shadow
    private MinecraftClient client;

    @Shadow
    protected abstract float getDarkness(LivingEntity entity, float factor, float delta);

    // region <update>
    @Inject(method = "update(F)V", at = @At(value = "HEAD"), cancellable = true)
    public void redirectUpdate(float delta, CallbackInfo ci) {
        if (this.dirty) {
            this.dirty = false;
            Profiler profiler = Profilers.get();
            profiler.push("lightTex");
            ClientWorld clientWorld = this.client.world;
            if (clientWorld != null) {
                float f = clientWorld.getSkyBrightness(1.0F);
                float skyFactor;
                if (clientWorld.getLightningTicksLeft() > 0) {
                    skyFactor = 1.0F;
                } else {
                    skyFactor = f * 0.95F + 0.05F;
                }

                float
                    h =
                    this.client.options.getDarknessEffectScale()
                        .getValue()
                        .floatValue();
                // getDarknessFactor was removed in 1.21.11; darkness factor defaults to 0
                float i = 0.0F;
                float darknessScale = this.getDarkness(this.client.player, i, delta) * h;
                float k = this.client.player.getUnderwaterVisibility();
                float nightVisionFactor;
                if (this.client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                    nightVisionFactor = GameRenderer.getNightVisionStrength(this.client.player,
                        delta);
                } else if (k > 0.0F && this.client.player.hasStatusEffect(
                    StatusEffects.CONDUIT_POWER)) {
                    nightVisionFactor = k;
                } else {
                    nightVisionFactor = 0.0F;
                }

                Vector3f skyLightColor = new Vector3f(f, f, 1.0F).lerp(
                    new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
                float blockFactor = this.flickerIntensity + 1.5F;
                float
                    ambientLightFactor =
                    clientWorld.getDimension()
                        .ambientLight();
                boolean
                    useBrightLightmap =
                    clientWorld.getDimensionEffects()
                        .shouldBrightenLighting();
                float
                    o =
                    this.client.options.getGamma()
                        .getValue()
                        .floatValue();

                float darkenWorldFactor = this.renderer.getSkyDarkness(delta);
                float brightnessFactor = Math.max(0.0F, o - i);

                this.ambientLightFactor = ambientLightFactor;
                this.skyFactor = skyFactor;
                this.blockFactor = blockFactor;
                this.useBrightLightmap = useBrightLightmap;
                this.skyLightColor = skyLightColor;
                this.nightVisionFactor = nightVisionFactor;
                this.darknessScale = darknessScale;
                this.darkenWorldFactor = darkenWorldFactor;
                this.brightnessFactor = brightnessFactor;

                profiler.pop();
            }
        }
        ci.cancel();
    }
    // endregion

    public float neoVoxelRT$getAmbientLightFactor() {
        return ambientLightFactor;
    }

    public float neoVoxelRT$getSkyFactor() {
        return skyFactor;
    }

    public float neoVoxelRT$getBlockFactor() {
        return blockFactor;
    }

    public boolean neoVoxelRT$isUseBrightLightmap() {
        return useBrightLightmap;
    }

    public Vector3f neoVoxelRT$getSkyLightColor() {
        return skyLightColor;
    }

    public float neoVoxelRT$getNightVisionFactor() {
        return nightVisionFactor;
    }

    public float neoVoxelRT$getDarknessScale() {
        return darknessScale;
    }

    public float neoVoxelRT$getDarkenWorldFactor() {
        return darkenWorldFactor;
    }

    public float neoVoxelRT$getBrightnessFactor() {
        return brightnessFactor;
    }
}
