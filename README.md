# Chat-gpt-epilepsy-filters

Android epilepsy-safety filter project.

## Android beta
The rootless Android implementation is under `android/EpilepsySafetyFilter/`.

The current beta includes MediaProjection screen monitoring, luminance-based temporal flicker detection, automatic maximum visual mitigation, service watchdog/failure states, configurable profiles, optional microphone loud-pulse mitigation, and optional system touch-haptics reduction.

It is designed as a risk-reduction accessibility tool, not a guarantee against seizures or a medically validated device.

## Validation status
CI validates source compilation, unit tests, and APK artifact creation. CI success is not equivalent to real-device safety validation. The earlier real-world strobe test failed, so real-device functional validation remains an explicit project gate.

See `android/EpilepsySafetyFilter/docs/SAFETY.md` for the safety model and limitations.
