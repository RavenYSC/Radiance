package com.radiance.client.texture;

import com.radiance.client.constant.VulkanConstants;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

public class TextureTracker {

    public static Map<Identifier, Integer> textureID2GLID = new ConcurrentHashMap<>();
    public static Map<Integer, Texture> GLID2Texture = new ConcurrentHashMap<>();
    public static Map<Integer, Integer> GLID2SpecularGLID = new ConcurrentHashMap<>();
    public static Map<Integer, Integer> GLID2NormalGLID = new ConcurrentHashMap<>();
    public static Map<Integer, Integer> GLID2FlagGLID = new ConcurrentHashMap<>();

    public record Texture(int width, int height, int channel, VulkanConstants.VkFormat format,
                          int maxLayer) {

        public Texture {
            if (width <= 0 || height <= 0 || channel <= 0 || maxLayer < 0) {
                throw new IllegalArgumentException(
                    "Invalid texture width, height, channel, or maxLayer: " + width + ", " + height
                        + ", " + channel + ", " + maxLayer);
            }
        }

        // In 1.21.11, NativeImage.InternalFormat was removed.
        // Using NativeImage.Format with channelCount for compatibility.
        public Texture(int width, int height, NativeImage.Format format, int maxLayer) {
            this(width, height, format.getChannelCount(), getFormat(format.getChannelCount()), maxLayer);
        }

        private static VulkanConstants.VkFormat getFormat(int channelCount) {
            return switch (channelCount) {
                case 4 -> VulkanConstants.VkFormat.VK_FORMAT_R8G8B8A8_SRGB;
                case 3 -> VulkanConstants.VkFormat.VK_FORMAT_R8G8B8_SRGB;
                case 2 -> VulkanConstants.VkFormat.VK_FORMAT_R8G8_SRGB;
                case 1 -> VulkanConstants.VkFormat.VK_FORMAT_R8_SRGB;
                default -> throw new IllegalArgumentException(
                    "Unknown channel count: " + channelCount);
            };
        }
    }
}
