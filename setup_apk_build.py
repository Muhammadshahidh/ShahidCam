import os, glob, re

workflow_files = glob.glob('.github/workflows/*.yml') + glob.glob('.github/workflows/*.yaml')

if not workflow_files:
    os.makedirs('.github/workflows', exist_ok=True)
    workflow_files = ['.github/workflows/build.yml']

for wf in workflow_files:
    print(f"Configuring workflow for direct APK in: {wf}")
    new_workflow = """name: Build ShahidCam APK & AAB

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

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build Installable APK
        run: ./gradlew assembleDebug --no-daemon --stacktrace

      - name: Upload Installable APK
        uses: actions/upload-artifact@v4
        with:
          name: ShahidCam-Installable-APK
          path: app/build/outputs/apk/debug/*.apk
          if-no-files-found: warn

      - name: Attempt Release AAB
        run: ./gradlew bundleDefaultRelease || ./gradlew bundleRelease || true

      - name: Upload Release AAB (if generated)
        uses: actions/upload-artifact@v4
        with:
          name: ShahidCam-PlayStore-AAB
          path: app/build/outputs/bundle/**/*.aab
          if-no-files-found: ignore
"""
    with open(wf, 'w', encoding='utf-8') as f:
        f.write(new_workflow)
    print("Workflow updated successfully!")

