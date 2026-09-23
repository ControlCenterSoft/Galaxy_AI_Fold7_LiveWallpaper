package pro.galaxyai.fold7.engine;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import pro.galaxyai.fold7.ai.RussianContextPresenceV34;
import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v34 visual Russian AI presence layer.
 *
 * Shows a compact Russian-only status card below the portrait. Visibility is local-only
 * and defaults to enabled. No new permissions or data sources are introduced.
 */
public final class AIStatusOverlayRendererV34 {
    public static final String PREFS = "russian_context_presence_v34";
    public static final String KEY_VISIBLE = "visible";

    private final SharedPreferences prefs;
    private final Paint panel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accent = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detail = new Paint(Paint.ANTI_ALIAS_FLAG);

    public AIStatusOverlayRendererV34(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        panel.setStyle(Paint.Style.FILL);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        detail.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        title.setTextAlign(Paint.Align.CENTER);
        detail.setTextAlign(Paint.Align.CENTER);
    }

    public void draw(Canvas canvas,
                     float centerX,
                     float centerY,
                     float maxWidth,
                     SceneDecision decision,
                     int reactionLevel,
                     float expressionIntensity,
                     boolean mainDisplay) {
        if (!isVisible() || canvas == null || decision == null) return;

        RussianContextPresenceV34.Snapshot snapshot = RussianContextPresenceV34.describe(
                decision.avatarState,
                decision.focus,
                decision.curiosity,
                decision.energy,
                reactionLevel,
                System.currentTimeMillis());

        float width = Math.max(240f, Math.min(maxWidth, mainDisplay ? 620f : 520f));
        float titleSize = clamp(width * 0.052f, 22f, 34f);
        float detailSize = clamp(width * 0.034f, 15f, 22f);
        float height = titleSize + detailSize + 54f;
        float radius = Math.max(22f, height * 0.32f);
        float top = centerY - height * 0.5f;
        float left = centerX - width * 0.5f;

        int panelAlpha = Math.round(72f + 34f * clamp01(expressionIntensity));
        panel.setColor(Color.argb(panelAlpha, 8, 12, 25));
        accent.setColor(Color.argb(Math.round(110f + snapshot.accent * 90f), 120, 195, 255));
        title.setColor(Color.argb(238, 238, 247, 255));
        detail.setColor(Color.argb(198, 205, 221, 245));
        title.setTextSize(titleSize);
        detail.setTextSize(detailSize);

        RectF box = new RectF(left, top, left + width, top + height);
        canvas.drawRoundRect(box, radius, radius, panel);
        canvas.drawRoundRect(new RectF(left + width * 0.18f, top + 8f,
                left + width * 0.82f, top + 12f), 4f, 4f, accent);

        float titleY = top + 24f - title.ascent();
        float detailY = titleY + title.descent() - detail.ascent() + 4f;
        canvas.drawText(snapshot.headline, centerX, titleY, title);
        canvas.drawText(snapshot.detail, centerX, detailY, detail);
    }

    public boolean isVisible() {
        return prefs.getBoolean(KEY_VISIBLE, true);
    }

    public static void setVisible(Context context, boolean visible) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_VISIBLE, visible).apply();
    }

    public static boolean readVisible(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_VISIBLE, true);
    }

    private static float clamp01(float value) {
        return clamp(value, 0f, 1f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
