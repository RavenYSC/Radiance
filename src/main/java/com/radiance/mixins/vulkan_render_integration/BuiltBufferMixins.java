package com.radiance.mixins.vulkan_render_integration;

import com.radiance.client.vertex.PBRVertexFormatElements;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import net.minecraft.client.render.BuiltBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.util.math.Vec3fArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BuiltBuffer.class)
public class BuiltBufferMixins {

    @Inject(method = "collectCentroids(Ljava/nio/ByteBuffer;ILcom/mojang/blaze3d/vertex/VertexFormat;)Lnet/minecraft/client/util/math/Vec3fArray;",
        at = @At(value = "HEAD"),
        cancellable = true)
    private static void addPBRPosition(ByteBuffer buf, int vertexCount, VertexFormat format,
        CallbackInfoReturnable<Vec3fArray> cir) {
        int i = format.getOffset(VertexFormatElement.POSITION);
        if (i == -1) {
            i = format.getOffset(PBRVertexFormatElements.PBR_POS);
        }
        if (i == -1) {
            throw new IllegalArgumentException(
                "Cannot identify quad centers with no position element");
        } else {
            FloatBuffer floatBuffer = buf.asFloatBuffer();
            int j = format.getVertexSizeByte() / 4;
            int k = j * 4;
            int l = vertexCount / 4;
            Vec3fArray vec3fArray = new Vec3fArray(l);

            for (int m = 0; m < l; m++) {
                int n = m * k + i;
                int o = n + j * 2;
                float f = floatBuffer.get(n);
                float g = floatBuffer.get(n + 1);
                float h = floatBuffer.get(n + 2);
                float p = floatBuffer.get(o);
                float q = floatBuffer.get(o + 1);
                float r = floatBuffer.get(o + 2);
                vec3fArray.set(m, (f + p) / 2.0F, (g + q) / 2.0F, (h + r) / 2.0F);
            }

            cir.setReturnValue(vec3fArray);
        }
    }
}
