package pro.galaxyai.fold7;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

/**
 * v69 Touch Presence overlay service.
 *
 * Keeps v68 persistent scaling, collapse/expand, edge docking, safe-inset clamping and
 * normalized Fold continuity. A deliberate long press now triggers a local visual attention
 * response and, when voice is enabled, a short Russian TTS acknowledgement. No recognition,
 * microphone, camera, precise location or raw-media access is used.
 */
public final class FloatingAssistantService extends Service {
    public static final String ACTION_SET_SCALE = "pro.galaxyai.fold7.action.SET_SCALE";
    public static final String EXTRA_SCALE = "scale";

    private static final String CHANNEL_ID = "galaxy_ai_overlay";
    private static final int NOTIFICATION_ID = 6501;
    private static final String PREFS = "floating_assistant_v65";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_X_FRACTION = "x_fraction";
    private static final String KEY_Y_FRACTION = "y_fraction";
    private static final String KEY_SCALE = "scale";
    private static final String KEY_COLLAPSED = "collapsed";
    private static final String KEY_DOCK = "dock";

    private static final int DOCK_NONE = 0;
    private static final int DOCK_LEFT = -1;
    private static final int DOCK_RIGHT = 1;
    private static final long LONG_PRESS_MS = 560L;

    private static volatile boolean running;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private FloatingAssistantViewV69 assistantView;

    private float downRawX;
    private float downRawY;
    private int startX;
    private int startY;
    private boolean dragging;
    private long downUptime;
    private long lastTapUptime;
    private float scale = 1f;
    private boolean collapsed;
    private int dockSide = DOCK_RIGHT;

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        scale = clamp(prefs.getFloat(KEY_SCALE, 1f), 0.82f, 1.22f);
        collapsed = prefs.getBoolean(KEY_COLLAPSED, false);
        dockSide = prefs.getInt(KEY_DOCK, DOCK_RIGHT);

        startForeground(NOTIFICATION_ID, buildNotification());
        if (Settings.canDrawOverlays(this)) {
            attachOverlay();
        } else {
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (assistantView == null && Settings.canDrawOverlays(this)) {
            attachOverlay();
        }
        if (intent != null && ACTION_SET_SCALE.equals(intent.getAction())) {
            setScale(intent.getFloatExtra(EXTRA_SCALE, 1f));
        }
        return START_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handler.postDelayed(this::restoreForCurrentDisplay, 140L);
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        if (assistantView != null) {
            assistantView.destroy();
            if (windowManager != null) {
                try {
                    windowManager.removeView(assistantView);
                } catch (Exception ignored) {
                }
            }
            assistantView = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean isRunning() {
        return running;
    }

    private void attachOverlay() {
        if (assistantView != null) return;

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            stopSelf();
            return;
        }

        int[] size = currentSize();
        params = new WindowManager.LayoutParams(
                size[0],
                size[1],
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;

        assistantView = new FloatingAssistantViewV69(this);
        assistantView.setOnTouchListener((v, event) -> handleTouch(event));
        restorePositionBeforeAttach();
        windowManager.addView(assistantView, params);
        clampAndApply();
    }

    private boolean handleTouch(MotionEvent event) {
        if (params == null || assistantView == null) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                startX = params.x;
                startY = params.y;
                downUptime = SystemClock.uptimeMillis();
                dragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - downRawX;
                float dy = event.getRawY() - downRawY;
                if (!dragging && Math.hypot(dx, dy) > dp(7)) {
                    dragging = true;
                    dockSide = DOCK_NONE;
                }
                if (dragging) {
                    params.x = startX + Math.round(dx);
                    params.y = startY + Math.round(dy);
                    clampAndApply();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (dragging) {
                    dockToNearestEdge();
                    persistPosition();
                } else {
                    long now = SystemClock.uptimeMillis();
                    long held = Math.max(0L, now - downUptime);
                    float nx = (event.getX() / Math.max(1f, assistantView.getWidth())) * 2f - 1f;
                    float ny = (event.getY() / Math.max(1f, assistantView.getHeight())) * 2f - 1f;

                    if (held >= LONG_PRESS_MS) {
                        lastTapUptime = 0L;
                        assistantView.reactToLongPress(nx, ny);
                    } else if (now - lastTapUptime <= 310L) {
                        lastTapUptime = 0L;
                        toggleCollapsed();
                    } else {
                        lastTapUptime = now;
                        assistantView.reactToTap(nx, ny);
                    }
                }
                dragging = false;
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    persistPosition();
                }
                dragging = false;
                return true;

            default:
                return false;
        }
    }

    private void toggleCollapsed() {
        if (params == null) return;
        collapsed = !collapsed;
        applySizePreservingCenter();
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(KEY_COLLAPSED, collapsed)
                .apply();
    }

    private void setScale(float requested) {
        scale = nearestScale(clamp(requested, 0.82f, 1.22f));
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putFloat(KEY_SCALE, scale)
                .apply();
        if (!collapsed) {
            applySizePreservingCenter();
        }
    }

    private void applySizePreservingCenter() {
        if (params == null || assistantView == null) return;
        int oldW = params.width;
        int oldH = params.height;
        int[] next = currentSize();
        params.x += (oldW - next[0]) / 2;
        params.y += (oldH - next[1]) / 2;
        params.width = next[0];
        params.height = next[1];
        clampAndApply();
        if (dockSide != DOCK_NONE) {
            applyDockSide();
        }
        persistPosition();
    }

    private int[] currentSize() {
        if (collapsed) {
            return new int[] { dp(72), dp(96) };
        }
        return new int[] {
                Math.round(dp(132) * scale),
                Math.round(dp(228) * scale)
        };
    }

    private void dockToNearestEdge() {
        if (params == null) return;
        Rect safe = safeBounds();
        int leftDistance = Math.abs(params.x - safe.left);
        int rightTarget = safe.right - params.width;
        int rightDistance = Math.abs(params.x - rightTarget);
        dockSide = leftDistance <= rightDistance ? DOCK_LEFT : DOCK_RIGHT;
        applyDockSide();
    }

    private void applyDockSide() {
        if (params == null || assistantView == null) return;
        Rect safe = safeBounds();
        if (dockSide == DOCK_LEFT) {
            params.x = safe.left + dp(3);
        } else if (dockSide == DOCK_RIGHT) {
            params.x = safe.right - params.width - dp(3);
        }
        clampAndApply();
    }

    private void restoreForCurrentDisplay() {
        if (params == null || assistantView == null) return;
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        Rect safe = safeBounds();
        float xf = clamp(prefs.getFloat(KEY_X_FRACTION, dockSide == DOCK_LEFT ? 0f : 1f), 0f, 1f);
        float yf = clamp(prefs.getFloat(KEY_Y_FRACTION, 0.20f), 0f, 1f);
        int travelX = Math.max(0, safe.width() - params.width);
        int travelY = Math.max(0, safe.height() - params.height);
        params.x = safe.left + Math.round(travelX * xf);
        params.y = safe.top + Math.round(travelY * yf);
        if (dockSide != DOCK_NONE) {
            applyDockSide();
        } else {
            clampAndApply();
        }
        persistPosition();
    }

    private void restorePositionBeforeAttach() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        Rect safe = safeBounds();
        if (prefs.contains(KEY_X_FRACTION) && prefs.contains(KEY_Y_FRACTION)) {
            float xf = clamp(prefs.getFloat(KEY_X_FRACTION, 1f), 0f, 1f);
            float yf = clamp(prefs.getFloat(KEY_Y_FRACTION, 0.20f), 0f, 1f);
            params.x = safe.left + Math.round(Math.max(0, safe.width() - params.width) * xf);
            params.y = safe.top + Math.round(Math.max(0, safe.height() - params.height) * yf);
        } else {
            params.x = prefs.getInt(KEY_X, Math.max(safe.left,
                    safe.right - params.width - dp(12)));
            params.y = prefs.getInt(KEY_Y, safe.top + dp(120));
        }
    }

