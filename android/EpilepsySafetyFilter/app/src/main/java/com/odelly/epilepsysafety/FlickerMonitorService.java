package com.odelly.epilepsysafety;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.nio.ByteBuffer;

/** Real-time screen capture and conservative flicker mitigation pipeline. */
public class FlickerMonitorService extends Service {
  public static final String ACTION_MAXIMUM = "com.odelly.epilepsysafety.MAXIMUM";
  public static final String EXTRA_RESULT_CODE = "result_code";
  public static final String EXTRA_RESULT_DATA = "result_data";
  private static final String CHANNEL = "safety_monitor";
  private static final int NOTIFICATION_ID = 1001;
  private HandlerThread captureThread;
  private Handler captureHandler;
  private Handler watchdogHandler;
  private MediaProjection projection;
  private VirtualDisplay virtualDisplay;
  private ImageReader imageReader;
  private FlickerMitigationEngine detector;
  private volatile boolean captureAvailable;
  private volatile long lastHeartbeat;
  private volatile boolean mitigationTriggered;

  @Override public void onCreate() {
    super.onCreate();
    NotificationManager nm = getSystemService(NotificationManager.class);
    nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Safety monitoring", NotificationManager.IMPORTANCE_LOW));
    startForeground(NOTIFICATION_ID, new Notification.Builder(this, CHANNEL)
        .setContentTitle("Epilepsy Safety Filter")
        .setContentText("Safety monitor active")
        .setSmallIcon(android.R.drawable.ic_lock_idle_lock).build());
    captureThread = new HandlerThread("SafetyFrameCapture"); captureThread.start();
    captureHandler = new Handler(captureThread.getLooper());
    watchdogHandler = new Handler(getMainLooper());
    detector = buildDetector();
    lastHeartbeat = SystemClock.elapsedRealtime();
    watchdogHandler.postDelayed(new Runnable() {
      @Override public void run() {
        long age = SystemClock.elapsedRealtime() - lastHeartbeat;
        if (captureAvailable && age > 3500L) {
          captureAvailable = false;
          log("watchdog_capture_timeout");
          triggerMitigation("watchdog_capture_timeout");
        }
        if (getSharedPreferences("safety", 0).getBoolean("maximum_mitigation", false)
            && !SafetyAccessibilityService.isConnected()) {
          log("watchdog_accessibility_unavailable");
        }
        if (watchdogHandler != null) watchdogHandler.postDelayed(this, 2000L);
      }
    }, 2000L);
  }

  private FlickerMitigationEngine buildDetector() {
    SharedPreferences p = getSharedPreferences("safety", 0);
    int threshold = Math.max(5, Math.min(100, p.getInt("flicker_threshold", 18)));
    int minHz = p.getInt("flicker_min_hz", 5);
    int maxHz = p.getInt("flicker_max_hz", 30);
    int transitions = p.getInt("flicker_transitions", 3);
    return new FlickerMitigationEngine(threshold, minHz, maxHz, transitions);
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    lastHeartbeat = SystemClock.elapsedRealtime();
    if (ACTION_MAXIMUM.equals(intent == null ? null : intent.getAction())) {
      getSharedPreferences("safety", 0).edit().putBoolean("maximum_mitigation", true).apply();
      triggerMitigation("manual_maximum");
    }
    if (intent != null && intent.hasExtra(EXTRA_RESULT_CODE) && intent.hasExtra(EXTRA_RESULT_DATA)) {
      int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
      Intent data = getProjectionIntent(intent);
      if (resultCode != 0 && data != null) startCapture(resultCode, data);
    }
    return START_STICKY;
  }

