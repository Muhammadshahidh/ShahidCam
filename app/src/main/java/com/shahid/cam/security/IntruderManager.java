package com.shahid.cam.security;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.shahid.cam.Constants;
import com.shahid.cam.FLog;
import com.shahid.cam.SharedPreferencesManager;
import com.shahid.cam.dualcam.service.DualCameraRecordingService;
import com.shahid.cam.services.RecordingService;
import com.shahid.cam.utils.ServiceUtils;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coordinates Wrong Password / PIN -> Front Camera Intruder Selfie capture.
 *
 * Privacy & Policy Guarantees:
 * - Only activates after explicit user opt-in and explicit CAMERA permission grant.
 * - Uses standard Android Camera2 APIs cleanly (no covert system bypass).
 * - Deduplicates rapid/repeated signals so a single failed attempt produces at most one capture.
 * - Never interrupts an ongoing video recording session.
 * - Stores captured images exclusively in private app storage; never uploads externally.
 */
public final class IntruderManager {
    private static final String TAG = "IntruderManager";
    private static final long MIN_CAPTURE_INTERVAL_MS = 2500L;
    private static final long CAMERA_TIMEOUT_MS = 6500L;

    private static volatile IntruderManager instance;

    private final Context appContext;
    private final AtomicBoolean captureInProgress = new AtomicBoolean(false);
    private volatile long lastAttemptTimestampMs = 0L;

