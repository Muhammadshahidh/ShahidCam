import os, re

# ==============================================================
# 1. FIX MainActivity.java (Remove R.id.navigation_lab)
# ==============================================================
main_path = "app/src/main/java/com/shahid/cam/MainActivity.java"
if os.path.exists(main_path):
    with open(main_path, 'r', encoding='utf-8') as f:
        mc = f.read()
    
    # Replace case 5: return R.id.navigation_lab; with safe return 0;
    mc = re.sub(r'case\s+5:\s*return\s+R\.id\.navigation_lab;', 'case 5: return 0;', mc)
    mc = re.sub(r'R\.id\.navigation_lab', '0', mc)
    
    with open(main_path, 'w', encoding='utf-8') as f:
        f.write(mc)
    print("Fixed MainActivity.java navigation_lab reference!")

# ==============================================================
# 2. COMPLETE AvatarToggleView.java (3D Sphere + Switch API)
# ==============================================================
avatar_path = None
for root, dirs, files in os.walk('app/src/main/java'):
    if "AvatarToggleView.java" in files:
        avatar_path = os.path.join(root, "AvatarToggleView.java")
        break

if avatar_path:
    with open(avatar_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    pkg_m = re.search(r'^\s*package\s+([a-zA-Z0-9_.]+);', content, re.MULTILINE)
    pkg = pkg_m.group(1) if pkg_m else "com.shahid.cam.ui"

    avatar_code = f'''package {pkg};

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
public class AvatarToggleView extends AppCompatImageView {{

    public interface OnCheckedChangeListener {{
        void onCheckedChanged(View buttonView, boolean isChecked);
    }}

    public enum SphereState {{
        ACTIVE,  // ON: Bright cyber-glowing aperture eyes, metallic sheen
        SLEEP,   // OFF: Smooth breathing pulse, dim dormant visor
        NIGHT,   // Low-light stealth mode, emerald green glow
        ALERT    // Recording/Motion alert: Crimson glow
    }}

    private SphereState currentState = SphereState.SLEEP;
    private boolean isChecked = false;
    private OnCheckedChangeListener onCheckedChangeListener;

    private AnimatorSet idleAnimatorSet;
    private boolean isAnimating = false;

    public AvatarToggleView(@NonNull Context context) {{
        super(context);
        init();
    }}

    public AvatarToggleView(@NonNull Context context, @Nullable AttributeSet attrs) {{
        super(context, attrs);
        init();
    }}

    public AvatarToggleView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {{
        super(context, attrs, defStyleAttr);
        init();
    }}

    private void init() {{
        setScaleType(ScaleType.FIT_CENTER);
        applyStateDrawable(currentState);
        setupAnimations();

        setOnClickListener(v -> toggle());
    }}

    public void setChecked(boolean checked) {{
        setChecked(checked, true);
    }}

    public void setChecked(boolean checked, boolean animate) {{
        if (this.isChecked != checked) {{
            this.isChecked = checked;
            setState(checked ? SphereState.ACTIVE : SphereState.SLEEP, animate);
            if (onCheckedChangeListener != null) {{
                onCheckedChangeListener.onCheckedChanged(this, checked);
            }}
        }} else {{
            applyStateDrawable(checked ? SphereState.ACTIVE : SphereState.SLEEP);
        }}
    }}

    public boolean isChecked() {{
        return isChecked;
    }}

    public void toggle() {{
        setChecked(!isChecked);
    }}

    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {{
        this.onCheckedChangeListener = listener;
    }}

    public void setState(@NonNull SphereState state) {{
        setState(state, true);
    }}

    public void setState(@NonNull SphereState state, boolean animate) {{
        this.currentState = state;
        this.isChecked = (state == SphereState.ACTIVE || state == SphereState.ALERT);

        if (animate) {{
            animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(100)
                .withEndAction(() -> {{
                    applyStateDrawable(currentState);
                    animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(140)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                }})
                .start();
        }} else {{
            applyStateDrawable(currentState);
        }}
    }}

    public SphereState getState() {{
        return currentState;
    }}

    private void applyStateDrawable(SphereState state) {{
        Context ctx = getContext();
        String resName = "toggle_off";
        switch (state) {{
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
        }}
        int resId = ctx.getResources().getIdentifier(resName, "drawable", ctx.getPackageName());
        if (resId != 0) {{
            setImageResource(resId);
        }}
    }}

    private void setupAnimations() {{
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
    }}

    public void startAnimations() {{
        if (!isAnimating && idleAnimatorSet != null) {{
            idleAnimatorSet.start();
            isAnimating = true;
        }}
    }}

    public void stopAnimations() {{
        if (isAnimating && idleAnimatorSet != null) {{
            idleAnimatorSet.cancel();
            setTranslationY(0f);
            setScaleX(1.0f);
            setScaleY(1.0f);
            isAnimating = false;
        }}
    }}

    @Override
    protected void onAttachedToWindow() {{
        super.onAttachedToWindow();
        startAnimations();
    }}

    @Override
    protected void onDetachedFromWindow() {{
        stopAnimations();
        super.onDetachedFromWindow();
    }}

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {{
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE) {{
            startAnimations();
        }} else {{
            stopAnimations();
        }}
    }}
}}
'''
    with open(avatar_path, 'w', encoding='utf-8') as f:
        f.write(avatar_code)
    print(f"Updated {avatar_path} with full Switch API & 3D metallic sphere face engine!")

