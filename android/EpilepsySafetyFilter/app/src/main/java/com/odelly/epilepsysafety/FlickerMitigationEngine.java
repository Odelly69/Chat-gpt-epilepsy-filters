package com.odelly.epilepsysafety;

/** Conservative temporal flicker detector. Not a medical detector. */
public final class FlickerMitigationEngine {
  private final int thresholdPercent;
  private final long minTransitionMs;
  private final long maxTransitionMs;
  private final int requiredTransitions;
  private int lastLuma = -1;
  private long lastTransition = -1L;
  private int rapidTransitions = 0;

  public FlickerMitigationEngine(int thresholdPercent, int minHz, int maxHz) {
    this(thresholdPercent, minHz, maxHz, 3);
  }

  public FlickerMitigationEngine(int thresholdPercent, int minHz, int maxHz, int requiredTransitions) {
    this.thresholdPercent = Math.max(1, Math.min(100, thresholdPercent));
    int safeMinHz = Math.max(1, Math.min(120, minHz));
    int safeMaxHz = Math.max(safeMinHz, Math.min(120, maxHz));
    this.minTransitionMs = Math.max(1L, 1000L / safeMaxHz);
    this.maxTransitionMs = Math.max(minTransitionMs + 1L, 1000L / safeMinHz);
    this.requiredTransitions = Math.max(1, requiredTransitions);
  }

  public boolean update(int luminance, long nowMs) {
    luminance = Math.max(0, Math.min(255, luminance));
    if (lastLuma < 0) {
      lastLuma = luminance;
      lastTransition = nowMs;
      return false;
    }

    int delta = Math.abs(luminance - lastLuma);
    int percent = (delta * 100) / 255;
    if (percent >= thresholdPercent) {
      long dt = nowMs - lastTransition;
      if (dt >= minTransitionMs && dt <= maxTransitionMs) {
        rapidTransitions++;
      } else {
        rapidTransitions = 1;
      }
      lastTransition = nowMs;
      lastLuma = luminance;
    }
    return rapidTransitions >= requiredTransitions;
  }

  public int getRapidTransitions() { return rapidTransitions; }
  public void reset() { rapidTransitions = 0; lastLuma = -1; lastTransition = -1L; }
}
