package pro.galaxyai.fold7;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
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

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(36, 36, 36, 36);

        TextView title = new TextView(this);
        title.setText("Galaxy AI Fold7 v34 — Русское контекстное присутствие");
        title.setTextSize(21f);
        root.addView(title, fullWidth());

        TextView languageInfo = new TextView(this);
        languageInfo.setText("Язык общения AI на телефоне: Русский (ru-RU). "
                + "v34 добавляет короткие контекстные русские статусы прямо под живым портретом. "
                + "Они формируются локально из уже доступных безопасных параметров сцены.");
        languageInfo.setTextSize(14f);
        languageInfo.setPadding(0, 12, 0, 12);
        root.addView(languageInfo, fullWidth());

        contextStatus = new TextView(this);
        contextStatus.setTextSize(15f);
        contextStatus.setPadding(0, 4, 0, 8);
        root.addView(contextStatus, fullWidth());
        refreshContextStatus();

        Button contextOn = new Button(this);
        contextOn.setText("Показывать русские AI-статусы");
        contextOn.setOnClickListener(v -> {
            AIStatusOverlayRendererV34.setVisible(MainActivity.this, true);
            refreshContextStatus();
        });
        root.addView(contextOn, fullWidth());

        Button contextOff = new Button(this);
        contextOff.setText("Скрыть AI-статусы");
        contextOff.setOnClickListener(v -> {
            AIStatusOverlayRendererV34.setVisible(MainActivity.this, false);
            refreshContextStatus();
        });
        root.addView(contextOff, fullWidth());

        TextView portraitInfo = new TextView(this);
        portraitInfo.setText("Живой AI-аватар сохраняет Expression Presence: плавное внимание, эмоции, "
                + "масштаб и позиционирование портрета, адаптивное свечение и частицы, Fold continuity и тихий sleep-режим.");
        portraitInfo.setTextSize(14f);
        portraitInfo.setPadding(0, 8, 0, 12);
        root.addView(portraitInfo, fullWidth());

        profileStatus = new TextView(this);
        profileStatus.setTextSize(15f);
        profileStatus.setPadding(0, 14, 0, 10);
        root.addView(profileStatus, fullWidth());
        refreshProfileStatus();

        addProfilePreset(root, "Профиль: Мягкий", AIPersonalizationProfileV30.PRESET_SOFT,
                AvatarStyleV28.HUMAN, AmbientPersonalityControllerV29.LEVEL_QUIET);
        addProfilePreset(root, "Профиль: Сбалансированный", AIPersonalizationProfileV30.PRESET_BALANCED,
                AvatarStyleV28.SCI_FI, AmbientPersonalityControllerV29.LEVEL_NORMAL);
        addProfilePreset(root, "Профиль: Яркий", AIPersonalizationProfileV30.PRESET_VIVID,
                AvatarStyleV28.CUSTOM, AmbientPersonalityControllerV29.LEVEL_EXPRESSIVE);

        Button reset = new Button(this);
        reset.setText("Сбросить персональный профиль аватара");
        reset.setOnClickListener(v -> {
            personalization.resetProfile();
            AvatarStyleControllerV28.persist(MainActivity.this, AvatarStyleV28.SCI_FI);
            refreshProfileStatus();
            refreshStyle();
        });
        root.addView(reset, fullWidth());

        selectedStyle = new TextView(this);
        selectedStyle.setTextSize(15f);
        selectedStyle.setPadding(0, 18, 0, 10);
        root.addView(selectedStyle, fullWidth());
        refreshStyle();

        addStyleButton(root, "Стиль лица: Человечный", AvatarStyleV28.HUMAN);
        addStyleButton(root, "Стиль лица: Sci-Fi", AvatarStyleV28.SCI_FI);
        addStyleButton(root, "Стиль лица: Custom Aurora", AvatarStyleV28.CUSTOM);

        TextView privacy = new TextView(this);
        privacy.setText("Приватность: внешность, параметры голоса и видимость AI-статусов хранятся локально. "
                + "AIDI получает только ограниченные нечувствительные значения предпочтений, локаль ru-RU "
                + "и стиль общения; имя, аккаунт, микрофон, камера, геолокация и raw media не передаются.");
        privacy.setTextSize(14f);
        privacy.setPadding(0, 20, 0, 10);
        root.addView(privacy, fullWidth());

        voiceStatus = new TextView(this);
        voiceStatus.setTextSize(15f);
        voiceStatus.setPadding(0, 4, 0, 10);
        root.addView(voiceStatus, fullWidth());
        refreshVoiceStatus();

        Button voiceOn = new Button(this);
        voiceOn.setText("Включить голосовые реакции");
        voiceOn.setOnClickListener(v -> {
            AmbientPersonalityControllerV29.setVoiceEnabled(MainActivity.this, true);
            refreshVoiceStatus();
        });
        root.addView(voiceOn, fullWidth());

        Button voiceOff = new Button(this);
        voiceOff.setText("Выключить голосовые реакции");
        voiceOff.setOnClickListener(v -> {
            AmbientPersonalityControllerV29.setVoiceEnabled(MainActivity.this, false);
            refreshVoiceStatus();
        });
        root.addView(voiceOff, fullWidth());

        Button apply = new Button(this);
        apply.setText("Применить живые обои");
        apply.setOnClickListener(v -> openWallpaperPicker());
        root.addView(apply, fullWidth());

        setContentView(root);
    }

    private void addProfilePreset(LinearLayout root, String label, String preset,
                                  AvatarStyleV28 style, int reactionLevel) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(v -> {
            personalization.applyPreset(preset);
            AvatarStyleControllerV28.persist(MainActivity.this, style);
            AmbientPersonalityControllerV29.setReactionLevel(MainActivity.this, reactionLevel);
            refreshProfileStatus();
            refreshStyle();
            refreshVoiceStatus();
        });
        root.addView(button, fullWidth());
    }

    private void addStyleButton(LinearLayout root, String label, AvatarStyleV28 style) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(v -> {
            AvatarStyleControllerV28.persist(MainActivity.this, style);
            refreshStyle();
        });
        root.addView(button, fullWidth());
    }

    private void refreshProfileStatus() {
        AIPersonalizationProfileV30.Snapshot p = personalization.snapshot();
        profileStatus.setText("Персональный профиль: " + p.preset
                + " · выразительность " + percent(p.expressionIntensity) + "%"
                + " · свечение глаз " + percent(p.eyeGlow) + "%"
                + " · голос " + String.format(java.util.Locale.US, "%.2f× / %.2f×", p.voicePitch, p.voiceRate));
    }

    private void refreshStyle() {
        AvatarStyleControllerV28 controller = new AvatarStyleControllerV28(this);
        selectedStyle.setText("Стиль аватара: " + controller.getStyle().displayName);
    }

    private void refreshContextStatus() {
        boolean visible = AIStatusOverlayRendererV34.readVisible(this);
        contextStatus.setText("Контекстные статусы AI: " + (visible ? "показываются" : "скрыты")
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

    private static LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }
}
