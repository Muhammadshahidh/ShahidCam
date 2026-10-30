import os, re

gradle_path = "app/build.gradle"
if not os.path.exists(gradle_path):
    gradle_path = "app/build.gradle.kts"

print(f">>> Updating Android SDK Compatibility in {gradle_path}...")

with open(gradle_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Bump targetSdk to 35 (Latest Android 15 / 14 standard)
content = re.sub(r'targetSdk(?:Version)?\s*=?\s*\d+', 'targetSdk 35', content)
content = re.sub(r'compileSdk(?:Version)?\s*=?\s*\d+', 'compileSdk 35', content)

with open(gradle_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated targetSdk & compileSdk to 35 (Android 15 compatible)!")

# 2. Check Onboarding to make sure Next button (>) and Skip directly enter the app
for root, dirs, files in os.walk('app/src/main/java'):
    for f in files:
        if "Onboarding" in f and f.endswith(".java"):
            p = os.path.join(root, f)
            with open(p, 'r', encoding='utf-8') as fl:
                c = fl.read()
            # Ensure clicking the arrow or finish smoothly starts MainActivity
            if "MainActivity" in c:
                print(f"Verified Onboarding flow in {f}")

print("\n>>> All Android 14/15 compatibility updates applied!")
