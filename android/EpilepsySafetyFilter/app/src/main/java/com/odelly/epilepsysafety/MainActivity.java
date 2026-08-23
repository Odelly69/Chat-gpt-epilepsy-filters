package com.odelly.epilepsysafety;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.net.Uri;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {
  static final String PREF = "safety";
  static final int REQUEST_PROJECTION = 401;
  static final int REQUEST_AUDIO = 402;
  static final int REQUEST_NOTIFICATIONS = 403;
  SeekBar dim, contrast, flickerThreshold;
  Switch enabled, vibration, motion, audio;
  TextView status;

  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(32, 32, 32, 32);

    TextView title = new TextView(this); title.setText("Epilepsy Safety Filter"); title.setTextSize(26); root.addView(title);
    TextView info = new TextView(this);
    info.setText("Rootless risk-reduction tool. It cannot guarantee seizure prevention or replace medical advice.");
    root.addView(info);
    status = new TextView(this); root.addView(status);

    enabled = new Switch(this); enabled.setText("Enable safety overlay"); root.addView(enabled);
    dim = bar("Brightness reduction", 80, root);
    contrast = bar("Contrast reduction", 60, root);
    flickerThreshold = bar("Flicker sensitivity", 18, root);
    motion = new Switch(this); motion.setText("Reduce motion / animation where possible"); root.addView(motion);
    vibration = new Switch(this); vibration.setText("Vibration-sensitive profile"); root.addView(vibration);
    audio = new Switch(this); audio.setText("Audio/loud-pulse profile (optional)"); root.addView(audio);

    Button access = new Button(this); access.setText("Open Accessibility Settings");
    access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))); root.addView(access);

    Button capture = new Button(this); capture.setText("Authorize Screen Monitoring");
    capture.setOnClickListener(v -> requestProjection()); root.addView(capture);

    Button audioButton = new Button(this); audioButton.setText("Grant Microphone + Start Audio Monitor");
    audioButton.setOnClickListener(v -> startAudioMonitor()); root.addView(audioButton);

    Button haptics = new Button(this); haptics.setText("Reduce System Touch Haptics");
    haptics.setOnClickListener(v -> reduceSystemHaptics()); root.addView(haptics);

    Button safe = new Button(this); safe.setText("Maximum Safety Mode");
    safe.setOnClickListener(v -> {
      dim.setProgress(95); contrast.setProgress(90); flickerThreshold.setProgress(10);
      motion.setChecked(true); vibration.setChecked(true); enabled.setChecked(true);
      getSharedPreferences(PREF, 0).edit().putBoolean("maximum_mitigation", true).apply();
      SafetyAccessibilityService.requestMaximumMitigation();
      startMonitorWithMaximumMode();
      save(); updateStatus();
    }); root.addView(safe);

    Button clear = new Button(this); clear.setText("Clear Maximum Safety Trigger");
    clear.setOnClickListener(v -> {
      getSharedPreferences(PREF, 0).edit().putBoolean("maximum_mitigation", false).apply();
      SafetyAccessibilityService.clearMaximumMitigation();
      save(); updateStatus();
    }); root.addView(clear);

    enabled.setOnCheckedChangeListener((v, c) -> { save(); updateStatus(); });
    motion.setOnCheckedChangeListener((v, c) -> save());
    vibration.setOnCheckedChangeListener((v, c) -> save());
    audio.setOnCheckedChangeListener((v, c) -> save());
    dim.setOnSeekBarChangeListener(simple());
    contrast.setOnSeekBarChangeListener(simple());
    flickerThreshold.setOnSeekBarChangeListener(simple());
    load();
    setContentView(root);
    requestNotificationPermissionIfNeeded();
    updateStatus();
  }

  private void requestProjection() {
    MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_PROJECTION);
  }

  private void startMonitorWithMaximumMode() {
    Intent i = new Intent(this, FlickerMonitorService.class).setAction(FlickerMonitorService.ACTION_MAXIMUM);
    if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
  }

  private void startAudioMonitor() {
    if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
      return;
    }
    Intent i = new Intent(this, AudioTriggerService.class);
    if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
  }

  private void reduceSystemHaptics() {
    if (!Settings.System.canWrite(this)) {
      Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
      startActivity(i);
      return;
    }
    try {
      Settings.System.putInt(getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 0);
      getSharedPreferences("safety", 0).edit().putBoolean("haptics_reduced", true).apply();
      updateStatus();
    } catch (SecurityException ignored) { }
  }

  private void requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode != REQUEST_PROJECTION) return;
    if (resultCode == RESULT_OK && data != null) {
      Intent service = new Intent(this, FlickerMonitorService.class)
          .putExtra(FlickerMonitorService.EXTRA_RESULT_CODE, resultCode)
          .putExtra(FlickerMonitorService.EXTRA_RESULT_DATA, data);
      if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
      getSharedPreferences(PREF, 0).edit().putBoolean("capture_authorized", true).apply();
    } else {
      getSharedPreferences(PREF, 0).edit().putBoolean("capture_authorized", false).apply();
    }
    updateStatus();
  }

  @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_AUDIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startAudioMonitor();
  }

  SeekBar bar(String label, int value, LinearLayout r) {
    TextView t = new TextView(this); t.setText(label); r.addView(t);
    SeekBar s = new SeekBar(this); s.setMax(100); s.setProgress(value); r.addView(s); return s;
  }

  SeekBar.OnSeekBarChangeListener simple() {
    return new SeekBar.OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar s, int p, boolean f) { save(); }
      public void onStartTrackingTouch(SeekBar s) {}
      public void onStopTrackingTouch(SeekBar s) {}
    };
  }

  void save() {
    if (enabled == null) return;
    getSharedPreferences(PREF, 0).edit()
        .putBoolean("enabled", enabled.isChecked())
        .putInt("dim", dim.getProgress())
        .putInt("contrast", contrast.getProgress())
        .putInt("flicker_threshold", Math.max(5, flickerThreshold.getProgress()))
        .putBoolean("motion", motion.isChecked())
        .putBoolean("vibration", vibration.isChecked())
        .putBoolean("audio", audio.isChecked()).apply();
  }

  void load() {
    SharedPreferences p = getSharedPreferences(PREF, 0);
    enabled.setChecked(p.getBoolean("enabled", false));
    dim.setProgress(p.getInt("dim", 80));
    contrast.setProgress(p.getInt("contrast", 60));
    flickerThreshold.setProgress(p.getInt("flicker_threshold", 18));
    motion.setChecked(p.getBoolean("motion", true));
    vibration.setChecked(p.getBoolean("vibration", false));
    audio.setChecked(p.getBoolean("audio", false));
  }

  void updateStatus() {
    if (status == null) return;
    SharedPreferences p = getSharedPreferences(PREF, 0);
    boolean auth = p.getBoolean("capture_authorized", false);
    boolean max = p.getBoolean("maximum_mitigation", false);
    boolean hapticsReduced = p.getBoolean("haptics_reduced", false);
    status.setText("Accessibility: " + (SafetyAccessibilityService.isConnected() ? "connected" : "not connected")
        + "\nScreen capture authorization: " + (auth ? "granted" : "not granted")
        + "\nMaximum mitigation: " + (max ? "ACTIVE" : "inactive")
        + "\nTouch haptics: " + (hapticsReduced ? "reduced" : "unchanged"));
  }
}
