package com.shahid.cam;

import com.shahid.cam.FLog;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.shahid.cam.services.TorchService;

import java.util.Random;

public class TorchToggleActivity extends Activity {

    private static final String TAG = "TorchToggleActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Intent intent = new Intent(this, TorchService.class);
            intent.setAction(Constants.INTENT_ACTION_TOGGLE_TORCH);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception e) {
            FLog.e(TAG, "Error toggling torch", e);
            showTorchErrorToast();
        } finally {
            moveTaskToBack(true);
            finish();
        }
    }

    private void showTorchErrorToast() {
        String[] errorMessages = getResources().getStringArray(R.array.torch_error_messages);
        String humorousMessage = errorMessages[new Random().nextInt(errorMessages.length)];

        new Handler(Looper.getMainLooper()).post(() ->
            Toast.makeText(this, humorousMessage, Toast.LENGTH_LONG).show()
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        moveTaskToBack(true);
    }
}
