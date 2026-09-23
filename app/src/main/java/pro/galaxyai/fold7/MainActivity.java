package pro.galaxyai.fold7;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import pro.galaxyai.fold7.engine.AvatarStyleControllerV28;
import pro.galaxyai.fold7.engine.AvatarStyleV28;

public class MainActivity extends Activity {
    private TextView selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(36, 48, 36, 48);

        TextView title = new TextView(this);
        title.setText("Galaxy AI Fold7 v28 — Avatar Style");
        title.setTextSize(22f);
        root.addView(title, fullWidth());

        selected = new TextView(this);
        selected.setTextSize(16f);
        selected.setPadding(0, 18, 0, 24);
        root.addView(selected, fullWidth());
        refreshSelection();

        addStyleButton(root, "Human", AvatarStyleV28.HUMAN);
        addStyleButton(root, "Sci-Fi", AvatarStyleV28.SCI_FI);
        addStyleButton(root, "Custom Aurora", AvatarStyleV28.CUSTOM);

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
            refreshSelection();
        });
        root.addView(button, fullWidth());
    }

    private void refreshSelection() {
        AvatarStyleControllerV28 controller = new AvatarStyleControllerV28(this);
        selected.setText("Selected: " + controller.getStyle().displayName);
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