  private Intent getProjectionIntent(Intent source) {
    if (Build.VERSION.SDK_INT >= 33) return source.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class);
    return source.getParcelableExtra(EXTRA_RESULT_DATA);
  }

  private void startCapture(int resultCode, Intent data) {
    stopCapture();
    try {
      MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
      projection = mpm.getMediaProjection(resultCode, data);
      if (projection == null) throw new IllegalStateException("MediaProjection unavailable");
      projection.registerCallback(new MediaProjection.Callback() {
        @Override public void onStop() {
          captureAvailable = false;
          lastHeartbeat = SystemClock.elapsedRealtime();
          log("projection_stopped");
          releaseCaptureResources();
        }
      }, captureHandler);
      WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
      DisplayMetrics dm = new DisplayMetrics(); wm.getDefaultDisplay().getRealMetrics(dm);
      int width = Math.max(320, Math.min(dm.widthPixels, 1280));
      int height = Math.max(320, Math.min(dm.heightPixels, 1280));
      imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
      imageReader.setOnImageAvailableListener(reader -> sampleLatest(reader), captureHandler);
      virtualDisplay = projection.createVirtualDisplay("EpilepsySafetyFilter", width, height, dm.densityDpi,
          DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, captureHandler);
      captureAvailable = virtualDisplay != null;
      lastHeartbeat = SystemClock.elapsedRealtime();
      log("capture_available=" + captureAvailable);
    } catch (RuntimeException e) {
      captureAvailable = false;
      log("capture_error=" + e.getClass().getSimpleName());
      triggerMitigation("capture_failure");
      stopCapture();
    }
  }

  private void sampleLatest(ImageReader reader) {
    Image image = null;
    try {
      image = reader.acquireLatestImage();
      if (image == null) return;
      int luma = estimateLuminance(image);
      long now = SystemClock.elapsedRealtime();
      lastHeartbeat = now;
      if (detector.update(luma, now) && !mitigationTriggered) triggerMitigation("temporal_flicker");
    } catch (RuntimeException e) {
      log("frame_error=" + e.getClass().getSimpleName());
      captureAvailable = false;
      triggerMitigation("frame_failure");
    } finally { if (image != null) image.close(); }
  }

  private int estimateLuminance(Image image) {
    Image.Plane plane = image.getPlanes()[0];
    ByteBuffer buffer = plane.getBuffer();
    int pixelStride = plane.getPixelStride();
    int rowStride = plane.getRowStride();
    int width = image.getWidth(), height = image.getHeight();
    int stepX = Math.max(1, width / 24), stepY = Math.max(1, height / 18);
    long sum = 0; int count = 0;
    for (int y = 0; y < height; y += stepY) {
      int rowBase = y * rowStride;
      for (int x = 0; x < width; x += stepX) {
        int index = rowBase + x * pixelStride;
        if (index < 0 || index + 2 >= buffer.limit()) continue;
        int r = buffer.get(index) & 0xff, g = buffer.get(index + 1) & 0xff, b = buffer.get(index + 2) & 0xff;
        sum += (2126L * r + 7152L * g + 722L * b) / 10000L; count++;
      }
    }
    return count == 0 ? 0 : (int) (sum / count);
  }

  private void triggerMitigation(String reason) {
    mitigationTriggered = true;
    getSharedPreferences("safety", 0).edit().putBoolean("maximum_mitigation", true)
        .putString("last_trigger", reason).putLong("last_trigger_time", System.currentTimeMillis()).apply();
    SafetyAccessibilityService.requestMaximumMitigation();
    log("mitigation=" + reason);
  }

  private void stopCapture() {
    releaseCaptureResources();
    if (projection != null) { try { projection.stop(); } catch (RuntimeException ignored) { } projection = null; }
    captureAvailable = false;
  }

  private void releaseCaptureResources() {
    if (virtualDisplay != null) { try { virtualDisplay.release(); } catch (RuntimeException ignored) { } virtualDisplay = null; }
    if (imageReader != null) { try { imageReader.close(); } catch (RuntimeException ignored) { } imageReader = null; }
  }

  private void log(String message) {
    getSharedPreferences("safety_log", 0).edit().putString("last", message).putLong("time", System.currentTimeMillis()).apply();
  }

  public boolean isCaptureAvailable() { return captureAvailable; }
  public long getLastHeartbeat() { return lastHeartbeat; }

  @Override public void onDestroy() {
    if (watchdogHandler != null) watchdogHandler.removeCallbacksAndMessages(null);
    stopCapture();
    if (captureThread != null) { captureThread.quitSafely(); captureThread = null; }
    super.onDestroy();
  }

  @Override public IBinder onBind(Intent intent) { return null; }
}
