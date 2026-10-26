package com.shahid.cam.opengl;

/**
 * WatermarkInfoProvider provides the current watermark text (timestamp, ShahidCam, location, etc).
 */
public interface WatermarkInfoProvider {
    /**
     * Returns the current watermark text to be rendered.
     */
    String getWatermarkText();
} 