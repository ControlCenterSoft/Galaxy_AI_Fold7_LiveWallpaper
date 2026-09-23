package pro.galaxyai.fold7;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import pro.galaxyai.fold7.ai.AIPersonalizationProfileV30;
import pro.galaxyai.fold7.ai.RussianAIPersonalityV33;
import pro.galaxyai.fold7.engine.AIStatusOverlayRendererV34;
import pro.galaxyai.fold7.engine.AmbientPersonalityControllerV29;
import pro.galaxyai.fold7.engine.AvatarStyleControllerV28;
import pro.galaxyai.fold7.engine.AvatarStyleV28;

public class MainActivity extends Activity {
    private TextView selectedStyle;
    private TextView voiceStatus;
    private TextView profileStatus;
    private TextView contextStatus;
    private AIPersonalizationProfileV30 personalization;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        personalization = new AIPersonalizationProfileV30(this);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(18), dp(12), dp(18), dp(20));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Galaxy AI Fold7 v37 — Живой фотореалистичный AI-персонаж");
        title.setTextSize(21f);
        root.addView(title, fullWidth());

        TextView languageInfo = new TextView(this);
        languageInfo.setText("Язык AI: Русский (ru-RU). Контекстные статусы, голосовые реакции и интерфейс работают на русском языке.");
        languageInfo.setTextSize(14f);
        languageInfo.setPadding(0, dp(8), 0, dp(8));
        root.addView(languageInfo, fullWidth());

        contextStatus = new TextView(this);
        contextStatus.setTextSize(15f);
        contextStatus.setPadding(0, dp(4), 0, dp(6));
        root.addView(contextStatus, fullWidth());
        refreshContextStatus();

        Button contextOn = button("Показывать AI-статусы");
        contextOn.setOnClickListener(v -> {
            AIStatusOverlayRendererV34.setVisible(MainActivity.this, true);
            refreshContextStatus();
        });
        root.addView(contextOn, buttonParams());

        Button contextOff = button("Скрыть AI-статусы");
        contextOff.setOnClickListener(v -> {
            AIStatusOverlayRendererV34.setVisible(MainActivity.this, false);
            refreshContextStatus();
        });
        root.addView(contextOff, buttonParams());

        TextView portraitInfo = new TextView(this);
        portraitInfo.setText("Фотореалистичный AI-персонаж получил естественное моргание, микродвижения взгляда, мягкое дыхание и эмоциональное освещение. Состояние AIDI, эффекты, частицы и переход между экранами Fold сохраняются.");
        portraitInfo.setTextSize(14f);
        portraitInfo.setPadding(0, dp(8), 0, dp(10));
        root.addView(portraitInfo, fullWidth());

        profileStatus = new TextView(this);
        profileStatus.setTextSize(15f);
        profileStatus.setPadding(0, dp(10), 0, dp(6));
        root.addView(profileStatus, fullWidth());
        refreshProfileStatus();

        addProfilePreset(root, "Профиль: Мягкий", AIPersonalizationProfileV30.PRESET_SOFT,
                AvatarStyleV28.HUMAN, AmbientPersonalityControllerV29.LEVEL_QUIET);
        addProfilePreset(root, "Профиль: Сбалансированный", AIPersonalizationProfileV30.PRESET_BALANCED,
                AvatarStyleV28.SCI_FI, AmbientPersonalityControllerV29.LEVEL_NORMAL);
        addProfilePreset(root, "Профиль: Яркий", AIPersonalizationProfileV30.PRESET_VIVID,
                AvatarStyleV28.CUSTOM, AmbientPersonalityControllerV29.LEVEL_EXPRESSIVE);

        Button reset = button("Сбросить профиль аватара");
        reset.setOnClickListener(v -> {
            personalization.resetProfile();
            AvatarStyleControllerV28.persist(MainActivity.this, AvatarStyleV28.SCI_FI);
            refreshProfileStatus();
            refreshStyle();
        });
        root.addView(reset, buttonParams());

        selectedStyle = new TextView(this);
        selectedStyle.setTextSize(15f);
        selectedStyle.setPadding(0, dp(14), 0, dp(6));
        root.addView(selectedStyle, fullWidth());
        refreshStyle();

        addStyleButton(root, "Стиль лица: Человечный", AvatarStyleV28.HUMAN);
        addStyleButton(root, "Стиль лица: Научная фантастика", AvatarStyleV28.SCI_FI);
        addStyleButton(root, "Стиль лица: Аврора", AvatarStyleV28.CUSTOM);

        voiceStatus = new TextView(this);
        voiceStatus.setTextSize(15f);
        voiceStatus.setPadding(0, dp(14), 0, dp(6));
        root.addView(voiceStatus, fullWidth());
        refreshVoiceStatus();

        Button voiceOn = button("Включить голосовые реакции");
        voiceOn.setOnClickListener(v -> {
            AmbientPersonalityControllerV29.setVoiceEnabled(MainActivity.this, true);
            refreshVoiceStatus();
        });
        root.addView(voiceOn, buttonParams());

        Button voiceOff = button("Выключить голосовые реакции");
        voiceOff.setOnClickListener(v -> {
            AmbientPersonalityControllerV29.setVoiceEnabled(MainActivity.this, false);
            refreshVoiceStatus();
        });
        root.addView(voiceOff, buttonParams());

        TextView privacy = new TextView(this);
        privacy.setText("Приватность: настройки внешности, голоса и AI-статусов хранятся локально. AIDI получает только разрешённые нечувствительные параметры, локаль ru-RU и стиль общения. Имя, аккаунт, микрофон, камера, точная геопозиция и исходные медиа не передаются.");
        privacy.setTextSize(13f);
        privacy.setPadding(0, dp(14), 0, dp(18));
        root.addView(privacy, fullWidth());

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        screen.addView(scroll, scrollParams);

        Button apply = button("ПРИМЕНИТЬ ЖИВЫЕ ОБОИ");
        apply.setOnClickListener(v -> openWallpaperPicker());
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        applyParams.setMargins(dp(12), dp(6), dp(12), dp(6));
        screen.addView(apply, applyParams);

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
    }

    private void addProfilePreset(LinearLayout root, String label, String preset,
                                  AvatarStyleV28 style, int reactionLevel) {
        Button button = button(label);
        button.setOnClickListener(v -> {
            personalization.applyPreset(preset);
            AvatarStyleControllerV28.persist(MainActivity.this, style);
            AmbientPersonalityControllerV29.setReactionLevel(MainActivity.this, reactionLevel);
            refreshProfileStatus();
            refreshStyle();
            refreshVoiceStatus();
        });
        root.addView(button, buttonParams());
    }

    private void addStyleButton(LinearLayout root, String label, AvatarStyleV28 style) {
        Button button = button(label);
        button.setOnClickListener(v -> {
            AvatarStyleControllerV28.persist(MainActivity.this, style);
            refreshStyle();
        });
        root.addView(button, buttonParams());
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

    private void refreshProfileStatus() {
        AIPersonalizationProfileV30.Snapshot p = personalization.snapshot();
        profileStatus.setText("Профиль: " + russianPreset(p.preset)
                + " · выразительность " + percent(p.expressionIntensity) + "%"
                + " · свечение глаз " + percent(p.eyeGlow) + "%"
                + " · голос " + String.format(java.util.Locale.US, "%.2f× / %.2f×", p.voicePitch, p.voiceRate));
    }

    private static String russianPreset(String preset) {
        if (AIPersonalizationProfileV30.PRESET_SOFT.equals(preset)) return "Мягкий";
        if (AIPersonalizationProfileV30.PRESET_VIVID.equals(preset)) return "Яркий";
        return "Сбалансированный";
    }

    private void refreshStyle() {
        AvatarStyleControllerV28 controller = new AvatarStyleControllerV28(this);
        AvatarStyleV28 style = controller.getStyle();
        String value;
        if (style == AvatarStyleV28.HUMAN) value = "Человечный";
        else if (style == AvatarStyleV28.CUSTOM) value = "Аврора";
        else value = "Научная фантастика";
        selectedStyle.setText("Стиль аватара: " + value);
    }

    private void refreshContextStatus() {
        boolean visible = AIStatusOverlayRendererV34.readVisible(this);
        contextStatus.setText("AI-статусы: " + (visible ? "показываются" : "скрыты")
                + " · язык: Русский (ru-RU)");
    }

    private void refreshVoiceStatus() {
        boolean enabled = AmbientPersonalityControllerV29.readVoiceEnabled(this);
        int level = AmbientPersonalityControllerV29.readReactionLevel(this);
        voiceStatus.setText("Голос AI: " + (enabled ? "включён" : "выключен")
                + " · язык: Русский (" + RussianAIPersonalityV33.LOCALE_TAG + ")"
                + " · реакции: " + RussianAIPersonalityV33.reactionLevelName(level));
    }

    private static int percent(float value) {
        return Math.round(value * 100f);
    }

    private void openWallpaperPicker() {
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(this, GalaxyAIWallpaperService.class));
        startActivity(intent);
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
