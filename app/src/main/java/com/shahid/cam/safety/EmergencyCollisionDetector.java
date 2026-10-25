package com.shahid.cam.safety;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.shahid.cam.FLog;

public class EmergencyCollisionDetector implements SensorEventListener {
    private static final String TAG = "EmergencyCollisionDetector";
    private static final float STANDARD_GRAVITY = 9.80665f;
    private static final float DEFAULT_TRIGGER_G = 2.8f;
    private static final long COOLDOWN_MS = 30000;

    public interface CollisionListener {
        void onCollisionDetected(float peakGForce);
    }

    private final Context context;
    private final CollisionListener listener;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isRunning = false;
    private long lastTriggerTime = 0;
    private float sensitivityThresholdG = DEFAULT_TRIGGER_G;

    public EmergencyCollisionDetector(Context context, CollisionListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public void setSensitivity(float gThreshold) {
        this.sensitivityThresholdG = Math.max(1.5f, gThreshold);
    }

    public synchronized void start() {
        if (isRunning) return;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) return;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) return;
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        isRunning = true;
        FLog.i(TAG, "Collision detection active (threshold: " + sensitivityThresholdG + "G).");
    }

    public synchronized void stop() {
        if (!isRunning || sensorManager == null) return;
        sensorManager.unregisterListener(this);
        isRunning = false;
        FLog.i(TAG, "Collision detection stopped.");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        double magnitude = Math.sqrt(x * x + y * y + z * z);
        float currentG = (float) (magnitude / STANDARD_GRAVITY);

        long now = System.currentTimeMillis();
        if (currentG >= sensitivityThresholdG && (now - lastTriggerTime > COOLDOWN_MS)) {
            lastTriggerTime = now;
            FLog.w(TAG, "INCIDENT IMPACT DETECTED! Peak G-Force: " + currentG + "G");
            if (listener != null) listener.onCollisionDetected(currentG);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
