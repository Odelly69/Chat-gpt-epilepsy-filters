package com.odelly.epilepsysafety;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

/** Rootless visual mitigation overlay. Not a medical device. */
public class SafetyAccessibilityService extends AccessibilityService {
  private static volatile SafetyAccessibilityService instance;
  private View overlay;
  private WindowManager windowManager;
  private boolean triggered;

  @Override protected void onServiceConnected() {
    super.onServiceConnected();
    instance = this;
    apply();
  }

  public static boolean isConnected() { return instance != null; }

  public static void requestMaximumMitigation() {
    SafetyAccessibilityService s = instance;
    if (s != null) s.triggerMaximumMitigation();
  }

  public static void clearMaximumMitigation() {
    SafetyAccessibilityService s = instance;
    if (s != null) s.clearTrigger();
  }

  @Override public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent e) { apply(); }
  @Override public void onInterrupt() { }

  public synchronized void triggerMaximumMitigation() {
    triggered = true;
    getSharedPreferences("safety", 0).edit().putBoolean("maximum_mitigation", true).apply();
    apply();
  }

  public synchronized void clearTrigger() {
    triggered = false;
    getSharedPreferences("safety", 0).edit().putBoolean("maximum_mitigation", false).apply();
    apply();
  }

  private void apply() {
    boolean enabled = getSharedPreferences("safety", 0).getBoolean("enabled", false);
    boolean maximum = triggered || getSharedPreferences("safety", 0).getBoolean("maximum_mitigation", false);
    if (!enabled && !maximum) { remove(); return; }

    if (overlay == null) {
      windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
      overlay = new FrameLayout(this);
      int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
          | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
          | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
          | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
      WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
          -1, -1, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
          flags, -3);
      windowManager.addView(overlay, lp);
    }

    int dim = getSharedPreferences("safety", 0).getInt("dim", 80);
    // Maximum mode intentionally blocks the visual stream instead of merely dimming it.
    // This is a user-selected emergency mitigation, not a guarantee of seizure prevention.
    int alpha = maximum ? 255 : SafetyProfile.overlayAlphaFromDim(dim);
    overlay.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
  }

  private void remove() {
    if (overlay != null && windowManager != null) {
      try { windowManager.removeView(overlay); } catch (RuntimeException ignored) { }
      overlay = null;
      windowManager = null;
    }
  }

  @Override public void onDestroy() {
    if (instance == this) instance = null;
    remove();
    super.onDestroy();
  }
}
