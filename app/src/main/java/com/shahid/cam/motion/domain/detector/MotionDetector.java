package com.shahid.cam.motion.domain.detector;

import android.media.Image;

public interface MotionDetector {
    float detectScore(Image image);
}
