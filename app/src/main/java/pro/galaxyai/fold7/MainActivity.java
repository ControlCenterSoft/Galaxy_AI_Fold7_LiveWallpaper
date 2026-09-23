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

import pro.galaxyai.fold7.engine.AmbientPersonalityControllerV29;
import pro.galaxyai.fold7.engine.AvatarStyleControllerV28;
import pro.galaxyai.fold7.engine.AvatarStyleV28;

public class MainActivity extends Activity {
    private TextView selectedStyle;
    private TextView voiceStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(36, 42, 36, 42);

        TextView title = new TextView(this);
        title.setText("Galaxy AI Fold7 v29 — Voice & Ambient Personality");
        title.setTextSize(21f);
        root.addView(title, fullWidth());

        selectedStyle = new TextView(this);
        selectedStyle.setTextSize(15f);
        selectedStyle.setPadding(0, 14, 0, 12);
        root.addView(selectedStyle, fullWidth());
        refreshStyle();

        addStyleButton(root, "Human", AvatarStyleV28.HUMAN);
        addStyleButton(root, "Sci-Fi", AvatarStyleV28.SCI_FI);
        addStyleButton(root, "Custom Aurora", AvatarStyleV28.CUSTOM);

        TextView privacy = new TextView(this);
        privacy.setText("Voice is opt-in and generated locally through Android TextToSpeech. "
                + "Galaxy AI never requests microphone permission or records audio.");
        privacy.setTextSize(14f);
        privacy.setPadding(0, 22, 0, 12);
        root.addView(privacy, fullWidth());

        voiceStatus = new TextView(this);
        voiceStatus.setTextSize(15f);
        voiceStatus.setPadding(0, 4, 0, 12);
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

        addReactionButton(root, "Reaction level: Quiet",
                AmbientPersonalityControllerV29.LEVEL_QUIET);
        addReactionButton(root, "Reaction level: Normal",
                AmbientPersonalityControllerV29.LEVEL_NORMAL);
        addReactionButton(root, "Reaction level: Expressive",
                AmbientPersonalityControllerV29.LEVEL_EXPRESSIVE);

        Button apply = new Button(this);
        apply.setText("Apply live wallpaper");
        apply.setOnClickListener(v -> openWallpaperPicker());
        root.addView(apply, fullWidth());

        setContentView(root);
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

    private void addReactionButton(LinearLayout root, String label, int level) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(v -> {
            AmbientPersonalityControllerV29.setReactionLevel(MainActivity.this, level);
            refreshVoiceStatus();
        });
        root.addView(button, fullWidth());
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
