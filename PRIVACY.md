# Privacy Policy for ShahidCam

**Last Updated:** October 26, 2026

ShahidCam ("we", "our", or "the application") is developed as a privacy-focused dashcam, background recording, and security utility. We believe that your privacy is a fundamental right. This Privacy Policy explains our commitment to transparency and data protection.

---

### 1. Zero Data Collection & Zero Telemetry
ShahidCam does **not** collect, harvest, monetize, sell, or transmit any personal identifiable information (PII). The application operates without user accounts, analytics trackers, advertising SDKs, or cloud telemetry.

---

### 2. Device Permissions & How They Are Used
All permissions requested by ShahidCam are strictly utilized on-device to deliver core functionality:

* **Camera (`android.permission.CAMERA`)**: Used exclusively to record video and capture photographs according to user commands (front camera, back camera, and dual camera modes).
* **Microphone (`android.permission.RECORD_AUDIO`)**: Used solely to capture synchronized audio with video recordings. Can be muted at any time in settings.
* **Storage & Media (`READ_MEDIA_VIDEO`)**: Used to store captured recordings locally in device memory and display them in your gallery and records list.
* **Location (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`)**: Used only when Dashcam Mode is active to compute speed and optional geographic coordinates for the on-screen video watermark. Location data is never stored separately or transmitted off-device.
* **Notifications (`POST_NOTIFICATIONS`)**: Used to display persistent foreground service indicators while active recording or streaming is in progress, complying with Android system requirements.
* **Sensors (Accelerometer)**: Used locally by the Emergency Collision Detector to sense sudden vehicular impact and protect the current video file from loop overwrite.

---

### 3. Local Web Streaming & Network Communication
When the local Web Studio feature is activated, ShahidCam hosts an internal HTTP server (`NanoHTTPD`) strictly on your local Wi-Fi / private network. Video streams and controls are transmitted directly point-to-point between your phone and your browser. No video data passes through any third-party cloud servers.

---

### 4. Children’s Privacy
ShahidCam does not address anyone under the age of 13. We do not knowingly collect personal identifiable information from children.

---

### 5. Open Source & Licensing
ShahidCam is an independent open-source application published under the GNU General Public License v3.0 (GPL-3.0). The full source code is publicly accessible for audit and verification.

---

### 6. Contact & Inquiries
If you have any questions or concerns regarding this Privacy Policy, please contact:
* **Developer**: Shahid (Shahid Labs)
* **Email**: cybershadowpk@gmail.com
* **Repository**: https://github.com/CyberShadowPKh/ShahidCam
