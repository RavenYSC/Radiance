package com.radiance.client.proxy.vulkan;

import com.radiance.client.constant.VulkanConstants;

public class TextureProxy {

    public synchronized static native int generateTextureId();

    public synchronized static native void prepareImage(int id, int mipLevels, int width,
        int height, int format);

    public static void prepareImage(int id, int mipLevels, int width, int height,
        VulkanConstants.VkFormat format) {
        prepareImage(id, mipLevels, width, height, format.getValue());
    }

    public synchronized static native void setFilter(int id, int samplingMode, int mipmapMode);

    public synchronized static native void setClamp(int id, int addressMode);

    public synchronized static native void queueUpload(long srcPointer,
        int srcSizeInBytes,
        int srcRowPixels,
        int dstId,
        int srcOffsetX,
        int srcOffsetY,
        int dstOffsetX,
        int dstOffsetY,
        int width,
        int height,
        int level);

    // OMM: 0 = FULLY_OPAQUE, 1 = FULLY_TRANSPARENT, 2 = MIXED
    public synchronized static native void setTextureAlphaClass(int id, int alphaClass);

    // In 1.21.11, NativeImage.InternalFormat was removed.
    // Using channelCount from NativeImage.Format instead.
    public static void prepareImage(int channelCount, int id,
        int mipLevels, int width, int height) {
        switch (channelCount) {
            case 4:
                prepareImage(id, mipLevels, width, height,
                    VulkanConstants.VkFormat.VK_FORMAT_R8G8B8A8_SRGB);
                break;
            case 3:
                prepareImage(id, mipLevels, width, height,
                    VulkanConstants.VkFormat.VK_FORMAT_R8G8B8_SRGB);
                break;
            case 2:
                prepareImage(id, mipLevels, width, height,
                    VulkanConstants.VkFormat.VK_FORMAT_R8G8_SRGB);
                break;
            case 1:
                prepareImage(id, mipLevels, width, height,
                    VulkanConstants.VkFormat.VK_FORMAT_R8_SRGB);
                break;
        }
    }
}
