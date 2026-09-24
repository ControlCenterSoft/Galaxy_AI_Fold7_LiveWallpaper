package pro.galaxyai.fold7;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import pro.galaxyai.fold7.ai.RussianAIPersonalityV33;
import pro.galaxyai.fold7.engine.AmbientPersonalityControllerV29;

public class MainActivity extends Activity {
    private TextView overlayStatus;
    private TextView voiceStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Galaxy AI v" + BuildConfig.VERSION_NAME + " — Плавающий помощник");
        title.setTextSize(22f);
        root.addView(title, fullWidth());

        TextView concept = new TextView(this);
        concept.setText("Живые обои больше не используются. Вы выбираете любое обычное фото или фон, а небольшой живой AI-помощник в виде торса девушки находится поверх экрана и приложений без карточки или рамки.");
        concept.setTextSize(15f);
        concept.setPadding(0, dp(10), 0, dp(14));
        root.addView(concept, fullWidth());

        overlayStatus = new TextView(this);
        overlayStatus.setTextSize(15f);
        overlayStatus.setPadding(0, dp(4), 0, dp(10));
        root.addView(overlayStatus, fullWidth());

        Button permission = button("Разрешить отображение поверх приложений");
        permission.setOnClickListener(v -> openOverlayPermission());
        root.addView(permission, buttonParams());

        Button start = button("Запустить AI-помощника");
        start.setOnClickListener(v -> startAssistant());
        root.addView(start, buttonParams());

        Button stop = button("Скрыть AI-помощника");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, FloatingAssistantService.class));
            refreshOverlayStatus();
        });
        root.addView(stop, buttonParams());

        Button wallpaper = button("Выбрать обычное фото / фон");
        wallpaper.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_SET_WALLPAPER));
            } catch (Exception ignored) {
            }
        });
        root.addView(wallpaper, buttonParams());

        TextView sizeTitle = new TextView(this);
        sizeTitle.setText("Размер помощника");
        sizeTitle.setTextSize(16f);
        sizeTitle.setPadding(0, dp(16), 0, dp(6));
        root.addView(sizeTitle, fullWidth());

        Button compact = button("Компактный — 82%");
        compact.setOnClickListener(v -> setAssistantScale(0.82f));
        root.addView(compact, buttonParams());

        Button normal = button("Обычный — 100%");
        normal.setOnClickListener(v -> setAssistantScale(1.0f));
        root.addView(normal, buttonParams());

        Button large = button("Крупный — 122%");
        large.setOnClickListener(v -> setAssistantScale(1.22f));
        root.addView(large, buttonParams());

        TextView behavior = new TextView(this);
        behavior.setText("Перетаскивайте помощника пальцем — после отпускания он мягко привязывается к ближайшему краю. Двойное касание сворачивает или разворачивает его. Размер, сторона, положение и состояние сохраняются. При переходе между внешним и внутренним экраном Fold позиция пересчитывается по безопасной области дисплея. v67 сохраняет естественные микросаккады, координацию глаз и головы, моргание и мягкую русскую TTS-артикуляцию.");
        behavior.setTextSize(14f);
        behavior.setPadding(0, dp(16), 0, dp(12));
        root.addView(behavior, fullWidth());

        TextView language = new TextView(this);
        language.setText("Язык общения AI: Русский (ru-RU). AIDI Gateway и offline/local fallback сохраняют русский язык.");
        language.setTextSize(14f);
        root.addView(language, fullWidth());

        voiceStatus = new TextView(this);
        voiceStatus.setTextSize(15f);
        voiceStatus.setPadding(0, dp(16), 0, dp(8));
        root.addView(voiceStatus, fullWidth());

        Button voiceOn = button("Включить русские голосовые реакции");
        voiceOn.setOnClickListener(v -> {
            AmbientPersonalityControllerV29.setVoiceEnabled(this, true);
            refreshVoiceStatus();
        });
        root.addView(voiceOn, buttonParams());

        Button voiceOff = button("Выключить голосовые реакции");
        voiceOff.setOnClickListener(v -> {
            AmbientPersonalityControllerV29.setVoiceEnabled(this, false);
            refreshVoiceStatus();
        });
        root.addView(voiceOff, buttonParams());

        TextView privacy = new TextView(this);
        privacy.setText("Приватность: для плавающего окна требуется только системное разрешение «Поверх других приложений». Камера, микрофон, точная геолокация и передача исходных медиа не используются. Анимация лица и русская TTS-мимика вычисляются локально.");
        privacy.setTextSize(13f);
        privacy.setPadding(0, dp(18), 0, dp(18));
        root.addView(privacy, fullWidth());

        screen.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        final int baseBottom = dp(6);
        screen.setOnApplyWindowInsetsListener((View v, WindowInsets insets) -> {
            v.setPadding(
                    insets.getSystemWindowInsetLeft(),
                    insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(),
                    Math.max(baseBottom, insets.getSystemWindowInsetBottom()));
            return insets;
        });
        screen.requestApplyInsets();
        setContentView(screen);

        refreshOverlayStatus();
        refreshVoiceStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshOverlayStatus();
        refreshVoiceStatus();
    }

    private void startAssistant() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlayPermission();
            return;
        }
        Intent service = new Intent(this, FloatingAssistantService.class);
        startForegroundService(service);
        refreshOverlayStatus();
    }

    private void setAssistantScale(float scale) {
        if (!Settings.canDrawOverlays(this)) {
            openOverlayPermission();
            return;
        }
        Intent service = new Intent(this, FloatingAssistantService.class);
        service.setAction(FloatingAssistantService.ACTION_SET_SCALE);
        service.putExtra(FloatingAssistantService.EXTRA_SCALE, scale);
        startForegroundService(service);
        refreshOverlayStatus();
    }

    private void openOverlayPermission() {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void refreshOverlayStatus() {
        if (overlayStatus == null) return;
        boolean permission = Settings.canDrawOverlays(this);
        boolean running = FloatingAssistantService.isRunning();
        overlayStatus.setText("Разрешение: " + (permission ? "есть" : "не выдано")
                + " · помощник: " + (running ? "активен" : "остановлен"));
    }

    private void refreshVoiceStatus() {
        if (voiceStatus == null) return;
        boolean enabled = AmbientPersonalityControllerV29.readVoiceEnabled(this);
        int level = AmbientPersonalityControllerV29.readReactionLevel(this);
        voiceStatus.setText("Голос: " + (enabled ? "включён" : "выключен")
                + " · Русский (" + RussianAIPersonalityV33.LOCALE_TAG + ")"
                + " · реакции: " + RussianAIPersonalityV33.reactionLevelName(level));
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        return button;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = fullWidth();
        params.setMargins(0, dp(3), 0, dp(3));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }
}
