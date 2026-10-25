package com.shahid.cam;

import java.io.Serializable;

public enum RecordingState implements Serializable {
    STARTING,
    IN_PROGRESS,
    PAUSED,
    NONE,
    WAITING_FOR_CAMERA
}
