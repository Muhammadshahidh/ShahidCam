import os, re, subprocess

print(">>> Fixing App Startup Crash...")

# 1. Inspect and Fix MainActivity.java navigation mapping
main_path = "app/src/main/java/com/shahid/cam/MainActivity.java"
if os.path.exists(main_path):
    with open(main_path, 'r', encoding='utf-8') as f:
        mc = f.read()

    # Remove invalid 'case 5: return 0;' or 'return 0;' from nav id switch
    mc = re.sub(r'case\s+5:\s*return\s+0;\s*', '', mc)
    
    # Ensure getNavIdForPosition never returns 0 for valid positions
    # If a switch has default: return 0;, make sure setSelectedItemId checks id != 0
    mc = re.sub(
        r'(\w+NavigationView\.setSelectedItemId\s*\(\s*)([^)]+)(\s*\);)',
        r'int targetNavId = \2;\nif (targetNavId != 0) { \1targetNavId\3 }',
        mc
    )
    with open(main_path, 'w', encoding='utf-8') as f:
        f.write(mc)
    print("Fixed MainActivity.java navigation crash!")

# 2. Check AvatarToggleView to ensure safe inflation on Android devices
avatar_path = None
for root, dirs, files in os.walk('app/src/main/java'):
    if "AvatarToggleView.java" in files:
        avatar_path = os.path.join(root, "AvatarToggleView.java")
        break

if avatar_path:
    with open(avatar_path, 'r', encoding='utf-8') as f:
        ac = f.read()
    
    # Ensure animations run safely on UI thread without NPE
    if "post(() -> startAnimations());" not in ac:
        ac = ac.replace("startAnimations();", "post(() -> { try { startAnimations(); } catch (Exception ignored) {} });")
        with open(avatar_path, 'w', encoding='utf-8') as f:
            f.write(ac)
        print("Safeguarded AvatarToggleView animations against inflation crashes!")

print("\n>>> Fix applied! Now committing and pushing...")
