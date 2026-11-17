package com.shahid.cam.ui.components;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.shahid.cam.Constants;
import com.shahid.cam.FLog;
import com.shahid.cam.R;
import com.shahid.cam.SharedPreferencesManager;

/**
 * Component for handling the 2-mode switcher (ShahidCam camera & ScreenRec).
 */
public class ModeSwitcherComponent {
    private static final String TAG = "ModeSwitcherComponent";
    private static final int ANIMATION_DURATION = 180;
    private static final float[] cachedSegmentLefts = new float[2];
    private static float cachedSegmentWidth = 0f;

    private final Context context;
    private final SharedPreferencesManager sharedPreferencesManager;
    private ModeSwitcherListener listener;

    private FrameLayout switcherRoot;
    private FrameLayout segmentShahidCam;
    private FrameLayout segmentShahidRec;
    private LinearLayout segmentsContainer;
    private View activeIndicator;
    private ViewPropertyAnimator activeIndicatorAnimator;

    private final android.os.Handler dragHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable dragLongPressRunnable;
    private boolean dragActive = false;
    private float dragDownX = 0f;
    private float dragDownY = 0f;
    private float dragBaseX = 0f;
    private int dragHighlightedSegment = 0;

    private String currentMode = Constants.MODE_SHAHIDCAM;
    private boolean isInitializing = true;

    public interface ModeSwitcherListener {
        void onModeSelected(String mode);
        void onComingSoonRequested(String modeName);
    }

    public ModeSwitcherComponent(Context context) {
        this.context = context;
        this.sharedPreferencesManager = SharedPreferencesManager.getInstance(context);
    }

    public void initialize(View rootView) {
        try {
            segmentShahidCam = rootView.findViewById(R.id.segment_shahidcam);
            segmentShahidRec = rootView.findViewById(R.id.segment_shahidrec);
            switcherRoot = rootView.findViewById(R.id.mode_switcher_root);
            activeIndicator = rootView.findViewById(R.id.segment_active_indicator);
            segmentsContainer = rootView.findViewById(R.id.mode_switcher_segments);

            if (switcherRoot == null && activeIndicator != null && activeIndicator.getParent() instanceof FrameLayout) {
                switcherRoot = (FrameLayout) activeIndicator.getParent();
            }
            if (switcherRoot == null && segmentsContainer != null && segmentsContainer.getParent() instanceof FrameLayout) {
                switcherRoot = (FrameLayout) segmentsContainer.getParent();
            }

            if (segmentShahidCam == null || segmentShahidRec == null || switcherRoot == null) {
                FLog.e(TAG, "Segment views missing");
                return;
            }

            switcherRoot.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> {
                updateCachedSegmentGeometry();
                if (!dragActive && !isInitializing && v.getWidth() > 0 && activeIndicatorAnimator == null) {
                    updateActiveIndicator(currentMode, false);
                }
            });

            String persisted = sharedPreferencesManager.getCurrentRecordingMode();
            if (!Constants.MODE_SHAHIDCAM.equals(persisted) && !Constants.MODE_SHAHIDREC.equals(persisted)) {
                persisted = Constants.MODE_SHAHIDCAM;
                sharedPreferencesManager.setCurrentRecordingMode(persisted);
            }
            currentMode = persisted;

            segmentShahidCam.setBackground(null);
            segmentShahidRec.setBackground(null);

            android.widget.TextView tvCam = (android.widget.TextView) segmentShahidCam.getChildAt(0);
            android.widget.TextView tvRec = (android.widget.TextView) segmentShahidRec.getChildAt(0);

            int activeTextColor = ContextCompat.getColor(context, android.R.color.white);
            int inactiveTextColor = ContextCompat.getColor(context, R.color.clay_text_secondary);

            if (tvCam != null) {
                boolean active = Constants.MODE_SHAHIDCAM.equals(currentMode);
                tvCam.setTextColor(active ? activeTextColor : inactiveTextColor);
                tvCam.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
            if (tvRec != null) {
                boolean active = Constants.MODE_SHAHIDREC.equals(currentMode);
                tvRec.setTextColor(active ? activeTextColor : inactiveTextColor);
                tvRec.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }

            rootView.post(() -> {
                setExclusiveSelected(currentMode, null);
                isInitializing = false;
            });

            setupClickListeners();
            FLog.d(TAG, "ModeSwitcher initialized (active=" + currentMode + ")");
        } catch (Exception e) {
            FLog.e(TAG, "Error initializing ModeSwitcher", e);
            isInitializing = false;
        }
    }

