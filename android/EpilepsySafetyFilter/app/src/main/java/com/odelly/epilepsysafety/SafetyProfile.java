package com.odelly.epilepsysafety;

public final class SafetyProfile {
  private SafetyProfile() {}
  public static boolean isPotentiallyProvocativeFlicker(double hz) { return hz >= 5.0 && hz <= 30.0; }
  public static int overlayAlphaFromDim(int percent) { int p=Math.max(0,Math.min(100,percent)); return Math.min(220,p*2); }
}
