package com.radiance.mixins.vulkan_render_integration;

import com.mojang.blaze3d.systems.RenderSystem;
import com.radiance.client.UnsafeManager;
import com.radiance.client.option.Options;
import com.radiance.client.cloud.CloudTileManager;
import com.radiance.client.proxy.vulkan.BufferProxy;
import com.radiance.client.proxy.world.ChunkProxy;
import com.radiance.client.proxy.world.EntityProxy;
import com.radiance.client.proxy.world.PlayerProxy;
import com.radiance.client.vertex.StorageVertexConsumerProvider;
import com.radiance.mixin_related.extensions.vulkan_render_integration.IGameRendererExt;
import com.radiance.mixin_related.extensions.vulkan_render_integration.ILightMapManagerExt;
import com.radiance.mixin_related.extensions.vulkan_render_integration.IOverlayTextureExt;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import net.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.option.CloudRenderMode;
// FogRenderer moved to net.minecraft.client.render.fog.FogRenderer in 1.21.11
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.ChunkRenderingDataPreparer;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.render.WorldBorderRendering;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.EndPortalBlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.BlockBreakingInfo;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixins {

    @Shadow
    private ClientWorld world;

    @Final
    @Shadow
    private MinecraftClient client;

    @Final
    @Shadow
    private EntityRenderDispatcher entityRenderManager;

    @Final
    @Shadow
    private BlockEntityRenderDispatcher blockEntityRenderManager;

    @Shadow
    private BuiltChunkStorage chunks;

    @Shadow
    private double lastCameraPitch;

    @Shadow
    private double lastCameraYaw;

    @Final
    @Shadow
    private ObjectArrayList<ChunkBuilder.BuiltChunk> builtChunks;

    @Shadow
    @Final
    private Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions;

    @Shadow
    @Final
    private WeatherRendering weatherRendering;

    @Shadow
    @Final
    private WorldBorderRendering worldBorderRendering;

    @Shadow
    private int ticks;
    @Shadow
    @Final
    private CloudRenderer cloudRenderer;
    // endregion

    // region <init>
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/client/render/SkyRendering"))
    private SkyRendering cancelNewSkyRendering() {
        return UnsafeManager.INSTANCE.allocateInstance(SkyRendering.class);
    }
    // endregion

    @Redirect(method = "scheduleTerrainUpdate()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/ChunkRenderingDataPreparer;scheduleTerrainUpdate()V"))
    public void cancelTerrainUpdateWithChunkRenderingDataPreparer(
        ChunkRenderingDataPreparer instance) {

    }

    // region <close>
    @Redirect(method = "close()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/SkyRendering;close()V"))
    public void cancelSkyRenderingClose(SkyRendering instance) {

    }

    @Redirect(method = "reload()V", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/render/ChunkRenderingDataPreparer;setStorage"
            + "(Lnet/minecraft/client/render/BuiltChunkStorage;)V"))
    public void cancelReloadWithChunkRenderingDataPreparerSetStorage(
        ChunkRenderingDataPreparer instance, BuiltChunkStorage storage) {

    }

    // region <render>
    @Shadow
    protected abstract boolean canDrawEntityOutlines();

    @Shadow
    protected abstract void applyFrustum(Frustum frustum);

    @Shadow
    protected abstract boolean hasBlindnessOrDarkness(Camera camera);

    @Inject(method =
        "render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;"
            + "ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;"
            + "Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
        at = @At("HEAD"), cancellable = true)
    public void redirectRender(ObjectAllocator allocator, RenderTickCounter tickCounter,
        boolean renderBlockOutline, Camera camera,
        Matrix4f effectedRotationMatrix, Matrix4f projectionMatrix, Matrix4f viewMatrix4f,
        GpuBufferSlice gpuBufferSlice, Vector4f fogColorVec, boolean skyRendered,
        CallbackInfo ci) {
        // TODO: 1.21.11 - verify new render parameters are correctly mapped
        GameRenderer gameRenderer = this.client.gameRenderer;
        PlayerProxy.setCameraPos(camera.getPos());

        float f = tickCounter.getTickDelta(false);
        RenderSystem.setShaderGameTime(this.world.getTime(), f);
        this.blockEntityRenderManager.configure(this.world, camera, this.client.crosshairTarget);
        this.entityRenderManager.configure(this.world, camera, this.client.targetedEntity);

        this.world.runQueuedChunkUpdates();
        this.world.getChunkManager().getLightingProvider().doLightUpdates();

        Vec3d vec3d = camera.getPos();
        double x = vec3d.getX();
        double y = vec3d.getY();
        double z = vec3d.getZ();

        Matrix4f viewMatrix = new Matrix4f(
            ((IGameRendererExt) gameRenderer).neoVoxelRT$getRotationMatrix());
        Matrix4f effectedViewMatrix = new Matrix4f(effectedRotationMatrix);

        // fog - In 1.21.11, BackgroundRenderer was renamed to FogRenderer and moved to fog subpackage.
        // FogRenderer.applyFog returns Vector4f instead of Fog, and the Fog class was removed.
        float h = gameRenderer.getViewDistanceBlocks();
        boolean bl2 = this.client.world.getDimensionEffects()
            .useThickFog(MathHelper.floor(x), MathHelper.floor(y))
            || this.client.inGameHud.getBossBarHud().shouldThickenFog();
        // Use the fogColorVec parameter passed to the render method instead
        float fogRed = fogColorVec.x();
        float fogGreen = fogColorVec.y();
        float fogBlue = fogColorVec.z();
        float fogAlpha = fogColorVec.w();
        // Default fog parameters - the actual fog distances come from FogData/FogRenderer
        // which manages fog via GPU buffers in 1.21.11
        float fogStart = bl2 ? h * 0.05f : h * 0.75f;
        float fogEnd = h;
        int fogShapeId = 0; // SPHERE

        TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
        OverlayTexture overlayTexture = gameRenderer.getOverlayTexture();
        int overlayTextureID = ((IOverlayTextureExt) overlayTexture).neoVoxelRT$getTexture()
            .getGlId();
        int endSkyTextureID = textureManager.getTexture(EndPortalBlockEntityRenderer.SKY_TEXTURE)
            .getGlId();
        int endPortalTextureID = textureManager.getTexture(
            EndPortalBlockEntityRenderer.PORTAL_TEXTURE).getGlId();
        BufferProxy.updateWorldUniform(camera, viewMatrix, effectedViewMatrix, projectionMatrix,
            overlayTextureID, fogStart, fogEnd, fogRed, fogGreen, fogBlue, fogAlpha, fogShapeId,
            world, endSkyTextureID, endPortalTextureID);

        // Sky
        float tickDelta = tickCounter.getTickDelta(false);
        int envDim = Options.getEnvironmentDimensionIndex(this.world);
        float skyBrightness = Options.getSkyBrightness(envDim);
        float rainBlendStrength = Options.getRainBlendStrength(envDim);
        float sunSizeMultiplier = Options.getSunSizeMultiplier(envDim);
        float moonSizeMultiplier = Options.getMoonSizeMultiplier(envDim);
        float sunIntensityMultiplier = Options.getSunIntensityMultiplier(envDim);
        float moonIntensityMultiplier = Options.getMoonIntensityMultiplier(envDim);
        float waterTintR = Options.getWaterTintR(envDim);
        float waterTintG = Options.getWaterTintG(envDim);
        float waterTintB = Options.getWaterTintB(envDim);
        float waterFogStrength = Options.getWaterFogStrength(envDim);
        float skyAngle = this.world.getSkyAngle(tickDelta);

        int baseColor = this.world.getSkyColor(this.client.gameRenderer.getCamera().getPos(),
            tickDelta);
        float baseColorR = ColorHelper.getRedFloat(baseColor) * skyBrightness;
        float baseColorG = ColorHelper.getGreenFloat(baseColor) * skyBrightness;
        float baseColorB = ColorHelper.getBlueFloat(baseColor) * skyBrightness;

        DimensionEffects dimensionEffects = this.world.getDimensionEffects();
        int horizontalColor = dimensionEffects.getSkyColor(skyAngle);
        float horizontalColorR = ColorHelper.getRedFloat(horizontalColor) * skyBrightness;
        float horizontalColorG = ColorHelper.getGreenFloat(horizontalColor) * skyBrightness;
        float horizontalColorB = ColorHelper.getBlueFloat(horizontalColor) * skyBrightness;
        float horizontalColorA = ColorHelper.getAlphaFloat(horizontalColor);

        // Compute sun/moon direction from the actual day-time tick value.
        // This keeps lighting aligned with `/time set ...` and avoids phase errors from getSkyAngle().
        long dayTimeTicks = this.world.getTimeOfDay() % 24000L;
        float dayFrac = (dayTimeTicks + tickDelta) / 24000.0F;
        float skyAngleForSun = dayFrac - 0.25F;

        Vector3f sunDirection;
        Vector3f moonDirection;

        if (Options.sunPathMode == 1) {
            // Physical mode: apply inclination (axial tilt) and azimuth offset
            float inclDeg = (float) Options.sunInclinationDeg;
            float azOffDeg = (float) Options.sunAzimuthOffsetDeg;

            MatrixStack ms = new MatrixStack();
            ms.push();
            // 1. Base Y rotation (vanilla baseline) + azimuth offset
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F + azOffDeg));
            // 2. Tilt the orbital plane by inclination (Z-axis rotation tilts the orbit)
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(inclDeg));
            // 3. Rotate sun around the tilted orbital plane
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(skyAngleForSun * 360.0F));
            Matrix4f rot = ms.peek().getPositionMatrix();
            sunDirection = rot.transformPosition(0, 1, 0, new Vector3f()).normalize();
            ms.pop();

            // Moon direction
            float moonInclDeg, moonAzOffDeg;
            if (Options.moonFollowSun) {
                moonInclDeg = inclDeg;
                moonAzOffDeg = azOffDeg;
            } else {
                moonInclDeg = (float) Options.moonInclinationDeg;
                moonAzOffDeg = (float) Options.moonAzimuthOffsetDeg;
            }
            MatrixStack moonMs = new MatrixStack();
            moonMs.push();
            moonMs.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F + moonAzOffDeg));
            moonMs.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(moonInclDeg));
            // Moon is 180deg opposite the sun in its orbit
            moonMs.multiply(RotationAxis.POSITIVE_X.rotationDegrees((skyAngleForSun + 0.5F) * 360.0F));
            Matrix4f moonRot = moonMs.peek().getPositionMatrix();
            moonDirection = moonRot.transformPosition(0, 1, 0, new Vector3f()).normalize();
            moonMs.pop();
        } else {
            // Legacy mode: vanilla-style overhead arc (current behavior)
            MatrixStack ms = new MatrixStack();
            ms.push();
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(skyAngleForSun * 360.0F));
            Matrix4f rot = ms.peek().getPositionMatrix();
            // Shaders treat sunDirection.y as elevation above the horizon.
            // In vanilla sky rendering the sun quad starts at +Y before being rotated.
            sunDirection = rot.transformPosition(0, 1, 0, new Vector3f()).normalize();
            ms.pop();

            // Moon opposite sun in legacy mode
            moonDirection = new Vector3f(-sunDirection.x, -sunDirection.y, -sunDirection.z);
        }

        int skyType = dimensionEffects.getSkyType().ordinal();

        boolean sunRisingOrSetting = dimensionEffects.isSunRisingOrSetting(skyAngle);

        // TODO: 1.21.11 - isSkyDark() was removed; compute sky darkness differently if needed
        boolean skyDark = false;

        boolean hasBlindnessOrDarkness = this.hasBlindnessOrDarkness(camera);

        int submersionType = camera.getSubmersionType().ordinal();

        int moonPhase = this.world.getMoonPhase();

        float rainGradient = this.world.getRainGradient(tickDelta);

        int sunTextureID = textureManager.getTexture(SkyRendering.SUN_TEXTURE).getGlId();

        int moonTextureID = textureManager.getTexture(SkyRendering.MOON_PHASES_TEXTURE).getGlId();

        CloudRenderMode cloudRenderMode = this.client.options.getCloudRenderModeValue();
        float cloudBaseHeight = Float.NaN;
        float cloudThickness = Options.getCloudThicknessBlocks(envDim);
        float cloudDensityScale = 0.0F;
        float cloudAlbedoScale = Options.getCloudBrightness(envDim);

        float cloudPuffiness = Options.getCloudPuffiness(envDim);
        float cloudDetailScale = Options.getCloudDetailScale(envDim);
        // Fast = analytic flat slab (detailStrength=0); Fancy = 3D FBM stepped volumetric.
        float cloudDetailStrength = (cloudRenderMode == CloudRenderMode.FANCY)
            ? Options.getCloudDetailStrength(envDim)
            : 0.0f;
        float cloudAnisotropy = Options.getCloudAnisotropy(envDim);
        float cloudShadowStrength = Options.getCloudShadowStrength(envDim);
        float cloudDensity = Options.getCloudDensity(envDim);
        float cloudNoiseAffectsShadows = Options.getCloudNoiseAffectsShadows(envDim) ? 1.0F : 0.0F;

        float cloudAmbientStrength = 0.15F;
        float cloudSunOcclusionStrength = 1.0F;

        float vanillaCloudsHeight = this.world.getDimensionEffects().getCloudsHeight();
        if (cloudRenderMode != CloudRenderMode.OFF && !Float.isNaN(vanillaCloudsHeight)) {
            cloudBaseHeight = vanillaCloudsHeight + 0.33F + Options.getCloudHeightOffset(envDim);
            float cloudAlpha = Options.getCloudAlpha(envDim);
            // Extinction coefficient in "per block" units. Tuned so 100% alpha produces visible sun occlusion and ground shadows.
            cloudDensityScale = 0.35F * cloudAlpha * cloudDensity;
        }

        BufferProxy.updateMapping();

        ILightMapManagerExt lightMapManagerExt = (ILightMapManagerExt) (gameRenderer.getLightmapTextureManager());
        BufferProxy.updateLightMapUniform(lightMapManagerExt.neoVoxelRT$getAmbientLightFactor(),
            lightMapManagerExt.neoVoxelRT$getSkyFactor(),
            lightMapManagerExt.neoVoxelRT$getBlockFactor(),
            lightMapManagerExt.neoVoxelRT$isUseBrightLightmap(),
            lightMapManagerExt.neoVoxelRT$getSkyLightColor(),
            lightMapManagerExt.neoVoxelRT$getNightVisionFactor(),
            lightMapManagerExt.neoVoxelRT$getDarknessScale(),
            lightMapManagerExt.neoVoxelRT$getDarkenWorldFactor(),
            lightMapManagerExt.neoVoxelRT$getBrightnessFactor());

        // Entities
        // TODO: 1.21.11 - entity rendering restructured into WorldRenderState; renderedEntities list no longer available
        EntityProxy.queueEntitiesBuild(camera, List.of(), this.entityRenderManager,
            tickCounter, canDrawEntityOutlines());

        // TODO: 1.21.11 - noCullingBlockEntities removed; passing empty set
        Pair<List<StorageVertexConsumerProvider>, EntityProxy.EntityRenderDataList> crumblingRenderData = EntityProxy.queueBlockEntitiesRebuild(
            camera, chunks, Set.of(), blockBreakingProgressions,
            blockEntityRenderManager, tickDelta);
        EntityProxy.queueCrumblingRebuild(camera, blockBreakingProgressions,
            this.client.getBlockRenderManager(), this.world, crumblingRenderData.getLeft(),
            crumblingRenderData.getRight());

        // TODO: 1.21.11 - frustum field removed; passing null for particle frustum culling
        EntityProxy.queueParticleRebuild(camera, tickDelta, null);

        if (renderBlockOutline) {
            EntityProxy.queueTargetBlockOutlineRebuild(camera, world);
        }

        EntityProxy.queueWeatherBuild(this.weatherRendering, this.worldBorderRendering, this.world,
            camera, this.ticks, tickDelta);

        // clouds
        if (cloudRenderMode != CloudRenderMode.OFF) {
            if (!Float.isNaN(vanillaCloudsHeight)) {
                float ticks = (float) this.ticks + f;
                int color = this.world.getCloudsColor(f);
                float cloudHeight = vanillaCloudsHeight + 0.33F + Options.getCloudHeightOffset(envDim);
                this.cloudRenderer.renderClouds(color, cloudRenderMode, cloudHeight, null, null,
                    camera.getPos(), ticks);
            }
        }

        int cloudTileTextureID = -1;
        int cloudCenterX = 0;
        int cloudCenterZ = 0;
        float cloudOffsetX = 0.0F;
        float cloudOffsetZ = 0.0F;
        float cloudTicks = 0.0F;
        if (CloudTileManager.isValid()) {
            cloudTileTextureID = CloudTileManager.getCloudMaskTextureId();
            cloudCenterX = CloudTileManager.getCenterCellX();
            cloudCenterZ = CloudTileManager.getCenterCellZ();
            cloudOffsetX = CloudTileManager.getOffsetX();
            cloudOffsetZ = CloudTileManager.getOffsetZ();
            cloudTicks = CloudTileManager.getTicks();
        }

        BufferProxy.updateSkyUniform(baseColorR, baseColorG, baseColorB, horizontalColorR,
            horizontalColorG, horizontalColorB, horizontalColorA, sunDirection, moonDirection,
            skyType, sunRisingOrSetting, skyDark, hasBlindnessOrDarkness, submersionType,
            moonPhase, rainGradient, sunTextureID, moonTextureID, sunSizeMultiplier,
            moonSizeMultiplier, sunIntensityMultiplier, moonIntensityMultiplier,
            waterTintR, waterTintG, waterTintB, waterFogStrength, rainBlendStrength,
            skyBrightness, cloudBaseHeight, cloudThickness, cloudDensityScale, cloudAlbedoScale,
            cloudTileTextureID, cloudCenterX, cloudCenterZ,
            cloudOffsetX, cloudOffsetZ, cloudTicks,
            cloudPuffiness, cloudDetailScale, cloudDetailStrength, cloudAnisotropy,
            cloudShadowStrength, cloudAmbientStrength, cloudSunOcclusionStrength,
            cloudNoiseAffectsShadows);

        // Chunks
        ChunkProxy.rebuild(camera);

        ci.cancel();
    }
    // endregion

    // region <setWorld>
    @Redirect(method = "setWorld(Lnet/minecraft/client/world/ClientWorld;)V", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/render/ChunkRenderingDataPreparer;setStorage"
            + "(Lnet/minecraft/client/render/BuiltChunkStorage;)V"))
    public void cancelSetWorldChunkRenderingDataPreparerSetStorage(
        ChunkRenderingDataPreparer instance, BuiltChunkStorage storage) {

    }
    // endregion

    // region <addBuiltChunk>
    @Redirect(method = "addBuiltChunk(Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk;)V", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/render/ChunkRenderingDataPreparer;schedulePropagationFrom"
            + "(Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk;)V"))
    public void cancelPropagateWithChunkRenderingDataPreparer(ChunkRenderingDataPreparer instance,
        ChunkBuilder.BuiltChunk builtChunk) {

    }
    // endregion

    // region <onChunkUnload>
    @Redirect(method = "onChunkUnload(J)V", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/render/ChunkRenderingDataPreparer;schedulePropagationFrom"
            + "(Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk;)V"))
    public void cancelPropagateUnloadWithChunkRenderingDataPreparer(
        ChunkRenderingDataPreparer instance, ChunkBuilder.BuiltChunk builtChunk) {

    }
    // endregion

    // region <scheduleNeighborUpdates>
    @Redirect(method = "scheduleNeighborUpdates(Lnet/minecraft/util/math/ChunkPos;)V", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/render/ChunkRenderingDataPreparer;addNeighbors(Lnet/minecraft/util/math/ChunkPos;)"
            + "V"))
    public void cancelNeighborUpdatesWithChunkRenderingDataPreparer(
        ChunkRenderingDataPreparer instance, ChunkPos chunkPos) {

    }
    // endregion

    // region <isRenderingReady>
    @Inject(method = "isRenderingReady(Lnet/minecraft/util/math/BlockPos;)Z", at = @At(value = "HEAD"), cancellable = true)
    public void redirectIsRenderingReady(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ChunkBuilder.BuiltChunk builtChunk = chunks.getRenderedChunk(pos);

        if (builtChunk == null) {
            cir.setReturnValue(false);
        // TODO: 1.21.11 - verify getCurrentRenderData() return type matches expected usage
        } else if (builtChunk.getCurrentRenderData().isEmpty(null)) {
            cir.setReturnValue(true);
        } else if (builtChunk.getCurrentRenderData() == ChunkProxy.PROCESSED) {
            cir.setReturnValue(ChunkProxy.isChunkReady(builtChunk));
        }
    }
    // endregion

    // region <>
    @Inject(method = "getCompletedChunkCount()I", at = @At(value = "HEAD"), cancellable = true)
    public void fixGetCompletedChunkCount(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ChunkProxy.builtChunkNum - 54); // 54 + 10 = 64
    }
    // endregion
}
