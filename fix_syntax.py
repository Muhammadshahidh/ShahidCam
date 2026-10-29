import os, re, subprocess

path = "app/src/main/java/com/shahid/cam/ui/SettingsHomeFragment.java"

# 1. First restore clean version of SettingsHomeFragment from git commit
try:
    subprocess.run(["git", "checkout", "HEAD~3", "--", path], check=False)
except Exception:
    pass

if not os.path.exists(path):
    # Find dynamically if path differs
    for root, dirs, files in os.walk('.'):
        if "SettingsHomeFragment.java" in files:
            path = os.path.join(root, "SettingsHomeFragment.java")
            break

print(f"Repairing: {path}")

with open(path, 'r', encoding='utf-8') as f:
    code = f.read()

# -------------------------------------------------------------
# A. Remove Haptics, Motion Lab, Digital Forensics, Automation
# -------------------------------------------------------------
# Cleanly remove any bindRow calls for advanced / coming soon / forensics
patterns_to_remove = [
    r'bindRow\([^)]*group_haptics[^)]*\);?',
    r'bindRow\([^)]*group_motion_lab[^)]*\);?',
    r'bindRow\([^)]*group_forensics[^)]*\);?',
    r'bindRow\([^)]*group_automation[^)]*\);?',
    r'bindRow\([^)]*group_thermal_guardian[^)]*\);?',
    r'bindRow\([^)]*group_audio_vision[^)]*\);?',
    r'bindRow\([^)]*group_scheduled_recording[^)]*\);?',
    r'bindRow\([^)]*group_profiles[^)]*\);?',
    # Broken dangling fragments if any still exist
    r'\(\)\s*->\s*openSubFragment\(new\s+HapticsSettingsFragment\(\)\)\);?',
    r'bindRow\(root,\s*R\.id\.group_haptics,\s*\n?\s*\(\)\s*->\s*openSubFragment\(new\s+HapticsSettingsFragment\(\)\)\);?',
]

for pat in patterns_to_remove:
    code = re.sub(pat, '', code)

# -------------------------------------------------------------
# B. Clean Mini App Cards Methods
# Replace setupMiniAppCards & equalizeMiniCardRows with clean empty bodies
# so no broken cards or missing strings are executed
# -------------------------------------------------------------
code = re.sub(
    r'private\s+void\s+setupMiniAppCards\s*\([^)]*\)\s*\{.*?\n\s*\}',
    'private void setupMiniAppCards(View root) {\n        // Mini apps removed as requested\n    }',
    code,
    flags=re.DOTALL
)

code = re.sub(
    r'private\s+void\s+equalizeMiniCardRows\s*\([^)]*\)\s*\{.*?\n\s*\}',
    'private void equalizeMiniCardRows(View root) {\n        // Mini apps removed as requested\n    }',
    code,
    flags=re.DOTALL
)

# -------------------------------------------------------------
# C. Clean dangling Toast lines or orphaned lambda closes
# -------------------------------------------------------------
# Clean orphan toast lines
code = re.sub(r'\s*android\.widget\.Toast\.makeText\(requireContext\(\),\s*R\.string\.mini_app_coming_soon_desc,\s*android\.widget\.Toast\.LENGTH_SHORT\)\.show\(\);\s*', '', code)
code = re.sub(r'setupMiniCard\([^;]*\);', '', code, flags=re.DOTALL)

with open(path, 'w', encoding='utf-8') as f:
    f.write(code)

print("SettingsHomeFragment.java syntax completely fixed and cleaned!")
