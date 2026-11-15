package com.shahid.cam.ui;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.graphics.Color;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.shahid.cam.Constants;
import com.shahid.cam.R;
import com.shahid.cam.SharedPreferencesManager;

public class ImageViewerActivity extends AppCompatActivity {

    private static final int OVERLAY_ANIM_DURATION = 200;

    @Nullable private String itemDisplayName;

    @Nullable
    private View topDock;
    @Nullable
    private View bottomDock;
    private boolean overlayVisible = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferencesManager spm = SharedPreferencesManager.getInstance(this);
        String savedTheme = spm.sharedPreferences.getString(
                Constants.PREF_APP_THEME,
                Constants.DEFAULT_APP_THEME
        );
        if ("Crimson Bloom".equals(savedTheme)) {
            setTheme(R.style.Theme_ShahidCam_Red);
        } else if ("Shahid Dark".equals(savedTheme)) {
            setTheme(R.style.Theme_ShahidCam_Amoled);
        } else {
            setTheme(R.style.Base_Theme_ShahidCam);
        }
        super.onCreate(savedInstanceState);
        enableEdgeToEdge();
        setContentView(R.layout.activity_image_viewer);

        ImageView back = findViewById(R.id.imageViewerBack);
        ImageView more = findViewById(R.id.imageViewerMore);
        ZoomableImageView imageView = findViewById(R.id.imageViewerImage);
        TextView metaView = findViewById(R.id.imageViewerMeta);
        topDock = findViewById(R.id.imageViewerTopDock);
        bottomDock = findViewById(R.id.imageViewerBottomDock);
        applySystemInsets();
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }
        if (imageView != null) {
            imageView.setOnSingleTapListener(this::toggleOverlay);
        }

        Uri uri = getIntent() != null ? getIntent().getData() : null;
        if (uri == null) {
            Toast.makeText(this, R.string.photo_capture_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        itemDisplayName = getIntent().getStringExtra("item_display_name");

        if (metaView != null) {
            String fallbackName = !TextUtils.isEmpty(itemDisplayName)
                    ? itemDisplayName
                    : (uri.getLastPathSegment() != null ? uri.getLastPathSegment() : "");
            metaView.setText(fallbackName);
        }
        if (more != null) {
            more.setOnClickListener(v -> showInfoSheet(uri));
        }

        Glide.with(this)
                .load(uri)
                .error(R.drawable.ic_video_placeholder)
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        Toast.makeText(ImageViewerActivity.this, R.string.photo_capture_failed, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(imageView);
    }

    /**
     * Toggles the visibility of the top and bottom overlay docks with a fade animation.
     * Called on single-tap of the image.
     */
    private void toggleOverlay() {
        overlayVisible = !overlayVisible;
        if (overlayVisible) {
            showDock(topDock);
            showDock(bottomDock);
        } else {
            hideDock(topDock);
            hideDock(bottomDock);
        }
    }

    private void showDock(@Nullable View dock) {
        if (dock == null) return;
        dock.setVisibility(View.VISIBLE);
        dock.setAlpha(0f);
        dock.animate()
                .alpha(1f)
                .setDuration(OVERLAY_ANIM_DURATION)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(null)
                .start();
    }

    private void hideDock(@Nullable View dock) {
        if (dock == null) return;
        dock.animate()
                .alpha(0f)
                .setDuration(OVERLAY_ANIM_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> dock.setVisibility(View.GONE))
                .start();
    }

    private void enableEdgeToEdge() {
        Window window = getWindow();
        if (window == null) return;
        WindowCompat.setDecorFitsSystemWindows(window, false);
        com.shahid.cam.util.SystemBarUtil.setStatusBarColor(window, Color.TRANSPARENT);
        com.shahid.cam.util.SystemBarUtil.setNavigationBarColor(window, Color.TRANSPARENT);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }
    }

    private void applySystemInsets() {
        final View root = findViewById(android.R.id.content);
        if (root == null) {
            return;
        }
        final View top = topDock;
        final View bottom = bottomDock;
        final int topBaseTopMargin = getTopMargin(top);
        final int topBaseStartMargin = getStartMargin(top);
        final int topBaseEndMargin = getEndMargin(top);
        final int bottomBaseBottomMargin = getBottomMargin(bottom);
        final int bottomBaseStartMargin = getStartMargin(bottom);
        final int bottomBaseEndMargin = getEndMargin(bottom);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (top != null && top.getLayoutParams() instanceof android.widget.FrameLayout.LayoutParams) {
                android.widget.FrameLayout.LayoutParams lp =
                        (android.widget.FrameLayout.LayoutParams) top.getLayoutParams();
                lp.topMargin = topBaseTopMargin + systemBars.top;
                lp.leftMargin = topBaseStartMargin + systemBars.left;
                lp.rightMargin = topBaseEndMargin + systemBars.right;
                top.setLayoutParams(lp);
            }
            if (bottom != null && bottom.getLayoutParams() instanceof android.widget.FrameLayout.LayoutParams) {
                android.widget.FrameLayout.LayoutParams lp =
                        (android.widget.FrameLayout.LayoutParams) bottom.getLayoutParams();
                lp.bottomMargin = bottomBaseBottomMargin + systemBars.bottom;
                lp.leftMargin = bottomBaseStartMargin + systemBars.left;
                lp.rightMargin = bottomBaseEndMargin + systemBars.right;
                bottom.setLayoutParams(lp);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private int getTopMargin(@Nullable View view) {
        if (view == null) return 0;
        if (view.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
            return ((android.view.ViewGroup.MarginLayoutParams) view.getLayoutParams()).topMargin;
        }
        return 0;
    }

    private int getBottomMargin(@Nullable View view) {
        if (view == null) return 0;
        if (view.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
            return ((android.view.ViewGroup.MarginLayoutParams) view.getLayoutParams()).bottomMargin;
        }
        return 0;
    }

    private int getStartMargin(@Nullable View view) {
        if (view == null) return 0;
        if (view.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
            return ((android.view.ViewGroup.MarginLayoutParams) view.getLayoutParams()).leftMargin;
        }
        return 0;
    }

    private int getEndMargin(@Nullable View view) {
        if (view == null) return 0;
        if (view.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
            return ((android.view.ViewGroup.MarginLayoutParams) view.getLayoutParams()).rightMargin;
        }
        return 0;
    }

    @Nullable
    private Uri parseUri(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        try {
            return Uri.parse(value);
        } catch (Throwable t) {
            return null;
        }
    }

    private void showInfoSheet(@NonNull Uri snapshotUri) {
        String name = itemDisplayName != null ? itemDisplayName
                : snapshotUri.getLastPathSegment();
        ScanInfoBottomSheet.newInstance(snapshotUri.toString(), name)
                .show(getSupportFragmentManager(), "scan_info");
    }

}
