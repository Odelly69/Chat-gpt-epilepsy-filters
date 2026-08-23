# Epilepsy Safety Filter for Android

Root-free Android accessibility prototype for reducing exposure to potentially provocative visual presentation.

## Implemented beta foundation
- User-configurable safety overlay.
- Brightness/dimming control.
- Contrast-risk control in the profile model.
- Motion/animation and vibration-sensitive profile settings.
- Accessibility-service integration.
- Maximum Safety preset.
- No root requirement.
- Medical/safety limitations documentation.

## Build
Install Android SDK and Gradle 8.7, then from this directory run:

`gradle assembleDebug`

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Important limitation
Android does not expose a universal root-free API that lets a third-party app rewrite every other application's rendered pixels, audio stream, or haptic output. This project therefore uses supported accessibility/display mechanisms and must not be marketed as guaranteed seizure prevention.
