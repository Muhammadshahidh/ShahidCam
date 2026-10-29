package com.shahid.cam.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Premium 3D Metallic Sphere Avatar Face with realistic materials,
 * cybernetic glowing eyes, dynamic floating/breathing, and 4 states:
 * ACTIVE, SLEEP, NIGHT, ALERT.
 * Battery-optimized: lifecycle auto-pause on screen off/detached.
 */
public class AvatarToggleView extends AppCompatImageView {

    public enum SphereState {
        ACTIVE,  // ON: Bright cyber-glowing aperture eyes, metallic sheen
        SLEEP,   // OFF: Smooth breathing pulse, dim dormant visor
        NIGHT,   // Low-light stealth mode, emerald green low-frequency glow
        ALERT    // Recording/Motion active: Crimson glow with alert pulse
    }

    private SphereState currentState = SphereState.SLEEP;
    private AnimatorSet idleAnimatorSet;
    private ValueAnimator eyeBreathingAnimator;
    private boolean isAnimating = false;

    public AvatarToggleView(@NonNull Context context) {
        super(context);
        init();
    }

    public AvatarToggleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AvatarToggleView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setScaleType(ScaleType.FIT_CENTER);
        applyStateDrawable(currentState);
        setupAnimations();
    }

    public void setState(@NonNull SphereState state) {
        if (this.currentState == state) return;
        this.currentState = state;

        // Smooth state transition with gentle 3D bounce
        animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(120)
            .withEndAction(() -> {
                applyStateDrawable(currentState);
                animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(160)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            })
            .start();
    }

    public SphereState getState() {
        return currentState;
    }

    public void toggleState() {
        if (currentState == SphereState.SLEEP) {
            setState(SphereState.ACTIVE);
        } else {
            setState(SphereState.SLEEP);
        }
    }

    private void applyStateDrawable(SphereState state) {
        Context ctx = getContext();
        String resName = "toggle_off";
        switch (state) {
            case ACTIVE:
                resName = "toggle_on_idle";
                break;
            case NIGHT:
                resName = "toggle_night";
                break;
            case ALERT:
                resName = "toggle_alert";
                break;
            case SLEEP:
            default:
                resName = "toggle_off";
                break;
        }
        int resId = ctx.getResources().getIdentifier(resName, "drawable", ctx.getPackageName());
        if (resId != 0) {
            setImageResource(resId);
        }
    }

    private void setupAnimations() {
        // 1. Idle 3D floating animation (subtle Y-axis levitation)
        ObjectAnimator floatAnim = ObjectAnimator.ofFloat(this, "translationY", -4f, 4f);
        floatAnim.setDuration(2400);
        floatAnim.setRepeatMode(ValueAnimator.REVERSE);
        floatAnim.setRepeatCount(ValueAnimator.INFINITE);
        floatAnim.setInterpolator(new AccelerateDecelerateInterpolator());

        // 2. Subtle organic breathing scale
        ObjectAnimator breathAnimX = ObjectAnimator.ofFloat(this, "scaleX", 0.985f, 1.015f);
        breathAnimX.setDuration(2400);
        breathAnimX.setRepeatMode(ValueAnimator.REVERSE);
        breathAnimX.setRepeatCount(ValueAnimator.INFINITE);
        breathAnimX.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator breathAnimY = ObjectAnimator.ofFloat(this, "scaleY", 0.985f, 1.015f);
        breathAnimY.setDuration(2400);
        breathAnimY.setRepeatMode(ValueAnimator.REVERSE);
        breathAnimY.setRepeatCount(ValueAnimator.INFINITE);
        breathAnimY.setInterpolator(new AccelerateDecelerateInterpolator());

        idleAnimatorSet = new AnimatorSet();
        idleAnimatorSet.playTogether(floatAnim, breathAnimX, breathAnimY);
    }

    public void startAnimations() {
        if (!isAnimating && idleAnimatorSet != null) {
            idleAnimatorSet.start();
            isAnimating = true;
        }
    }

    public void stopAnimations() {
        if (isAnimating && idleAnimatorSet != null) {
            idleAnimatorSet.cancel();
            setTranslationY(0f);
            setScaleX(1.0f);
            setScaleY(1.0f);
            isAnimating = false;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimations();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimations();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE) {
            startAnimations();
        } else {
            stopAnimations();
        }
    }
}
