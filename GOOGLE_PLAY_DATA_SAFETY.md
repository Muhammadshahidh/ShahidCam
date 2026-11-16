# ShahidCam — Google Play Release & Data Safety Reference

## Branding & Developer Info
- **App Name:** ShahidCam
- **Developer:** Shahid Hussain
- **Support Email:** mshahidwrites@gmail.com
- **Source Repository:** https://github.com/MuhammadShahidh/ShahidCam
- **License:** GPL-3.0 (with preserved third-party OSS attributions)

## Data Safety Summary (Play Console Form Guidance)
- **Does your app collect or share any required user data types?**
  - **No user account required**, **no analytics/advertising SDKs**, **no off-device upload of videos, audio, photos, or intruder selfies**.
  - All video recordings, audio files, thumbnails, and **Wrong-Password Intruder Selfies** (`filesDir/intruder_selfies/`) remain **100% locally on the user's device**.
  - Users can view, manage, or permanently delete all intruder records at any time from the Intruder History screen.

## Prominent Disclosure Implemented In-App
- **Intruder Detection (`CAMERA`):** Before activation, `SecuritySettingsFragment` displays an explicit disclosure modal explaining front-camera capture on wrong App Lock PIN/password, standard Android camera privacy indicator behavior, and private on-device storage, followed by runtime `CAMERA` permission request and front-camera availability verification.

## Manifest & SDK Targets Checked
- `compileSdk`: 36, `targetSdk`: 35, `minSdk`: 24
- Invalid `WRITE_MEDIA_VIDEO` permission removed.
- Legacy storage permissions scoped (`READ_EXTERNAL_STORAGE` `maxSdkVersion="32"`, `WRITE_EXTERNAL_STORAGE` `maxSdkVersion="29"`).
- `AnnotationService` `specialUse` foreground service includes `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE`.
- Note on signing: Production Google Play submission requires configuring a release upload keystore via `local.properties` (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) in Play App Signing.
- Note on `applicationId`: Preserved existing `com.cybershadowpk.shahidcam` in `app/build.gradle.kts` to maintain upgrade/data compatibility for existing installs (review in Play Console before first store publish).