    private IntruderManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    @NonNull
    public static IntruderManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (IntruderManager.class) {
                if (instance == null) {
                    instance = new IntruderManager(context);
                }
            }
        }
        return instance;
    }

    public boolean isFeatureEnabled() {
        return SharedPreferencesManager.getInstance(appContext).isIntruderSelfieEnabled();
    }

    public void setFeatureEnabled(boolean enabled) {
        SharedPreferencesManager.getInstance(appContext).setIntruderSelfieEnabled(enabled);
    }

    public static boolean hasCameraPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasFrontCamera(@NonNull Context context) {
        return findFrontCameraId(context) != null;
    }

    @Nullable
    public static String findFrontCameraId(@NonNull Context context) {
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (manager == null) return null;
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId;
                }
            }
        } catch (Exception e) {
            FLog.w(TAG, "Error inspecting camera list", e);
        }
        return null;
    }

    /**
     * Called when AppLock verifies an incorrect PIN or Password.
     * Safe to invoke from any thread. Guarantees at most one capture per attempt.
     */
    public void onWrongCredentialAttempt(@NonNull String reasonTag) {
        try {
            if (!isFeatureEnabled()) {
                return;
            }
            if (!hasCameraPermission(appContext)) {
                FLog.w(TAG, "Intruder capture skipped: CAMERA permission not granted");
                return;
            }
            String frontCameraId = findFrontCameraId(appContext);
            if (frontCameraId == null) {
                FLog.w(TAG, "Intruder capture skipped: no front-facing camera available");
                return;
            }
            if (isAnyCameraRecordingActive()) {
                FLog.w(TAG, "Intruder capture skipped: active recording session in progress");
                return;
            }

            long now = System.currentTimeMillis();
            synchronized (this) {
                if ((now - lastAttemptTimestampMs) < MIN_CAPTURE_INTERVAL_MS) {
                    FLog.d(TAG, "Intruder capture debounced within cooldown window");
                    return;
                }
                if (!captureInProgress.compareAndSet(false, true)) {
                    FLog.d(TAG, "Intruder capture already running");
                    return;
                }
                lastAttemptTimestampMs = now;
            }

            startSingleFrontCapture(frontCameraId, reasonTag);
        } catch (Exception e) {
            FLog.e(TAG, "Unexpected error handling wrong credential attempt", e);
            captureInProgress.set(false);
        }
    }

    private boolean isAnyCameraRecordingActive() {
        try {
            if (SharedPreferencesManager.getInstance(appContext).isRecordingInProgress()) {
                return true;
            }
            if (ServiceUtils.isServiceRunning(appContext, RecordingService.class)) {
                return true;
            }
            if (ServiceUtils.isServiceRunning(appContext, DualCameraRecordingService.class)) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    private void startSingleFrontCapture(@NonNull String frontCameraId, @NonNull String reasonTag) {
        HandlerThread cameraThread = new HandlerThread("ShahidCamIntruderCam");
        cameraThread.start();
        Handler cameraHandler = new Handler(cameraThread.getLooper());
        Handler mainHandler = new Handler(Looper.getMainLooper());

        final CameraDevice[] openDevice = new CameraDevice[1];
        final CameraCaptureSession[] openSession = new CameraCaptureSession[1];
        final ImageReader[] openReader = new ImageReader[1];
        final AtomicBoolean finished = new AtomicBoolean(false);

        final Runnable cleanup = () -> {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            try {
                if (openSession[0] != null) {
                    try {
                        openSession[0].close();
                    } catch (Exception ignored) {
                    }
                    openSession[0] = null;
                }
                if (openDevice[0] != null) {
                    try {
                        openDevice[0].close();
                    } catch (Exception ignored) {
                    }
                    openDevice[0] = null;
                }
                if (openReader[0] != null) {
                    try {
                        openReader[0].close();
                    } catch (Exception ignored) {
                    }
                    openReader[0] = null;
                }
            } finally {
                try {
                    cameraThread.quitSafely();
                } catch (Exception ignored) {
                }
                captureInProgress.set(false);
            }
        };

        Runnable timeoutRunnable = () -> {
            if (!finished.get()) {
                FLog.w(TAG, "Intruder front camera capture timed out; releasing resources");
                cleanup.run();
            }
        };
        mainHandler.postDelayed(timeoutRunnable, CAMERA_TIMEOUT_MS);

        try {
            CameraManager manager = (CameraManager) appContext.getSystemService(Context.CAMERA_SERVICE);
            if (manager == null) {
                mainHandler.removeCallbacks(timeoutRunnable);
                cleanup.run();
                return;
            }

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(frontCameraId);
            Size targetSize = chooseCaptureSize(characteristics);
            int jpegOrientation = computeJpegOrientation(characteristics);

            ImageReader reader = ImageReader.newInstance(
                    targetSize.getWidth(), targetSize.getHeight(), ImageFormat.JPEG, 2);
            openReader[0] = reader;

            final AtomicBoolean imageHandled = new AtomicBoolean(false);
            reader.setOnImageAvailableListener(r -> {
                if (!imageHandled.compareAndSet(false, true)) {
                    try (Image extra = r.acquireLatestImage()) {
                        // Drain without saving duplicate frame
                    } catch (Exception ignored) {
                    }
                    return;
                }
                try (Image image = r.acquireLatestImage()) {
                    if (image != null) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        IntruderRecord saved = IntruderStorage.saveCapture(appContext, bytes, reasonTag);
                        if (saved != null) {
                            FLog.i(TAG, "Captured intruder selfie: " + saved.id);
                            notifyCaptureBroadcast();
                        }
                    }
                } catch (Exception e) {
                    FLog.e(TAG, "Error saving intruder JPEG frame", e);
                } finally {
                    mainHandler.removeCallbacks(timeoutRunnable);
                    cleanup.run();
                }
            }, cameraHandler);

            manager.openCamera(frontCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    openDevice[0] = camera;
                    if (finished.get()) {
                        cleanup.run();
                        return;
                    }
                    try {
                        Surface readerSurface = reader.getSurface();
                        createSessionCompat(camera, readerSurface, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                openSession[0] = session;
                                if (finished.get()) {
                                    cleanup.run();
                                    return;
                                }
                                try {
                                    CaptureRequest.Builder req =
                                            camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                                    req.addTarget(readerSurface);
                                    req.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                                    req.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                    req.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                                    req.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation);
                                    session.capture(req.build(), new CameraCaptureSession.CaptureCallback() {
                                        @Override
                                        public void onCaptureCompleted(@NonNull CameraCaptureSession s,
                                                                       @NonNull CaptureRequest r,
                                                                       @NonNull TotalCaptureResult result) {
                                            // ImageReader listener handles saving & cleanup
                                        }
                                    }, cameraHandler);
                                } catch (Exception e) {
                                    FLog.e(TAG, "Failed capturing intruder frame", e);
                                    mainHandler.removeCallbacks(timeoutRunnable);
                                    cleanup.run();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                FLog.w(TAG, "Front camera session configuration failed");
                                mainHandler.removeCallbacks(timeoutRunnable);
                                cleanup.run();
                            }
                        }, cameraHandler);
                    } catch (Exception e) {
                        FLog.e(TAG, "Error creating intruder camera session", e);
                        mainHandler.removeCallbacks(timeoutRunnable);
                        cleanup.run();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    mainHandler.removeCallbacks(timeoutRunnable);
                    cleanup.run();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    FLog.w(TAG, "Front camera error code=" + error);
                    mainHandler.removeCallbacks(timeoutRunnable);
                    cleanup.run();
                }
            }, cameraHandler);

        } catch (SecurityException se) {
            FLog.w(TAG, "Camera permission denied during intruder capture", se);
            mainHandler.removeCallbacks(timeoutRunnable);
            cleanup.run();
        } catch (CameraAccessException cae) {
            FLog.e(TAG, "CameraAccessException opening front camera", cae);
            mainHandler.removeCallbacks(timeoutRunnable);
            cleanup.run();
        } catch (Exception e) {
            FLog.e(TAG, "Unexpected exception opening front camera", e);
            mainHandler.removeCallbacks(timeoutRunnable);
            cleanup.run();
        }
    }

    @SuppressWarnings("deprecation")
    private void createSessionCompat(@NonNull CameraDevice camera,
                                     @NonNull Surface surface,
                                     @NonNull CameraCaptureSession.StateCallback callback,
                                     @NonNull Handler handler) throws CameraAccessException {
        camera.createCaptureSession(Collections.singletonList(surface), callback, handler);
    }

    @NonNull
    private Size chooseCaptureSize(@NonNull CameraCharacteristics characteristics) {
        Size fallback = new Size(640, 480);
        try {
            StreamConfigurationMap map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) return fallback;
            Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
            if (sizes == null || sizes.length == 0) return fallback;

            Size best = sizes[0];
            int targetPixels = 960 * 720;
            int bestDiff = Integer.MAX_VALUE;
            for (Size candidate : sizes) {
                int w = candidate.getWidth();
                int h = candidate.getHeight();
                if (w > 1280 || h > 960) continue;
                int diff = Math.abs((w * h) - targetPixels);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    best = candidate;
                }
            }
            return best;
        } catch (Exception e) {
            return fallback;
        }
    }

    private int computeJpegOrientation(@NonNull CameraCharacteristics characteristics) {
        Integer sensorOrientationObj = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int sensorOrientation = sensorOrientationObj != null ? sensorOrientationObj : 270;

        int rotation = Surface.ROTATION_0;
        try {
            WindowManager wm = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                @SuppressWarnings("deprecation")
                Display display = wm.getDefaultDisplay();
                if (display != null) {
                    rotation = display.getRotation();
                }
            }
        } catch (Exception ignored) {
        }

        int deviceDegrees;
        switch (rotation) {
            case Surface.ROTATION_90:
                deviceDegrees = 270;
                break;
            case Surface.ROTATION_180:
                deviceDegrees = 180;
                break;
            case Surface.ROTATION_270:
                deviceDegrees = 90;
                break;
            case Surface.ROTATION_0:
            default:
                deviceDegrees = 0;
                break;
        }
        // Front camera is mirrored relative to rear camera
        return (sensorOrientation - deviceDegrees + 360) % 360;
    }

    private void notifyCaptureBroadcast() {
        try {
            Intent intent = new Intent(Constants.BROADCAST_ON_INTRUDER_CAPTURED);
            LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent);
        } catch (Exception ignored) {
        }
    }
}
