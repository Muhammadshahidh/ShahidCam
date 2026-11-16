# Privacy Policy for ShahidCam

**Last Updated:** 2026
**Developer:** Shahid Hussain

ShahidCam is a privacy-focused mobile camera, screen recorder, and local device security utility. We respect user privacy and operate on a strict local-first architecture.

---

### 1. Zero Telemetry & Zero External Data Collection
ShahidCam does **not** collect, harvest, sell, or transmit your personal files, recordings, or intruder photos to external developer servers. The application operates without ad SDKs or behavioral analytics trackers.

---

### 2. Permissions & On-Device Usage

* **Camera (`android.permission.CAMERA`)**:
  - Used for primary video recording, photo capture, and Dual Camera mode when initiated by the user.
  - Used optionally for **Intruder Selfie Detection**: when explicitly enabled by the user and App Lock detects an incorrect PIN/password attempt, ShahidCam uses the front-facing camera (triggering standard Android camera privacy indicators) to save a single local photo exclusively inside private app storage. No covert background bypassing is performed, and photos are never uploaded off-device.
* **Microphone (`android.permission.RECORD_AUDIO`)**: Used solely for synchronized audio recording or voice recording mode according to user settings.
* **Storage & Media (`READ_MEDIA_VIDEO` / scoped storage)**: Used strictly to save and organize user recordings inside device storage folders or user-selected SAF directories.
* **Location (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`)**: Optional, used only when location watermarking or dashcam features are active to obtain GPS coordinates on-device.
* **Notifications (`POST_NOTIFICATIONS`)**: Displays ongoing foreground service indicators while recording or screen capturing is active.
* **Biometrics / Security (`USE_BIOMETRIC`, `USE_FINGERPRINT`)**: Used for App Lock unlock verification.

---

### 3. Optional Network Requests
Certain optional user-initiated features connect directly to public services:
* **App Update Check**: Reads GitHub release Atom feeds (`github.com/MuhammadShahidh/ShahidCam/releases.atom`) when checking for updates.
* **Weather / Reverse Geocoding (optional watermark metadata)**: Requests Open-Meteo (`api.open-meteo.com`) or OpenStreetMap Nominatim (`nominatim.openstreetmap.org`) only when weather/location metadata options are turned on by the user.

---

### 4. Licensing & Attributions
ShahidCam is distributed under the GNU General Public License v3.0 (GPL-3.0) and incorporates open-source libraries including AndroidX, Material Components, Media3/ExoPlayer, AppIntro, Guardanis AppLock, OSMDroid, and AOSP/Grafika utilities in accordance with their respective licenses.

---

### 5. Contact
* **Developer**: Shahid Hussain
* **Email**: mshahidwrites@gmail.com
* **Repository**: https://github.com/MuhammadShahidh/ShahidCam
