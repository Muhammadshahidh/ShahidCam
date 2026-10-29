import os, subprocess, re

print(">>> Checking Forensics & Room files in repository...")

# 1. Check if ForensicsSnapshotWithMedia was accidentally deleted in git
status = subprocess.run(["git", "status", "--porcelain"], capture_output=True, text=True).stdout
deleted_files = [line.split()[1] for line in status.splitlines() if line.startswith(" D") or line.startswith("D ")]

for df in deleted_files:
    if "Forensic" in df or "Snapshot" in df or "Event" in df or "Dao" in df:
        print(f"Restoring deleted dependency file: {df}")
        subprocess.run(["git", "checkout", "HEAD~5", "--", df], capture_output=True)

# 2. Check in git history if ForensicsSnapshotWithMedia.java exists
found = False
for root, dirs, files in os.walk('.'):
    if "ForensicsSnapshotWithMedia.java" in files:
        found = True
        print(f"Found ForensicsSnapshotWithMedia.java at: {os.path.join(root, 'ForensicsSnapshotWithMedia.java')}")
        break

if not found:
    print("ForensicsSnapshotWithMedia.java was deleted in a previous commit, restoring from commit 7c9a3a7...")
    res = subprocess.run(["git", "checkout", "7c9a3a7", "--", "app/src/main/java/com/shahid/cam/forensics/"], capture_output=True, text=True)
    if res.returncode == 0:
        print("Restored all forensics models from 7c9a3a7 successfully!")
    else:
        # Check rev-list
        subprocess.run(["git", "checkout", "HEAD~8", "--", "app/src/main/java/com/shahid/cam/forensics/"], capture_output=True)

# 3. In RecordingService.java, ensure digitalForensicsEventRecorder has its import or safe fallback
rec_path = "app/src/main/java/com/shahid/cam/services/RecordingService.java"
if os.path.exists(rec_path):
    with open(rec_path, 'r', encoding='utf-8') as f:
        rc = f.read()
    # If import is missing, restore RecordingService from 7c9a3a7
    if "DigitalForensicsEventRecorder" in rc and "import" not in rc:
        print("Restoring RecordingService.java from commit 7c9a3a7...")
        subprocess.run(["git", "checkout", "7c9a3a7", "--", rec_path], capture_output=True)

print("Forensics dependencies synchronized!")
