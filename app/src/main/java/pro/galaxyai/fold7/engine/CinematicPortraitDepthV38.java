package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v43-compatible cinematic depth layer.
 *
 * All color shaping is rendered behind the photoreal portrait. The foreground pass is
 * intentionally empty so the face is never covered by radial blooms, eye glints or
 * procedural geometry.
 */
public final class CinematicPortraitDepthV38 {
    private final Paint soft = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint overlay = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private float energy = 0.45f;
    private float presence = 0.75f;
    private String emotion = "calm";

    public void update(SceneDecision decision, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        if (decision == null) return;
        float k = Math.min(1f, dt * 2.4f);
        energy += (clamp(decision.energy, 0f, 1f) - energy) * k;
        presence += (clamp(decision.avatarPresence, 0.35f, 1f) - presence) * k;
        emotion = normalizeEmotion(decision.avatarState);
    }

    public void drawBehind(Canvas canvas, int width, int height, boolean mainDisplay) {
        if (width <= 0 || height <= 0) return;

        float cx = width * (mainDisplay ? 0.56f : 0.52f);
        float cy = height * 0.34f;
        float radius = Math.max(width, height) * (mainDisplay ? 0.56f : 0.50f);

        int tone = emotionTone();
        int haloAlpha = Math.round((14f + presence * 13f + energy * 6f)
                * ("sleep".equals(emotion) ? 0.50f : 1f));
        soft.setShader(new RadialGradient(
                cx, cy, radius,
                withAlpha(tone, haloAlpha),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius, soft);
        soft.setShader(null);

        int lower = withAlpha(Color.rgb(8, 10, 24), mainDisplay ? 30 : 24);
        overlay.setShader(new LinearGradient(
                0f, height * 0.50f, 0f, height,
                Color.TRANSPARENT, lower,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0f, height * 0.48f, width, height, overlay);
        overlay.setShader(null);
    }

    /** Foreground face pass disabled in v43 to guarantee maskless rendering. */
    public void drawOver(Canvas canvas, int width, int height, boolean mainDisplay) {
        // Intentionally no-op.
    }

    private int emotionTone() {
        if ("happy".equals(emotion)) return Color.rgb(255, 150, 211);
        if ("thinking".equals(emotion)) return Color.rgb(145, 124, 255);
        if ("focused".equals(emotion)) return Color.rgb(87, 171, 255);
        if ("sleep".equals(emotion)) return Color.rgb(68, 77, 126);
        return Color.rgb(104, 158, 255);
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(Math.max(0, Math.min(255, alpha)),
                Color.red(color), Color.green(color), Color.blue(color));
    }

    private static String normalizeEmotion(String value) {
        String v = value == null ? "calm" : value.trim().toLowerCase();
        if ("aware".equals(v)) return "thinking";
        if ("resting".equals(v)) return "sleep";
        if ("calm".equals(v) || "focused".equals(v) || "thinking".equals(v)
                || "happy".equals(v) || "sleep".equals(v)) return v;
        return "calm";
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
