package com.odelly.epilepsysafety;

/**
 * Conservative temporal flicker detector. It does not claim medical detection.
 * Samples luminance estimates and requests mitigation after repeated rapid
 * high-contrast transitions in a configurable temporal-frequency band.
 */
public final class FlickerMitigationEngine {
  private final int thresholdPercent;
  private final long minTransitionMs;
  private final long maxTransitionMs;
  private int lastLuma = -1;
  private long lastTransition = 0L;
  private int rapidTransitions = 0;

  public FlickerMitigationEngine(int thresholdPercent, int minHz, int maxHz) {
    this.thresholdPercent = Math.max(1, Math.min(100, thresholdPercent));
    this.minTransitionMs = Math.max(1L, 1000L / Math.max(1, maxHz));
    this.maxTransitionMs = Math.max(minTransitionMs + 1L, 1000L / Math.max(1, minHz));
  }

  public boolean update(int luminance, long nowMs) {
    luminance = Math.max(0, Math.min(255, luminance));
    if (lastLuma < 0) { lastLuma = luminance; lastTransition = nowMs; return false; }
    int delta = Math.abs(luminance - lastLuma);
    int percent = (delta * 100) / 255;
    if (percent >= thresholdPercent) {
      long dt = nowMs - lastTransition;
      if (dt >= minTransitionMs && dt <= maxTransitionMs) rapidTransitions++;
      else rapidTransitions = 0;
      lastTransition = nowMs;
      lastLuma = luminance;
    }
    return rapidTransitions >= 3;
  }

  public void reset() { rapidTransitions = 0; lastLuma = -1; lastTransition = 0L; }
}
