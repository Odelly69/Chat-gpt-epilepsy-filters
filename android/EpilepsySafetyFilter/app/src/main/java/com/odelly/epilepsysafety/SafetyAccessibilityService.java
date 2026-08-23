package com.odelly.epilepsysafety;

import android.accessibilityservice.AccessibilityService;import android.graphics.Color;import android.graphics.drawable.ColorDrawable;import android.view.*;import android.widget.FrameLayout;

public class SafetyAccessibilityService extends AccessibilityService {
  View overlay;
  @Override protected void onServiceConnected(){super.onServiceConnected(); apply();}
  @Override public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent e){apply();}
  @Override public void onInterrupt(){}
  void apply(){if(!getSharedPreferences("safety",0).getBoolean("enabled",false)){remove();return;} if(overlay==null){WindowManager wm=(WindowManager)getSystemService(WINDOW_SERVICE);overlay=new FrameLayout(this);overlay.setBackgroundColor(Color.argb(1,0,0,0));WindowManager.LayoutParams lp=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,-3);wm.addView(overlay,lp);} int dim=getSharedPreferences("safety",0).getInt("dim",80);overlay.setBackgroundColor(Color.argb(Math.min(220,dim*2),0,0,0));}
  void remove(){if(overlay!=null){((WindowManager)getSystemService(WINDOW_SERVICE)).removeView(overlay);overlay=null;}}
  @Override public void onDestroy(){remove();super.onDestroy();}
}
