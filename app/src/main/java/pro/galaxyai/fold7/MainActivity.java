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
import pro.galaxyai.fold7.engine.AmbientPersonalityControllerV29;
import pro.galaxyai.fold7.engine.AvatarStyleControllerV28;
import pro.galaxyai.fold7.engine.AvatarStyleV28;

public class MainActivity extends Activity {
    private TextView selectedStyle;
    private TextView voiceStatus;
    private TextView profileStatus;
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
        title.setText("Galaxy AI Fold7 v31 — Adaptive Portrait Presence");
        title.setTextSize(21f);
        root.addView(title, fullWidth());

        TextView portraitInfo = new TextView(this);
        portraitInfo.setText("v31 portrait renderer: dimensional face shading, natural eyelids and lips, iris highlights, "
                + "subtle gaze/head motion and a larger Fold-aware composition. All portrait motion is generated locally.");
        portraitInfo.setTextSize(14f);
        portraitInfo.setPadding(0, 12, 0, 12);
        root.addView(portraitInfo, fullWidth());

        profileStatus = new TextView(this);
        profileStatus.setTextSize(15f);
        profileStatus.setPadding(0, 14, 0, 10);
        root.addView(profileStatus, fullWidth());
        refreshProfileStatus();

        addProfilePreset(root, "Personal profile: Soft", AIPersonalizationProfileV30.PRESET_SOFT,
                AvatarStyleV28.HUMAN, AmbientPersonalityControllerV29.LEVEL_QUIET);
        addProfilePreset(root, "Personal profile: Balanced", AIPersonalizationProfileV30.PRESET_BALANCED,
                AvatarStyleV28.SCI_FI, AmbientPersonalityControllerV29.LEVEL_NORMAL);
        addProfilePreset(root, "Personal profile: Vivid", AIPersonalizationProfileV30.PRESET_VIVID,
                AvatarStyleV28.CUSTOM, AmbientPersonalityControllerV29.LEVEL_EXPRESSIVE);

        Button reset = new Button(this);
        reset.setText("Reset personal avatar profile");
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

        addStyleButton(root, "Face style: Human", AvatarStyleV28.HUMAN);
        addStyleButton(root, "Face style: Sci-Fi", AvatarStyleV28.SCI_FI);
        addStyleButton(root, "Face style: Custom Aurora", AvatarStyleV28.CUSTOM);

        TextView privacy = new TextView(this);
        privacy.setText("Privacy: personal appearance and voice parameters are stored locally. "
                + "AIDI receives only bounded non-sensitive preference values; no local profile id, "
                + "name, account data, microphone audio, camera frames or raw media is sent.");
        privacy.setTextSize(14f);
        privacy.setPadding(0, 20, 0, 10);
        root.addView(privacy, fullWidth());

        voiceStatus = new TextView(this);
        voiceStatus.setTextSize(15f);
        voiceStatus.setPadding(0, 4, 0, 10);
        root.addView(voiceStatus, fullWidth());
        refreshVoiceStatus();

        Button voiceOn = new Button(this);
        voiceOn.setText("Enable ambient voice");
        voiceOn.setOnClickListener(v -> {
            AmbientPersonalityControllerV29.setVoiceEnabled(MainActivity.this, true);
            refreshVoiceStatus();
        });
        root.addView(voiceOn, fullWidth());

        Button voiceOff = new Button(this);
        voiceOff.setText("Disable ambient voice");
        voiceOff.setOnClickListener(v -> {
            AmbientPersonalityControllerV29.setVoiceEnabled(MainActivity.this, false);
            refreshVoiceStatus();
        });
        root.addView(voiceOff, fullWidth());

        Button apply = new Button(this);
        apply.setText("Apply live wallpaper");
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
        profileStatus.setText("Personal profile: " + p.preset
                + " · expression " + percent(p.expressionIntensity)
                + " · eye glow " + percent(p.eyeGlow)
                + " · voice " + String.format(java.util.Locale.US, "%.2f× / %.2f×", p.voicePitch, p.voiceRate));
    }

    private void refreshStyle() {
        AvatarStyleControllerV28 controller = new AvatarStyleControllerV28(this);
        selectedStyle.setText("Avatar style: " + controller.getStyle().displayName);
    }

    private void refreshVoiceStatus() {
        boolean enabled = AmbientPersonalityControllerV29.readVoiceEnabled(this);
        int level = AmbientPersonalityControllerV29.readReactionLevel(this);
        voiceStatus.setText("Ambient voice: " + (enabled ? "enabled" : "disabled")
                + " · reactions: " + levelName(level));
    }

    private static int percent(float value) {
        return Math.round(value * 100f);
    }

    private static String levelName(int level) {
        if (level == AmbientPersonalityControllerV29.LEVEL_EXPRESSIVE) return "expressive";
        if (level == AmbientPersonalityControllerV29.LEVEL_NORMAL) return "normal";
        return "quiet";
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
