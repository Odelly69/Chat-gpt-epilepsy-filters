# Safety and medical limitations

This project is a risk-reduction accessibility tool, not a medical device and not a guarantee against seizures.

## Trigger model
The design prioritizes flashing/flickering visual content, rapid visual transitions, high-contrast patterns, brightness and contrast. The Epilepsy Foundation reports that visually provoked seizures can be triggered by flashing lights and patterns; 5–30 Hz is a commonly provocative range, but sensitivity varies by person. The app therefore uses conservative user-configurable controls rather than claiming a universal safe threshold.

## Audio and vibration
Some users have individualized sound or vibration sensitivities. Android does not provide an ordinary third-party app with a universal root-free interception point for every other app's audio stream or haptic output. The UI therefore treats these as profiles/settings and must not claim universal suppression.

## Safe-use principles
- Prefer prevention and avoidance of known triggers.
- Provide a prominent emergency safe-display control.
- Never intentionally generate flashing test patterns on a user's device.
- Do not run seizure-trigger testing against a human subject.
- Users with seizure concerns should consult a qualified clinician.
