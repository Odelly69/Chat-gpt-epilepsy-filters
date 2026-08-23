package com.odelly.epilepsysafety;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.view.*;
import android.widget.FrameLayout;

/** Conservative visual mitigation overlay; not a medical device. */
public class SafetyAccessibilityService extends AccessibilityService {
  private View overlay;
  private WindowManager windowManager;
  private boolean triggered;

  @Override protected void onServiceConnected() { super.onServiceConnected(); apply(); }
  @Override public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent e) { apply(); }
  @Override public void onInterrupt() { }

  public synchronized void triggerMaximumMitigation() { triggered = true; apply(); }
  public synchronized void clearTrigger() { triggered = false; apply(); }

  private void apply() {
    boolean enabled = getSharedPreferences("safety", 0).getBoolean("enabled", false);
    if (!enabled) { remove(); return; }
    if (overlay == null) {
      windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
      overlay = new FrameLayout(this);
      int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
          | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
          | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
      WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
          -1, -1, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
          flags, -3);
      windowManager.addView(overlay, lp);
    }
    int dim = getSharedPreferences("safety", 0).getInt("dim", 80);
    if (triggered) dim = Math.max(dim, 95);
    overlay.setBackgroundColor(Color.argb(SafetyProfile.overlayAlphaFromDim(dim), 0, 0, 0));
  }

  private void remove() {
    if (overlay != null && windowManager != null) {
      windowManager.removeView(overlay);
      overlay = null;
      windowManager = null;
    }
  }

  @Override public void onDestroy() { remove(); super.onDestroy(); }
}
