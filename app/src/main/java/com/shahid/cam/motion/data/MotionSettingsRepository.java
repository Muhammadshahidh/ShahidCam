package com.shahid.cam.motion.data;

import com.shahid.cam.motion.domain.model.MotionSettings;
import com.shahid.cam.motion.domain.model.MotionTriggerMode;

public interface MotionSettingsRepository {
    MotionSettings getSettings();
    void setEnabled(boolean enabled);
    void setTriggerMode(MotionTriggerMode mode);
    void setSensitivity(int sensitivity);
    void setAnalysisFps(int fps);
    void setDebounceMs(int debounceMs);
    void setPostRollMs(int postRollMs);
    void setPreRollSeconds(int seconds);
    void setAutoTorchEnabled(boolean enabled);
}