    private void clampAndApply() {
        if (windowManager == null || params == null || assistantView == null) return;
        Rect safe = safeBounds();
        params.x = Math.max(safe.left,
                Math.min(params.x, Math.max(safe.left, safe.right - params.width)));
        params.y = Math.max(safe.top,
                Math.min(params.y, Math.max(safe.top, safe.bottom - params.height)));
        windowManager.updateViewLayout(assistantView, params);
    }

    private void persistPosition() {
        if (params == null) return;
        Rect safe = safeBounds();
        int travelX = Math.max(1, safe.width() - params.width);
        int travelY = Math.max(1, safe.height() - params.height);
        float xf = clamp((params.x - safe.left) / (float) travelX, 0f, 1f);
        float yf = clamp((params.y - safe.top) / (float) travelY, 0f, 1f);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putInt(KEY_X, params.x)
                .putInt(KEY_Y, params.y)
                .putFloat(KEY_X_FRACTION, xf)
                .putFloat(KEY_Y_FRACTION, yf)
                .putFloat(KEY_SCALE, scale)
                .putBoolean(KEY_COLLAPSED, collapsed)
                .putInt(KEY_DOCK, dockSide)
                .apply();
    }

    private Rect safeBounds() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
            WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
            Rect bounds = new Rect(metrics.getBounds());
            WindowInsets windowInsets = metrics.getWindowInsets();
            Insets insets = windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            bounds.left += insets.left;
            bounds.top += insets.top;
            bounds.right -= insets.right;
            bounds.bottom -= insets.bottom;
            return bounds;
        }
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        return new Rect(0, 0, screenW, screenH);
    }

    private Notification buildNotification() {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Galaxy AI помощник",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Поддерживает небольшого AI-помощника поверх приложений");
            manager.createNotificationChannel(channel);
        }

        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                this,
                0,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Galaxy AI активен")
                .setContentText("Живой AI-помощник отображается поверх приложений")
                .setContentIntent(pending)
                .setOngoing(true)
                .build();
    }

    private float nearestScale(float value) {
        float[] allowed = {0.82f, 1.0f, 1.22f};
        float best = allowed[0];
        float distance = Math.abs(value - best);
        for (float candidate : allowed) {
            float d = Math.abs(value - candidate);
            if (d < distance) {
                best = candidate;
                distance = d;
            }
        }
        return best;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
