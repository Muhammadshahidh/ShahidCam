package com.shahid.cam.security;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinates Wrong Password / PIN -> Front Camera Intruder Selfie capture.
 *
 * Uses a 2-stage Camera2 capture (brief repeating preview warmup to let 3A
 * auto-exposure / white-balance converge, followed by STILL_CAPTURE to ImageReader)
 * with single-surface fallback for legacy HALs, upright EXIF orientation normalization,
 * duplicate cooldown protection, and strict private on-device storage.
 */
public final class IntruderManager {
    private static final String TAG = "IntruderManager";
    private static final long MIN_CAPTURE_INTERVAL_MS = 2500L;
    private static final long CAMERA_TIMEOUT_MS = 7500L;
    private static final long MAX_WARMUP_MS = 500L;
    private static final int MIN_WARMUP_FRAMES = 4;

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
     * Safe to invoke from any thread. Guarantees at most one capture per failed attempt.
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
        final SurfaceTexture[] openTexture = new SurfaceTexture[1];
        final Surface[] openPreviewSurface = new Surface[1];
        final AtomicBoolean finished = new AtomicBoolean(false);
        final AtomicBoolean stillSent = new AtomicBoolean(false);

        final Runnable cleanup = () -> {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            try {
                if (openSession[0] != null) {
                    try {
                        openSession[0].stopRepeating();
                    } catch (Exception ignored) {
                    }
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
                if (openPreviewSurface[0] != null) {
                    try {
                        openPreviewSurface[0].release();
                    } catch (Exception ignored) {
                    }
                    openPreviewSurface[0] = null;
                }
                if (openTexture[0] != null) {
                    try {
                        openTexture[0].release();
                    } catch (Exception ignored) {
                    }
                    openTexture[0] = null;
                }
            } finally {
                cameraThread.quitSafely();
                captureInProgress.set(false);
            }
        };

        final Runnable timeoutRunnable = () -> {
            FLog.w(TAG, "Front camera intruder capture timed out, cleaning up");
            cleanup.run();
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
            Size captureSize = chooseCaptureSize(characteristics);
            Size previewSize = choosePreviewSize(characteristics);
            int jpegOrientation = computeJpegOrientation(characteristics);

            ImageReader reader = ImageReader.newInstance(
                    captureSize.getWidth(),
                    captureSize.getHeight(),
                    ImageFormat.JPEG,
                    2);
            openReader[0] = reader;

            SurfaceTexture dummyTexture = new SurfaceTexture(0);
            dummyTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            openTexture[0] = dummyTexture;
            Surface previewSurface = new Surface(dummyTexture);
            openPreviewSurface[0] = previewSurface;

            reader.setOnImageAvailableListener(imgReader -> {
                if (finished.get()) return;
                try (Image image = imgReader.acquireLatestImage()) {
                    if (image != null && image.getPlanes().length > 0) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] rawBytes = new byte[buffer.remaining()];
                        buffer.get(rawBytes);
                        byte[] finalBytes = normalizeUprightJpeg(rawBytes);
                        IntruderRecord saved = IntruderStorage.saveCapture(appContext, finalBytes, reasonTag);
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
                    startWarmupAndCaptureSession(
                            camera, previewSurface, reader.getSurface(), jpegOrientation,
                            cameraHandler, mainHandler, timeoutRunnable,
                            openSession, finished, stillSent, cleanup);
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
    private void startWarmupAndCaptureSession(
            @NonNull CameraDevice camera,
            @NonNull Surface previewSurface,
            @NonNull Surface readerSurface,
            int jpegOrientation,
            @NonNull Handler cameraHandler,
            @NonNull Handler mainHandler,
            @NonNull Runnable timeoutRunnable,
            @NonNull CameraCaptureSession[] openSession,
            @NonNull AtomicBoolean finished,
            @NonNull AtomicBoolean stillSent,
            @NonNull Runnable cleanup) {
        try {
            camera.createCaptureSession(
                    Arrays.asList(previewSurface, readerSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            openSession[0] = session;
                            if (finished.get()) {
                                cleanup.run();
                                return;
                            }
                            try {
                                CaptureRequest.Builder previewReq =
                                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                previewReq.addTarget(previewSurface);
                                previewReq.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                                previewReq.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                previewReq.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                                previewReq.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);

                                final AtomicInteger frames = new AtomicInteger(0);
                                final Runnable forceStill = () ->
                                        fireStillCapture(camera, session, readerSurface, jpegOrientation,
                                                cameraHandler, mainHandler, timeoutRunnable, finished, stillSent, cleanup);

                                cameraHandler.postDelayed(forceStill, MAX_WARMUP_MS);
                                session.setRepeatingRequest(previewReq.build(), new CameraCaptureSession.CaptureCallback() {
                                    @Override
                                    public void onCaptureCompleted(@NonNull CameraCaptureSession s,
                                                                   @NonNull CaptureRequest req,
                                                                   @NonNull TotalCaptureResult result) {
                                        if (finished.get() || stillSent.get()) return;
                                        int count = frames.incrementAndGet();
                                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                                        boolean aeReady = aeState == null
                                                || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED
                                                || aeState == CaptureResult.CONTROL_AE_STATE_LOCKED
                                                || aeState == CaptureResult.CONTROL_AE_STATE_INACTIVE;
                                        if (count >= MIN_WARMUP_FRAMES && (aeReady || count >= 8)) {
                                            cameraHandler.removeCallbacks(forceStill);
                                            fireStillCapture(camera, session, readerSurface, jpegOrientation,
                                                    cameraHandler, mainHandler, timeoutRunnable, finished, stillSent, cleanup);
                                        }
                                    }
                                }, cameraHandler);
                            } catch (Exception e) {
                                FLog.w(TAG, "Warmup preview failed, falling back to direct still", e);
                                fireStillCapture(camera, session, readerSurface, jpegOrientation,
                                        cameraHandler, mainHandler, timeoutRunnable, finished, stillSent, cleanup);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            FLog.w(TAG, "Dual-surface session failed; retrying single-surface still session");
                            fallbackSingleSurfaceCapture(camera, readerSurface, jpegOrientation,
                                    cameraHandler, mainHandler, timeoutRunnable, openSession, finished, stillSent, cleanup);
                        }
                    },
                    cameraHandler);
        } catch (Exception e) {
            FLog.e(TAG, "Error configuring dual-surface session", e);
            fallbackSingleSurfaceCapture(camera, readerSurface, jpegOrientation,
                    cameraHandler, mainHandler, timeoutRunnable, openSession, finished, stillSent, cleanup);
        }
    }

    @SuppressWarnings("deprecation")
    private void fallbackSingleSurfaceCapture(
            @NonNull CameraDevice camera,
            @NonNull Surface readerSurface,
            int jpegOrientation,
            @NonNull Handler cameraHandler,
            @NonNull Handler mainHandler,
            @NonNull Runnable timeoutRunnable,
            @NonNull CameraCaptureSession[] openSession,
            @NonNull AtomicBoolean finished,
            @NonNull AtomicBoolean stillSent,
            @NonNull Runnable cleanup) {
        if (finished.get()) return;
        try {
            camera.createCaptureSession(
                    Collections.singletonList(readerSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            openSession[0] = session;
                            fireStillCapture(camera, session, readerSurface, jpegOrientation,
                                    cameraHandler, mainHandler, timeoutRunnable, finished, stillSent, cleanup);
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            FLog.w(TAG, "Single-surface front camera session configuration failed");
                            mainHandler.removeCallbacks(timeoutRunnable);
                            cleanup.run();
                        }
                    },
                    cameraHandler);
        } catch (Exception e) {
            FLog.e(TAG, "Single-surface capture setup error", e);
            mainHandler.removeCallbacks(timeoutRunnable);
            cleanup.run();
        }
    }

    private void fireStillCapture(
            @NonNull CameraDevice camera,
            @NonNull CameraCaptureSession session,
            @NonNull Surface readerSurface,
            int jpegOrientation,
            @NonNull Handler cameraHandler,
            @NonNull Handler mainHandler,
            @NonNull Runnable timeoutRunnable,
            @NonNull AtomicBoolean finished,
            @NonNull AtomicBoolean stillSent,
            @NonNull Runnable cleanup) {
        if (finished.get() || !stillSent.compareAndSet(false, true)) {
            return;
        }
        try {
            try {
                session.stopRepeating();
            } catch (Exception ignored) {
            }
            CaptureRequest.Builder req = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            req.addTarget(readerSurface);
            req.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            req.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            req.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            req.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            req.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation);
            session.capture(req.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession s,
                                               @NonNull CaptureRequest r,
                                               @NonNull TotalCaptureResult result) {
                    // ImageReader callback persists bytes & releases camera
                }
            }, cameraHandler);
        } catch (Exception e) {
            FLog.e(TAG, "Failed capturing intruder still frame", e);
            mainHandler.removeCallbacks(timeoutRunnable);
            cleanup.run();
        }
    }

    @NonNull
    private byte[] normalizeUprightJpeg(@NonNull byte[] rawBytes) {
        try {
            ExifInterface exif = new ExifInterface(new ByteArrayInputStream(rawBytes));
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_NORMAL
                    || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
                return rawBytes;
            }
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90f);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180f);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270f);
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.postScale(-1f, 1f);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.postScale(1f, -1f);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.postRotate(90f);
                    matrix.postScale(-1f, 1f);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.postRotate(270f);
                    matrix.postScale(-1f, 1f);
                    break;
                default:
                    return rawBytes;
            }
            Bitmap decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length);
            if (decoded == null) return rawBytes;
            Bitmap rotated = Bitmap.createBitmap(
                    decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), matrix, true);
            if (rotated != decoded) {
                decoded.recycle();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            rotated.compress(Bitmap.CompressFormat.JPEG, 92, baos);
            rotated.recycle();
            return baos.toByteArray();
        } catch (Exception e) {
            FLog.w(TAG, "EXIF orientation normalization skipped: " + e.getMessage());
            return rawBytes;
        }
    }

    @NonNull
    private Size choosePreviewSize(@NonNull CameraCharacteristics characteristics) {
        Size fallback = new Size(640, 480);
        try {
            StreamConfigurationMap map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) return fallback;
            Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
            if (sizes == null || sizes.length == 0) return fallback;
            Size best = sizes[0];
            int target = 640 * 480;
            int bestDiff = Integer.MAX_VALUE;
            for (Size s : sizes) {
                if (s.getWidth() > 1280 || s.getHeight() > 960) continue;
                int diff = Math.abs((s.getWidth() * s.getHeight()) - target);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    best = s;
                }
            }
            return best;
        } catch (Exception e) {
            return fallback;
        }
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
