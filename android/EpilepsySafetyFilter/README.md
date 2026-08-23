# Epilepsy Safety Filter for Android

Rootless Android accessibility and display-monitoring prototype for reducing exposure to potentially provocative visual presentation.

## Current beta implementation
- Configurable visual safety overlay.
- Brightness and contrast reduction profiles.
- Motion/animation and vibration-sensitive profile controls.
- Android Accessibility Service integration.
- MediaProjection authorization and real screen-frame capture.
- Down-sampled luminance sampling without frame persistence.
- Configurable temporal flicker detection with regression tests.
- Automatic maximum-mitigation overlay on detected rapid high-contrast flicker.
- Foreground-service heartbeat/watchdog and explicit capture/service failure states.
- Optional microphone loud-pulse sentinel with media-stream muting.
- Optional system touch-haptics reduction when Android's special settings permission is granted.
- Prominent Maximum Safety Mode.
- No root requirement.
- Safety/medical limitations documentation.

## Build

The GitHub Actions workflow `android-epilepsy-filter.yml` builds the debug APK with Android SDK 35 and Gradle 8.9. From this directory, a local build can be run with:

`gradle test assembleDebug`

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Runtime setup

1. Install the debug APK on a test Android device.
2. Open the app and enable its Accessibility Service.
3. Choose **Authorize Screen Monitoring** and accept Android's MediaProjection consent dialog.
4. Confirm the app reports both accessibility and screen-capture authorization as available.
5. Use Maximum Safety Mode when an immediate visual shutdown is desired.
6. Enable the optional audio monitor only when microphone-based loud-pulse detection is wanted.
7. Do not test potentially provocative flashing content on a human subject.

## Important limitations

Android does not expose a universal rootless API that lets a third-party app rewrite every other application's rendered pixels, intercept every application's audio, or suppress every haptic/vibration event. This implementation therefore uses supported Accessibility, MediaProjection, audio-recording, and system-settings mechanisms and must not be marketed as guaranteed seizure prevention.

CI success is not real-device validation. The project previously failed a real-world strobe test; that result remains unresolved until controlled, non-human-subject testing demonstrates the intended mitigation behavior on the target device.
