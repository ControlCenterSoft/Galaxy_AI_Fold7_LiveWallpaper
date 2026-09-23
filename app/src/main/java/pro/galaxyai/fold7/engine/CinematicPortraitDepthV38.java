package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v38 cinematic depth layer for the photoreal portrait.
 *
 * Adds low-cost, fully local light shaping around the embedded portrait:
 * soft facial key light, cheek/eye moisture highlights, depth vignette and
 * emotion-aware color temperature. It never captures camera/microphone data
 * and does not transmit media.
 */
public final class CinematicPortraitDepthV38 {
    private final Paint soft = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint overlay = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint glint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float time;
    private float focus = 0.42f;
    private float curiosity = 0.38f;
    private float energy = 0.45f;
    private float presence = 0.75f;
    private String emotion = "calm";

    public void update(SceneDecision decision, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        if (decision == null) return;
        float k = Math.min(1f, dt * 2.4f);
        focus += (clamp(decision.focus, 0f, 1f) - focus) * k;
        curiosity += (clamp(decision.curiosity, 0f, 1f) - curiosity) * k;
        energy += (clamp(decision.energy, 0f, 1f) - energy) * k;
        presence += (clamp(decision.avatarPresence, 0.35f, 1f) - presence) * k;
        emotion = normalizeEmotion(decision.avatarState);
    }

    /** Draw background depth before the portrait. */
    public void drawBehind(Canvas canvas, int width, int height, boolean mainDisplay) {
        if (width <= 0 || height <= 0) return;

        float cx = width * (mainDisplay ? 0.56f : 0.52f);
        float cy = height * 0.34f;
        float radius = Math.max(width, height) * (mainDisplay ? 0.56f : 0.50f);

        int tone = emotionTone();
        int haloAlpha = Math.round((18f + presence * 18f + energy * 8f)
                * ("sleep".equals(emotion) ? 0.55f : 1f));
        soft.setShader(new RadialGradient(
                cx, cy, radius,
                withAlpha(tone, haloAlpha),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius, soft);
        soft.setShader(null);

        // Gentle lower-background separation gives the portrait more physical depth.
        int lower = withAlpha(Color.rgb(8, 10, 24), mainDisplay ? 34 : 28);
        overlay.setShader(new LinearGradient(
                0f, height * 0.46f, 0f, height,
                Color.TRANSPARENT, lower,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0f, height * 0.42f, width, height, overlay);
        overlay.setShader(null);
    }

    /** Draw subtle optical/skin highlights after the portrait. */
    public void drawOver(Canvas canvas, int width, int height, boolean mainDisplay) {
        if (width <= 0 || height <= 0) return;

        float activity = "sleep".equals(emotion) ? 0.20f : 1f;
        float microX = ((float) Math.sin(time * 2.41f)
                + (float) Math.sin(time * 4.79f + 0.7f) * 0.31f)
                * width * 0.00115f * (0.45f + curiosity * 0.55f) * activity;
        float microY = (float) Math.sin(time * 3.17f + 1.1f)
                * height * 0.00042f * activity;

        // Portrait eye locations after center-crop; deliberately subtle to avoid a mask effect.
        float eyeY = height * (mainDisplay ? 0.336f : 0.326f) + microY;
        float leftEyeX = width * 0.405f + microX;
        float rightEyeX = width * 0.596f + microX;
        float glintRadius = Math.max(1.0f, width * 0.0021f);
        int glintAlpha = Math.min(108, Math.round(42f + focus * 34f + curiosity * 20f));
        if (!"sleep".equals(emotion)) {
            glint.setColor(Color.argb(glintAlpha, 241, 249, 255));
            canvas.drawCircle(leftEyeX, eyeY, glintRadius, glint);
            canvas.drawCircle(rightEyeX, eyeY, glintRadius, glint);
        }

        // Skin bloom is concentrated around forehead/cheeks and is intentionally low-alpha.
        float faceCx = width * 0.50f;
        float faceCy = height * (mainDisplay ? 0.39f : 0.38f);
        float faceRadius = width * (mainDisplay ? 0.30f : 0.36f);
        int skinTone = emotionSkinTone();
        int bloomAlpha = Math.round((9f + presence * 10f + energy * 4f)
                * ("focused".equals(emotion) ? 0.82f : 1f));
        soft.setShader(new RadialGradient(
                faceCx, faceCy, faceRadius,
                withAlpha(skinTone, bloomAlpha),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(faceCx, faceCy, faceRadius, soft);
        soft.setShader(null);

        // Very light cinematic edge vignette improves perceived contrast without hiding details.
        float vignetteRadius = Math.max(width, height) * 0.72f;
        int edgeAlpha = mainDisplay ? 23 : 18;
        overlay.setShader(new RadialGradient(
                width * 0.50f, height * 0.46f, vignetteRadius,
                new int[]{Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(edgeAlpha, 0, 0, 8)},
                new float[]{0f, 0.68f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(new RectF(0f, 0f, width, height), overlay);
        overlay.setShader(null);
    }

    private int emotionTone() {
        if ("happy".equals(emotion)) return Color.rgb(255, 150, 211);
        if ("thinking".equals(emotion)) return Color.rgb(145, 124, 255);
        if ("focused".equals(emotion)) return Color.rgb(87, 171, 255);
        if ("sleep".equals(emotion)) return Color.rgb(68, 77, 126);
        return Color.rgb(104, 158, 255);
    }

    private int emotionSkinTone() {
        if ("happy".equals(emotion)) return Color.rgb(255, 205, 194);
        if ("thinking".equals(emotion)) return Color.rgb(224, 207, 255);
        if ("focused".equals(emotion)) return Color.rgb(210, 228, 255);
        if ("sleep".equals(emotion)) return Color.rgb(164, 175, 214);
        return Color.rgb(229, 220, 255);
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
