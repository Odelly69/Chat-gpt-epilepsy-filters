# Safety and medical limitations

This project is a **risk-reduction accessibility tool**, not a medical device and not a guarantee against seizures. No software-only display overlay should be described as seizure-proof.

## Visual trigger model
The design prioritizes flashing/flickering visual content, rapid luminance transitions, high-contrast patterns, brightness and contrast. A commonly discussed provocative temporal range is approximately 5–30 Hz, but individual sensitivity varies. The detector therefore uses configurable thresholds and profiles instead of claiming a universal safe threshold.

## Real-time screen-monitoring pipeline
The Android beta now has a rootless MediaProjection path:

1. The user explicitly authorizes screen capture through Android's system consent dialog.
2. `FlickerMonitorService` creates a `VirtualDisplay` and receives frames through `ImageReader`.
3. Frames are down-sampled for luminance estimation; no frame is persisted.
4. `FlickerMitigationEngine` evaluates repeated high-contrast temporal transitions.
5. A detected event requests the accessibility overlay's maximum mitigation mode.
6. Maximum mitigation uses an opaque, non-touchable accessibility overlay to block the captured visual stream from reaching the user's view through the app's overlay layer.
7. A watchdog monitors capture heartbeats and records failure states.

The pipeline is intentionally conservative, but capture latency, Android compositor behavior, device-specific restrictions, display refresh behavior, and accessibility-service availability can affect effectiveness.

## Maximum Safety Mode
Maximum Safety Mode is an emergency user-selected profile. It increases visual reduction and can place an opaque black accessibility overlay over the screen. It is deliberately disruptive and is intended to remove visual content rather than preserve normal usability.

If the accessibility service is not connected, the app cannot create this overlay. The monitor records that fail-safe condition; it must not claim that protection is active when it is not.

## Audio / phono
Android does not provide a normal rootless third-party interception point for every other app's audio stream. The optional audio service therefore uses the microphone to detect sustained loud pulses and can mute the device's music stream after a configurable threshold is exceeded. This is **not** a general audio-trigger detector and does not guarantee suppression of sounds produced by another app.

Microphone monitoring is opt-in and requires Android's runtime microphone permission. The service does not store microphone recordings.

## Vibration
Android does not provide a universal rootless listener or suppression API for every vibration/haptic event produced by every app. The vibration profile is therefore a mitigation profile, not universal interception. Where the user grants Android's special modify-system-settings permission, the app can disable system touch haptics. This does not guarantee suppression of notification, alarm, or application-specific vibration.

## Permissions and failure states
The app may request:

- Accessibility-service access for the mitigation overlay.
- MediaProjection consent for real-time screen sampling.
- Microphone permission only when the optional audio monitor is enabled.
- Notification permission where required for visible foreground-service status.
- Modify-system-settings access only when the user chooses the system-haptics reduction control.

If capture permission is denied, the monitor must report capture unavailable. If the accessibility service is unavailable, the app must not report visual mitigation as active. Service heartbeat/watchdog failures are logged as safety faults.

## Validation policy
CI build success proves only that the software compiles and automated tests pass. It does **not** prove real-device flicker mitigation.

The prior real-world strobe test failed with the earlier APK. That failure remains part of the project's validation history. A future APK is not considered functionally validated until it has been exercised on a real Android device using a controlled, non-human-subject test procedure and the observed mitigation behavior has been recorded.

Never intentionally expose a person with epilepsy or seizure susceptibility to a flashing test pattern. Use synthetic recordings, instrumentation, or other non-human test targets for validation.

## Safe-use principles
- Prefer prevention and avoidance of known triggers.
- Keep a prominent maximum-safety control available.
- Never intentionally generate flashing test patterns on a user's device for human testing.
- Do not run seizure-trigger testing against a human subject.
- Treat all automated detection as best-effort risk reduction.
- Consult a qualified clinician for medical guidance.