    private void setExclusiveSelected(String mode, Runnable onTransitionComplete) {
        String previousMode = currentMode;
        boolean shouldAnimateTransition = !isInitializing && previousMode != null && !previousMode.equals(mode);

        if (shouldAnimateTransition) {
            animatePillFlow(previousMode, mode, onTransitionComplete);
        } else {
            updateActiveIndicator(mode, false, null);
            if (onTransitionComplete != null) {
                onTransitionComplete.run();
            }
        }

        if (!isInitializing) {
            if (Constants.MODE_SHAHIDCAM.equals(previousMode) && !Constants.MODE_SHAHIDCAM.equals(mode)) {
                styleText(segmentShahidCam, false, false);
            } else if (Constants.MODE_SHAHIDREC.equals(previousMode) && !Constants.MODE_SHAHIDREC.equals(mode)) {
                styleText(segmentShahidRec, false, false);
            }
            if (Constants.MODE_SHAHIDCAM.equals(mode)) {
                styleText(segmentShahidCam, true, true);
            } else if (Constants.MODE_SHAHIDREC.equals(mode)) {
                styleText(segmentShahidRec, true, true);
            }
        }
    }

    private void applyTextStatesInstant(String mode) {
        styleText(segmentShahidCam, Constants.MODE_SHAHIDCAM.equals(mode), false);
        styleText(segmentShahidRec, Constants.MODE_SHAHIDREC.equals(mode), false);
    }

    private void updateActiveIndicator(String mode, boolean animate) {
        updateActiveIndicator(mode, animate, null);
    }

    private void updateActiveIndicator(String mode, boolean animate, Runnable onAnimationComplete) {
        if (activeIndicator == null || switcherRoot == null) return;
        FrameLayout target = getSegmentForMode(mode);
        if (target == null) return;

        Runnable apply = () -> {
            int targetLeft = switcherRoot.getPaddingLeft() + target.getLeft();
            int targetTop = 0;
            int targetWidth = target.getWidth();
            int resolvedTargetHeight = switcherRoot.getHeight();
            if (resolvedTargetHeight <= 0) {
                resolvedTargetHeight = target.getHeight();
            }
            final int targetHeight = resolvedTargetHeight;
            if (targetWidth <= 0) {
                int segIndex = segmentIndexForMode(mode);
                targetWidth = (int) cachedSegmentWidth;
                targetLeft = (int) cachedSegmentLefts[segIndex];
            }
            if (targetWidth <= 0 || targetHeight <= 0) {
                return;
            }
            cancelActiveIndicatorAnimation();
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) activeIndicator.getLayoutParams();
            boolean paramsDirty = lp.gravity != (Gravity.TOP | Gravity.START)
                    || lp.leftMargin != 0 || lp.topMargin != 0
                    || lp.width != targetWidth || lp.height != targetHeight;
            lp.gravity = Gravity.TOP | Gravity.START;
            lp.leftMargin = 0;
            lp.topMargin = 0;
            lp.width = targetWidth;
            lp.height = targetHeight;

            if (activeIndicator.getBackground() == null
                    || activeIndicator.getBackground().getConstantState() == null
                    || !activeIndicator.getBackground().getConstantState().equals(
                            ContextCompat.getDrawable(context, R.drawable.segment_active_background).getConstantState())) {
                activeIndicator.setBackgroundResource(R.drawable.segment_active_background);
            }
            activeIndicator.setVisibility(View.VISIBLE);
            if (paramsDirty) {
                activeIndicator.setLayoutParams(lp);
            }
            if (Math.abs(activeIndicator.getY() - targetTop) > 0.5f) {
                activeIndicator.setY(targetTop);
            }
            if (!animate) {
                if (Math.abs(activeIndicator.getX() - targetLeft) > 0.5f) {
                    activeIndicator.setX(targetLeft);
                }
                if (onAnimationComplete != null) {
                    onAnimationComplete.run();
                }
                return;
            }

            final float finalLeft = targetLeft;
            unclipHierarchy(activeIndicator);
            boolean alreadyScaled = activeIndicator.getScaleX() > 1.05f;
            if (alreadyScaled) {
                activeIndicatorAnimator = activeIndicator.animate()
                        .x(targetLeft)
                        .setDuration(ANIMATION_DURATION + 70L)
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .withEndAction(() -> {
                            activeIndicatorAnimator = null;
                            activeIndicator.setX(finalLeft);
                            activeIndicator.animate().scaleX(1f).scaleY(1f)
                                    .setDuration(220)
                                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                                    .withEndAction(() -> {
                                        if (onAnimationComplete != null) {
                                            onAnimationComplete.run();
                                        }
                                    })
                                    .start();
                        });
            } else {
                activeIndicatorAnimator = activeIndicator.animate()
                        .x(targetLeft)
                        .scaleX(1.10f)
                        .scaleY(1.10f)
                        .setDuration(ANIMATION_DURATION + 70L)
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .withEndAction(() -> {
                            activeIndicatorAnimator = null;
                            activeIndicator.setX(finalLeft);
                            activeIndicator.animate().scaleX(1f).scaleY(1f)
                                    .setDuration(220)
                                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                                    .withEndAction(() -> {
                                        if (onAnimationComplete != null) {
                                            onAnimationComplete.run();
                                        }
                                    })
                                    .start();
                        });
            }
        };

