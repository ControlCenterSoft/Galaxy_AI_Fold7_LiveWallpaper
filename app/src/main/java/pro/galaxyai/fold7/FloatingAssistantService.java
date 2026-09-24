package pro.galaxyai.fold7;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;

public final class FloatingAssistantService extends Service {
    private static final String CHANNEL_ID = "galaxy_ai_overlay";
    private static final int NOTIFICATION_ID = 6501;
    private static final String PREFS = "floating_assistant_v65";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";

    private static volatile boolean running;

    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private FloatingAssistantViewV65 assistantView;

    private float downRawX;
    private float downRawY;
    private int startX;
    private int startY;
    private boolean dragging;

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
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
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
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

        int width = dp(148);
        int height = dp(222);
        params = new WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(KEY_X, Math.max(0, getResources().getDisplayMetrics().widthPixels - width - dp(12)));
        params.y = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(KEY_Y, dp(120));

        assistantView = new FloatingAssistantViewV65(this);
        assistantView.setOnTouchListener((v, event) -> handleTouch(event));
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
                dragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - downRawX;
                float dy = event.getRawY() - downRawY;
                if (!dragging && Math.hypot(dx, dy) > dp(7)) {
                    dragging = true;
                }
                if (dragging) {
                    params.x = startX + Math.round(dx);
                    params.y = startY + Math.round(dy);
                    clampAndApply();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!dragging && event.getActionMasked() == MotionEvent.ACTION_UP) {
                    float nx = (event.getX() / Math.max(1f, assistantView.getWidth())) * 2f - 1f;
                    float ny = (event.getY() / Math.max(1f, assistantView.getHeight())) * 2f - 1f;
                    assistantView.reactToTap(nx, ny);
                }
                persistPosition();
                dragging = false;
                return true;

            default:
                return false;
        }
    }

    private void clampAndApply() {
        if (windowManager == null || params == null || assistantView == null) return;
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        params.x = Math.max(0, Math.min(params.x, Math.max(0, screenW - params.width)));
        params.y = Math.max(0, Math.min(params.y, Math.max(0, screenH - params.height)));
        windowManager.updateViewLayout(assistantView, params);
    }

    private void persistPosition() {
        if (params == null) return;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putInt(KEY_X, params.x)
                .putInt(KEY_Y, params.y)
                .apply();
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
                .setContentText("AI-помощник отображается поверх приложений")
                .setContentIntent(pending)
                .setOngoing(true)
                .build();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
