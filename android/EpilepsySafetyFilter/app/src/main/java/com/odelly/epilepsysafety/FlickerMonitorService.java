package com.odelly.epilepsysafety;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

/**
 * Monitoring service scaffold. A real MediaProjection capture session must be
 * user-authorized before frame sampling can begin. This service provides the
 * lifecycle/watchdog and conservative state model; it never claims that a
 * stream is being monitored when capture is unavailable.
 */
public class FlickerMonitorService extends Service {
  public static final String ACTION_MAXIMUM = "com.odelly.epilepsysafety.MAXIMUM";
  private static final String CHANNEL = "safety_monitor";
  private boolean captureAvailable;
  private long lastHeartbeat;

  @Override public void onCreate() {
    super.onCreate();
    NotificationManager nm = getSystemService(NotificationManager.class);
    nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Safety monitoring", NotificationManager.IMPORTANCE_LOW));
    startForeground(1001, new Notification.Builder(this, CHANNEL)
        .setContentTitle("Epilepsy Safety Filter")
        .setContentText("Monitoring service active; display capture requires authorization")
        .setSmallIcon(android.R.drawable.ic_lock_idle_lock).build());
    lastHeartbeat = SystemClock.elapsedRealtime();
  }

  public void setCaptureAvailable(boolean available) { captureAvailable = available; lastHeartbeat = SystemClock.elapsedRealtime(); }
  public boolean isCaptureAvailable() { return captureAvailable; }
  public long getLastHeartbeat() { return lastHeartbeat; }
  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (ACTION_MAXIMUM.equals(intent == null ? null : intent.getAction())) {
      getSharedPreferences("safety", 0).edit().putBoolean("maximum_mitigation", true).apply();
    }
    lastHeartbeat = SystemClock.elapsedRealtime();
    return START_STICKY;
  }
  @Override public IBinder onBind(Intent intent) { return null; }
}
