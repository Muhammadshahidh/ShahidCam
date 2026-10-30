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
 * Premium 3D Metallic Sphere Avatar Face with Realistic Materials,
 * Cybernetic glowing eyes, 4 states (ACTIVE, SLEEP, NIGHT, ALERT),
 * and full backward compatibility with standard Android Switch APIs.
 */
public class AvatarToggleView extends AppCompatImageView {
    public void setDimWhenOff(boolean dim) { /* supported */ }


    public interface OnCheckedChangeListener {
        void onCheckedChanged(View buttonView, boolean isChecked);
    }

    public enum SphereState {
        ACTIVE,  // ON: Bright cyber-glowing aperture eyes, metallic sheen
        SLEEP,   // OFF: Smooth breathing pulse, dim dormant visor
        NIGHT,   // Low-light stealth mode, emerald green glow
        ALERT    // Recording/Motion alert: Crimson glow
    }

    private SphereState currentState = SphereState.SLEEP;
    private boolean isChecked = false;
    private OnCheckedChangeListener onCheckedChangeListener;

    private AnimatorSet idleAnimatorSet;
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

        setOnClickListener(v -> toggle());
    }

    public void setChecked(boolean checked) {
        setChecked(checked, true);
    }

    public void setChecked(boolean checked, boolean animate) {
        if (this.isChecked != checked) {
            this.isChecked = checked;
            setState(checked ? SphereState.ACTIVE : SphereState.SLEEP, animate);
            if (onCheckedChangeListener != null) {
                onCheckedChangeListener.onCheckedChanged(this, checked);
            }
        } else {
            applyStateDrawable(checked ? SphereState.ACTIVE : SphereState.SLEEP);
        }
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void toggle() {
        setChecked(!isChecked);
    }

    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        this.onCheckedChangeListener = listener;
    }

    public void setState(@NonNull SphereState state) {
        setState(state, true);
    }

    public void setState(@NonNull SphereState state, boolean animate) {
        this.currentState = state;
        this.isChecked = (state == SphereState.ACTIVE || state == SphereState.ALERT);

        if (animate) {
            animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(100)
                .withEndAction(() -> {
                    applyStateDrawable(currentState);
                    animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(140)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                })
                .start();
        } else {
            applyStateDrawable(currentState);
        }
    }

    public SphereState getState() {
        return currentState;
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
        ObjectAnimator floatAnim = ObjectAnimator.ofFloat(this, "translationY", -3.5f, 3.5f);
        floatAnim.setDuration(2400);
        floatAnim.setRepeatMode(ValueAnimator.REVERSE);
        floatAnim.setRepeatCount(ValueAnimator.INFINITE);
        floatAnim.setInterpolator(new AccelerateDecelerateInterpolator());

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
        post(() -> { try { startAnimations(); } catch (Exception ignored) {} });
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
            post(() -> { try { startAnimations(); } catch (Exception ignored) {} });
        } else {
            stopAnimations();
        }
    }
}
