package com.odelly.epilepsysafety;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

/** Optional loud-pulse sentinel. It does not analyze system audio or provide medical detection. */
public class AudioTriggerService extends Service {
  private static final String CHANNEL = "audio_safety";
  private Thread worker;
  private volatile boolean running;

  @Override public void onCreate() {
    super.onCreate();
    NotificationManager nm = getSystemService(NotificationManager.class);
    nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Audio safety", NotificationManager.IMPORTANCE_LOW));
    startForeground(1002, new Notification.Builder(this, CHANNEL)
        .setContentTitle("Epilepsy Safety Filter")
        .setContentText("Audio pulse monitor active")
        .setSmallIcon(android.R.drawable.ic_lock_idle_lock).build());
  }

  @Override public int onStartCommand(android.content.Intent intent, int flags, int startId) {
    if (running) return START_STICKY;
    running = true;
    worker = new Thread(this::monitor, "AudioSafetyMonitor");
    worker.start();
    return START_STICKY;
  }

  private void monitor() {
    int rate = 16000;
    int min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    if (min <= 0) { running = false; return; }
    AudioRecord record = null;
    try {
      record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate,
          AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 4096));
      short[] samples = new short[1024];
      record.startRecording();
      long lastTrigger = 0;
      while (running) {
        int n = record.read(samples, 0, samples.length);
        if (n <= 0) continue;
        long sum = 0;
        for (int i = 0; i < n; i++) sum += (long) samples[i] * samples[i];
        double rms = Math.sqrt((double) sum / n);
        int threshold = getSharedPreferences("safety", 0).getInt("audio_threshold", 12000);
        long now = SystemClock.elapsedRealtime();
        if (rms >= threshold && now - lastTrigger > 1500) {
          lastTrigger = now;
          getSharedPreferences("safety", 0).edit().putBoolean("maximum_mitigation", true)
              .putString("last_trigger", "audio_pulse").putLong("last_trigger_time", System.currentTimeMillis()).apply();
          SafetyAccessibilityService.requestMaximumMitigation();
          AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
          try { am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0); } catch (RuntimeException ignored) { }
        }
      }
    } catch (SecurityException ignored) {
      getSharedPreferences("safety_log", 0).edit().putString("last", "audio_permission_missing").apply();
    } catch (RuntimeException e) {
      getSharedPreferences("safety_log", 0).edit().putString("last", "audio_error").apply();
    } finally {
      if (record != null) { try { record.stop(); } catch (RuntimeException ignored) { } record.release(); }
    }
  }

  @Override public void onDestroy() { running = false; super.onDestroy(); }
  @Override public IBinder onBind(android.content.Intent intent) { return null; }
}
