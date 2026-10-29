import os
import re

print(">>> Searching repository files...")

def find_file(filename):
    for root, dirs, files in os.walk('.'):
        if filename in files:
            path = os.path.join(root, filename)
            print(f"Found {filename} at: {path}")
            return path
    print(f"Warning: {filename} not found in repository.")
    return None

# ==============================================================================
# 1. UPGRADE AVATAR TOGGLE VIEW (3D METALLIC SPHERE ENGINE)
# ==============================================================================
avatar_path = find_file("AvatarToggleView.java")
if avatar_path:
    with open(avatar_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Extract package name
    pkg_match = re.search(r'^\s*package\s+([a-zA-Z0-9_.]+);', content, re.MULTILINE)
    pkg_name = pkg_match.group(1) if pkg_match else "com.fadcam.ui"

    avatar_code = f'''package {pkg_name};

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
public class AvatarToggleView extends AppCompatImageView {{

    public enum SphereState {{
        ACTIVE,  // ON: Bright cyber-glowing aperture eyes, metallic sheen
        SLEEP,   // OFF: Smooth breathing pulse, dim dormant visor
        NIGHT,   // Low-light stealth mode, emerald green low-frequency glow
        ALERT    // Recording/Motion active: Crimson glow with alert pulse
    }}

    private SphereState currentState = SphereState.SLEEP;
    private AnimatorSet idleAnimatorSet;
    private ValueAnimator eyeBreathingAnimator;
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
    }}

    public void setState(@NonNull SphereState state) {{
        if (this.currentState == state) return;
        this.currentState = state;

        // Smooth state transition with gentle 3D bounce
        animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(120)
            .withEndAction(() -> {{
                applyStateDrawable(currentState);
                animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(160)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            }})
            .start();
    }}

    public SphereState getState() {{
        return currentState;
    }}

    public void toggleState() {{
        if (currentState == SphereState.SLEEP) {{
            setState(SphereState.ACTIVE);
        }} else {{
            setState(SphereState.SLEEP);
        }}
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
    print(f"Updated {avatar_path} successfully!")


# ==============================================================================
# 2. REMOVE FORENSIC INTELLIGENCE (LAB) FROM VIEWPAGER ADAPTER
# ==============================================================================
vp_path = find_file("ViewPagerAdapter.java")
if vp_path:
    with open(vp_path, 'r', encoding='utf-8') as f:
        vp_code = f.read()

    # Remove Forensic fragment import and instance
    vp_code = re.sub(r'import\s+[a-zA-Z0-9_.]*ForensicIntelligenceFragment;\s*', '', vp_code)
    vp_code = re.sub(r'import\s+[a-zA-Z0-9_.]*LabFragment;\s*', '', vp_code)

    # Adjust getItemCount or list
    if "return 6;" in vp_code:
        vp_code = vp_code.replace("return 6;", "return 5;")
    
    # Remove case / fragment creation for forensics
    vp_code = re.sub(r'case\s+[2-5]:\s*return\s+new\s+ForensicIntelligenceFragment\(\);', '', vp_code)
    vp_code = re.sub(r'case\s+[2-5]:\s*return\s+new\s+LabFragment\(\);', '', vp_code)

    with open(vp_path, 'w', encoding='utf-8') as f:
        f.write(vp_code)
    print(f"Cleaned {vp_path}!")


# ==============================================================================
# 3. REMOVE LAB / FORENSICS FROM MAINACTIVITY
# ==============================================================================
main_path = find_file("MainActivity.java")
if main_path:
    with open(main_path, 'r', encoding='utf-8') as f:
        main_code = f.read()

    # Remove navigation_lab switch case or listener
    main_code = re.sub(r'case\s+R\.id\.navigation_lab:\s*[^;]*;\s*return\s+true;', '', main_code)
    main_code = re.sub(r'if\s*\([^)]*navigation_lab[^)]*\)\s*\{[^}]*\}', '', main_code)
    main_code = re.sub(r'import\s+[a-zA-Z0-9_.]*ForensicIntelligenceFragment;\s*', '', main_code)

    with open(main_path, 'w', encoding='utf-8') as f:
        f.write(main_code)
    print(f"Cleaned {main_path}!")


# ==============================================================================
# 4. REMOVE MINI APPS, ADVANCED & COMING SOON FROM SETTINGSHOMEFRAGMENT
# ==============================================================================
settings_path = find_file("SettingsHomeFragment.java")
if settings_path:
    with open(settings_path, 'r', encoding='utf-8') as f:
        set_code = f.read()

    unwanted_rows = [
        "torch", "flashlight", "qr_scanner", "sound_meter", "sensor_dashboard",
        "speedometer", "clinometer", "compass", "pedometer", "metal_detector",
        "parking_marker", "qr_generator", "haptic", "motion_lab", "forensic",
        "digital_forensics", "automation", "thermal_guardian", "audio_vision",
        "scheduled_recording", "profiles"
    ]

    lines = set_code.split('\n')
    filtered_lines = []
    skip_block = False
    brace_depth = 0

    for line in lines:
        lower = line.lower()
        if any(bad in lower for bad in unwanted_rows) and ("bindrow" in lower or "setonclicklistener" in lower or "r.id." in lower):
            continue
        filtered_lines.append(line)

    with open(settings_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(filtered_lines))
    print(f"Cleaned {settings_path}!")


# ==============================================================================
# 5. REMOVE MINI APPS AND PROFILES FROM HOMESIDEBARFRAGMENT
# ==============================================================================
sidebar_path = find_file("HomeSidebarFragment.java")
if sidebar_path:
    with open(sidebar_path, 'r', encoding='utf-8') as f:
        side_code = f.read()

    unwanted_sidebar = [
        "mini_apps", "miniapp", "profiles", "forensic", "motion_lab"
    ]

    lines = side_code.split('\n')
    filtered_lines = []
    for line in lines:
        lower = line.lower()
        if any(bad in lower for bad in unwanted_sidebar) and ("setonclicklistener" in lower or "r.id." in lower or "intent" in lower):
            continue
        filtered_lines.append(line)

    with open(sidebar_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(filtered_lines))
    print(f"Cleaned {sidebar_path}!")

print("\n>>> All files updated successfully without UI mockups!")
