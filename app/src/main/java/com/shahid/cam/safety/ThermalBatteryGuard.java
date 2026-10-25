package com.shahid.cam.safety;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import com.shahid.cam.FLog;

public class ThermalBatteryGuard {
    private static final String TAG = "ThermalBatteryGuard";
    public static final int TEMP_WARNING_THRESHOLD = 440; // 44.0°C
    public static final int TEMP_CRITICAL_THRESHOLD = 480; // 48.0°C

    public interface ThermalListener {
        void onThermalWarning(float currentTempC);
        void onThermalCritical(float currentTempC);
    }

    private final Context context;
    private final ThermalListener listener;
    private BroadcastReceiver batteryReceiver;
    private boolean isRegistered = false;

    public ThermalBatteryGuard(Context context, ThermalListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public synchronized void startMonitoring() {
        if (isRegistered) return;
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent == null || !Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) return;
                int rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                float tempC = rawTemp / 10.0f;

                if (rawTemp >= TEMP_CRITICAL_THRESHOLD) {
                    FLog.e(TAG, "CRITICAL BATTERY TEMPERATURE: " + tempC + "°C");
                    if (listener != null) listener.onThermalCritical(tempC);
                } else if (rawTemp >= TEMP_WARNING_THRESHOLD) {
                    FLog.w(TAG, "High battery temperature warning: " + tempC + "°C");
                    if (listener != null) listener.onThermalWarning(tempC);
                }
            }
        };
        context.registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        isRegistered = true;
        FLog.i(TAG, "Thermal battery monitoring active.");
    }

    public synchronized void stopMonitoring() {
        if (!isRegistered || batteryReceiver == null) return;
        try { context.unregisterReceiver(batteryReceiver); } catch (Exception ignored) {}
        isRegistered = false;
        batteryReceiver = null;
        FLog.i(TAG, "Thermal battery monitoring stopped.");
    }

    public boolean isMonitoring() { return isRegistered; }
}
