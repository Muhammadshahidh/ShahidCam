package com.shahid.cam;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.shahid.cam.ui.WatchMainActivity;
import com.shahid.cam.utils.RuntimeCompat;

/**
 * Lightweight DayNight Claymorphism startup router for ShahidCam.
 * Initializes essential preferences on the first frame without artificial blocking delays,
 * plays a crisp camera-lens aperture reveal, and offers retry feedback if startup init fails.
 */
public class SplashActivity extends AppCompatActivity {
    private View loadingContainer;
    private ProgressBar progressBar;
    private View errorContainer;
    private TextView errorText;
    private boolean navigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferencesManager.getInstance(getApplicationContext()).applySavedNightMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        loadingContainer = findViewById(R.id.splash_loading_container);
        progressBar = findViewById(R.id.splash_progress);
        errorContainer = findViewById(R.id.splash_error_container);
        errorText = findViewById(R.id.splash_error_text);

        View halo = findViewById(R.id.splash_halo);
        ImageView iv = findViewById(R.id.splash_image);
        View retryBtn = findViewById(R.id.splash_retry_button);

        DecelerateInterpolator ease = new DecelerateInterpolator();
        if (halo != null) {
            halo.setScaleX(0.84f);
            halo.setScaleY(0.84f);
            halo.setAlpha(0.5f);
            halo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(220).setInterpolator(ease).start();
        }
        if (iv != null) {
            iv.setScaleX(0.86f);
            iv.setScaleY(0.86f);
            iv.setRotation(-10f);
            iv.setAlpha(0.75f);
            iv.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .rotation(0f)
                    .alpha(1f)
                    .setDuration(200)
                    .setInterpolator(ease)
                    .withEndAction(this::performRealStartup)
                    .start();
        } else {
            View root = getWindow() != null ? getWindow().getDecorView() : loadingContainer;
            if (root != null) {
                root.post(this::performRealStartup);
            } else {
                performRealStartup();
            }
        }

        if (retryBtn != null) {
            retryBtn.setOnClickListener(v -> performRealStartup());
        }
    }

    private void performRealStartup() {
        if (navigated || isFinishing() || isDestroyed()) {
            return;
        }
        if (errorContainer != null) errorContainer.setVisibility(View.GONE);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        try {
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
