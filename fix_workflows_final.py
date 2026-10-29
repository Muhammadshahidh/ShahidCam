import os, glob

# 1. Update primary build.yml with the exact working media3 clone
primary_workflow = """name: Build ShahidCam APK

on:
  push:
    branches: [ master, main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4
        with:
          lfs: true

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Setup local.properties
        run: |
          echo "sdk.dir=$ANDROID_HOME" > local.properties
          echo "KEYSTORE_FILE=" >> local.properties

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Clone Patched Media3
        run: |
          git clone --depth 1 https://github.com/anonfaded/media3-patched.git /tmp/media3-patched
          echo "✓ Patched Media3 cloned to /tmp/media3-patched"

      - name: Build Installable APK
        run: ./gradlew assembleDefaultDebug --no-daemon --stacktrace

      - name: Upload Installable APK
        uses: actions/upload-artifact@v4
        with:
          name: ShahidCam-Installable-APK
          path: app/build/outputs/apk/**/*.apk
          if-no-files-found: error
"""

with open('.github/workflows/build.yml', 'w', encoding='utf-8') as f:
    f.write(primary_workflow)
print("Updated .github/workflows/build.yml with media3-patched!")

# 2. Disable push triggers on other 4 workflows so only ONE build runs instead of 5!
other_workflows = [
    '.github/workflows/build-playstore-aab.yml',
    '.github/workflows/build-pro-plus.yml',
    '.github/workflows/build-pro.yml',
    '.github/workflows/validate-fastlane.yml'
]

manual_only = """name: Manual Secondary Build
on:
  workflow_dispatch:
jobs:
  disabled:
    runs-on: ubuntu-latest
    steps:
      - run: echo "Triggered manually only."
"""

for ow in other_workflows:
    if os.path.exists(ow):
        with open(ow, 'w', encoding='utf-8') as f:
            f.write(manual_only)
        print(f"Disabled auto push trigger on: {ow}")

print("\n>>> All workflows optimized! Only 1 build will run now.")