        if (target.getWidth() > 0 && target.getHeight() > 0) {
            apply.run();
        } else {
            switcherRoot.post(apply);
        }
    }

    private void cancelActiveIndicatorAnimation() {
        if (activeIndicatorAnimator != null) {
            activeIndicator.animate().cancel();
            activeIndicatorAnimator = null;
        }
    }

    private void animatePillFlow(String fromMode, String toMode, Runnable onTransitionComplete) {
        if (segmentShahidCam != null) segmentShahidCam.setBackground(null);
        if (segmentShahidRec != null) segmentShahidRec.setBackground(null);
        updateActiveIndicator(toMode, true, () -> {
            if (onTransitionComplete != null) {
                onTransitionComplete.run();
            }
        });
    }

    private void styleText(FrameLayout segment, boolean active, boolean animate) {
        if (segment == null) return;
        android.widget.TextView tv = (android.widget.TextView) segment.getChildAt(0);
        if (tv == null) return;

        int activeColor = ContextCompat.getColor(context, android.R.color.white);
        int inactiveColor = ContextCompat.getColor(context, R.color.clay_text_secondary);
        int targetColor = active ? activeColor : inactiveColor;
        int currentColor = tv.getCurrentTextColor();

        tv.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        if (animate && currentColor != targetColor) {
            animateTextColor(tv, currentColor, targetColor);
        } else {
            tv.setTextColor(targetColor);
        }
    }

    private void setupClickListeners() {
        if (segmentShahidCam != null) {
            segmentShahidCam.setOnClickListener(v -> handleModeClick(Constants.MODE_SHAHIDCAM));
        }
        if (segmentShahidRec != null) {
            segmentShahidRec.setOnClickListener(v -> handleModeClick(Constants.MODE_SHAHIDREC));
        }
        installPillDragListeners();
    }

    private void handleModeClick(String mode) {
        if (mode.equals(currentMode)) {
            return;
        }
        String previousMode = currentMode;
        currentMode = mode;
        setExclusiveSelected(mode, () -> {
            if (listener != null) {
                listener.onModeSelected(mode);
            }
        });
        sharedPreferencesManager.setCurrentRecordingMode(mode);
        FLog.d(TAG, "Mode switched from " + previousMode + " to " + mode);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void installPillDragListeners() {
        if (switcherRoot == null || activeIndicator == null) return;
        final int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        final long longPressMs = Math.max(ViewConfiguration.getLongPressTimeout(), 300);

        View.OnTouchListener dragListener = (v, ev) -> {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dragDownX = ev.getRawX();
                    dragDownY = ev.getRawY();
                    dragActive = false;
                    updateCachedSegmentGeometry();
                    cancelDragLongPress();
                    dragLongPressRunnable = this::grabPill;
                    dragHandler.postDelayed(dragLongPressRunnable, longPressMs);
                    return false;

                case MotionEvent.ACTION_MOVE: {
                    float dx = ev.getRawX() - dragDownX;
                    float dy = ev.getRawY() - dragDownY;
                    if (!dragActive) {
                        if (Math.hypot(dx, dy) > touchSlop) {
                            cancelDragLongPress();
                        }
                        return false;
                    }
                    float maxX = switcherRoot.getPaddingLeft() + cachedSegmentWidth;
                    float minX = switcherRoot.getPaddingLeft();
                    float clampedX = Math.max(minX, Math.min(maxX, dragBaseX + dx));
                    activeIndicator.setX(clampedX);
                    int hovered = nearestSegment(clampedX);
                    if (hovered != dragHighlightedSegment) {
                        dragHighlightedSegment = hovered;
                        if (com.shahid.cam.Utils.hapticsAllowedForUi(activeIndicator.getContext())) {
                            activeIndicator.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                        }
                    }
                    return true;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    cancelDragLongPress();
                    if (!dragActive) {
                        return false;
                    }
                    boolean canceled = (ev.getActionMasked() == MotionEvent.ACTION_CANCEL);
                    dragActive = false;
                    setPillDragGate(false);
                    int targetSeg = canceled ? segmentIndexForMode(currentMode) : dragHighlightedSegment;
                    String targetMode = modeForSegment(targetSeg);
                    if (!canceled && !targetMode.equals(currentMode)) {
                        String previousMode = currentMode;
                        currentMode = targetMode;
                        styleText(segmentShahidCam, Constants.MODE_SHAHIDCAM.equals(targetMode), true);
                        styleText(segmentShahidRec, Constants.MODE_SHAHIDREC.equals(targetMode), true);
                        sharedPreferencesManager.setCurrentRecordingMode(targetMode);
                        updateActiveIndicator(targetMode, true, () -> {
                            if (listener != null) {
                                listener.onModeSelected(targetMode);
                            }
                        });
                        FLog.d(TAG, "Mode dragged from " + previousMode + " to " + targetMode);
                    } else {
                        settlePillToSegment(segmentIndexForMode(currentMode));
                    }
                    return true;

                default:
                    return false;
            }
        };

        FrameLayout[] segments = { segmentShahidCam, segmentShahidRec };
        for (FrameLayout seg : segments) {
            if (seg != null) seg.setOnTouchListener(dragListener);
        }
    }

    private void grabPill() {
        if (switcherRoot == null || activeIndicator == null || dragActive) return;
        unclipHierarchy(activeIndicator);
        activeIndicator.setPivotX(activeIndicator.getWidth() / 2f);
        activeIndicator.setPivotY(activeIndicator.getHeight() / 2f);
        cancelActiveIndicatorAnimation();
        dragBaseX = activeIndicator.getX();
        dragActive = true;
        dragHighlightedSegment = nearestSegment(dragBaseX);
        activeIndicator.animate().scaleX(1.12f).scaleY(1.12f).setDuration(150).start();
        if (com.shahid.cam.Utils.hapticsAllowedForUi(activeIndicator.getContext())) {
            activeIndicator.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        }
        setPillDragGate(true);
    }

    private void unclipHierarchy(View v) {
        try {
            ViewParent p = v.getParent();
            while (p instanceof android.view.ViewGroup) {
                android.view.ViewGroup g = (android.view.ViewGroup) p;
                g.setClipChildren(false);
                g.setClipToPadding(false);
                p = p.getParent();
            }
        } catch (Exception e) {
            FLog.w(TAG, "unclipHierarchy failed: " + e.getMessage());
        }
    }

    private void cancelDragLongPress() {
        if (dragLongPressRunnable != null) {
            dragHandler.removeCallbacks(dragLongPressRunnable);
            dragLongPressRunnable = null;
        }
    }

    private void setPillDragGate(boolean active) {
        try {
            Context c = switcherRoot != null ? switcherRoot.getContext() : context;
            if (c instanceof com.shahid.cam.MainActivity) {
                ((com.shahid.cam.MainActivity) c).setModePillDragActive(active);
            }
        } catch (Exception e) {
            FLog.w(TAG, "setPillDragGate failed: " + e.getMessage());
        }
    }

    private void updateCachedSegmentGeometry() {
        if (switcherRoot == null) return;
        float rootW = switcherRoot.getWidth() - switcherRoot.getPaddingLeft() - switcherRoot.getPaddingRight();
        if (rootW <= 0f) return;
        cachedSegmentWidth = rootW / 2f;
        float pad = switcherRoot.getPaddingLeft();
        for (int i = 0; i < 2; i++) {
            cachedSegmentLefts[i] = pad + i * cachedSegmentWidth;
        }
    }

    private float segmentCenter(int index) {
        if (cachedSegmentWidth <= 0f) {
            updateCachedSegmentGeometry();
        }
        return cachedSegmentLefts[index] + cachedSegmentWidth / 2f;
    }

    private int nearestSegment(float indicatorLeft) {
        float center = indicatorLeft + (activeIndicator != null ? activeIndicator.getWidth() / 2f : 0f);
        int best = 0;
        float bestDist = Float.MAX_VALUE;
        for (int i = 0; i < 2; i++) {
            float d = Math.abs(center - segmentCenter(i));
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    private String modeForSegment(int index) {
        return index == 1 ? Constants.MODE_SHAHIDREC : Constants.MODE_SHAHIDCAM;
    }

    private int segmentIndexForMode(String mode) {
        return Constants.MODE_SHAHIDREC.equals(mode) ? 1 : 0;
    }

    private void settlePillToSegment(int index) {
        if (activeIndicator == null || switcherRoot == null) return;
        if (cachedSegmentWidth <= 0f) updateCachedSegmentGeometry();
        float targetLeft = cachedSegmentLefts[index];
        cancelActiveIndicatorAnimation();
        activeIndicator.animate()
                .x(targetLeft)
                .scaleX(1f).scaleY(1f)
                .setDuration(200)
                .setInterpolator(new FastOutSlowInInterpolator())
                .withEndAction(() -> {
                    if (activeIndicator != null) activeIndicator.setX(targetLeft);
                })
                .start();
        if (com.shahid.cam.Utils.hapticsAllowedForUi(activeIndicator.getContext())) {
            activeIndicator.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
        }
    }

    private void syncActiveStateInstant(String activeMode) {
        if (activeMode == null) return;
        currentMode = activeMode;
        boolean previousInitializing = isInitializing;
        isInitializing = true;
        setExclusiveSelected(activeMode, null);
        applyTextStatesInstant(activeMode);
        isInitializing = previousInitializing;
    }

    private void animateTextColor(android.widget.TextView textView, int fromColor, int toColor) {
        ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
        colorAnimator.setDuration(ANIMATION_DURATION);
        colorAnimator.addUpdateListener(animator ->
                textView.setTextColor((Integer) animator.getAnimatedValue()));
        colorAnimator.start();
    }

    private FrameLayout getSegmentForMode(String mode) {
        return Constants.MODE_SHAHIDREC.equals(mode) ? segmentShahidRec : segmentShahidCam;
    }

    public void setListener(ModeSwitcherListener listener) {
        this.listener = listener;
    }

    public String getCurrentMode() {
        return currentMode;
    }

    public void setActiveMode(String mode) {
        syncActiveStateInstant(mode);
    }

    public boolean isInitialized() {
        return segmentShahidCam != null && segmentShahidRec != null;
    }
}
