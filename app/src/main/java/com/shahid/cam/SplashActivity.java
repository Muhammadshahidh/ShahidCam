package com.shahid.cam;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.shahid.cam.ui.WatchMainActivity;
import com.shahid.cam.utils.RuntimeCompat;

/**
 * Lightweight startup router for ShahidCam.
 * Initializes essential preferences on the first frame without artificial wait delays,
 * and shows an honest retry state if startup initialization fails.
 */
public class SplashActivity extends Activity {

    private View loadingContainer;
    private ProgressBar progressBar;
    private View errorContainer;
    private TextView errorText;
    private boolean navigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        loadingContainer = findViewById(R.id.splash_loading_container);
        progressBar = findViewById(R.id.splash_progress);
        errorContainer = findViewById(R.id.splash_error_container);
        errorText = findViewById(R.id.splash_error_text);
        ImageView iv = findViewById(R.id.splash_image);
        View retryBtn = findViewById(R.id.splash_retry_button);

        if (iv != null) {
            iv.setScaleX(0.92f);
            iv.setScaleY(0.92f);
            iv.setAlpha(0.85f);
            iv.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(180).start();
        }
        if (retryBtn != null) {
            retryBtn.setOnClickListener(v -> performRealStartup());
        }

        View root = getWindow() != null ? getWindow().getDecorView() : loadingContainer;
        if (root != null) {
            root.post(this::performRealStartup);
        } else {
            performRealStartup();
        }
    }

    private void performRealStartup() {
        if (navigated || isFinishing() || isDestroyed()) {
            return;
        }
        if (errorContainer != null) errorContainer.setVisibility(View.GONE);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        try {
            // Warm up preferences and resolve device form-factor destination immediately.
            SharedPreferencesManager.getInstance(getApplicationContext());
            Class<?> destination = RuntimeCompat.shouldUseWatchUi(SplashActivity.this)
                    ? WatchMainActivity.class
                    : MainActivity.class;
            navigated = true;
            startActivity(new Intent(SplashActivity.this, destination));
            Utils.overridePendingTransitionCompat(this, R.anim.fade_in, R.anim.fade_out);
            finish();
        } catch (Throwable t) {
            FLog.e("SplashActivity", "Startup initialization failed", t);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (errorContainer != null) errorContainer.setVisibility(View.VISIBLE);
            if (errorText != null && t.getMessage() != null) {
                errorText.setText(getString(R.string.splash_init_error));
            }
        }
    }
}
