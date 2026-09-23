package pro.galaxyai.fold7.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import pro.galaxyai.fold7.ai.SceneDecision;

/**
 * v41 privacy-safe speech face synchronizer.
 *
 * Animates a subtle mouth opening/highlight from the local TTS lifecycle envelope only.
 * It never reads microphone samples, audio buffers or network media. The geometry is tied
 * to the bundled v36 portrait crop and remains completely local.
 */
public final class SpeechFaceSyncV41 {
    private static final float PORTRAIT_W = 941f;
    private static final float PORTRAIT_H = 1672f;

    private final Paint mouthShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lipLight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerLight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint speechGlow = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float mouthOpen;
    private float targetOpen;
    private float speechEnergy;
    private float time;
    private String emotion = "calm";

    public SpeechFaceSyncV41() {
        lipLight.setStyle(Paint.Style.STROKE);
        lipLight.setStrokeCap(Paint.Cap.ROUND);
        innerLight.setStyle(Paint.Style.STROKE);
        innerLight.setStrokeCap(Paint.Cap.ROUND);
    }

    public void update(boolean speaking, float activity, SceneDecision decision, float deltaSeconds) {
        float dt = clamp(deltaSeconds, 0f, 0.10f);
        time += dt;
        if (decision != null) emotion = normalizeEmotion(decision.avatarState);

        float a = clamp(activity, 0f, 1f);
        if (speaking) {
            // Avoid a mechanical binary flap: preserve a small resting gap and blend the
            // synthetic Russian-TTS envelope with two local harmonic components.
            float harmonic = 0.5f + 0.5f * (float) Math.sin(time * 20.6f + 0.35f);
            targetOpen = clamp(0.12f + a * 0.68f + harmonic * 0.16f, 0.08f, 0.96f);
            speechEnergy += (a - speechEnergy) * Math.min(1f, dt * 8.0f);
        } else {
            targetOpen = 0f;
            speechEnergy += (0f - speechEnergy) * Math.min(1f, dt * 5.0f);
        }

        float speed = targetOpen > mouthOpen ? 12.5f : 9.0f;
        mouthOpen += (targetOpen - mouthOpen) * Math.min(1f, dt * speed);
        if (mouthOpen < 0.01f) mouthOpen = 0f;
    }

    public void draw(Canvas canvas, int width, int height, boolean mainDisplay) {
        if (mouthOpen <= 0.012f || width <= 0 || height <= 0) return;

        float baseScale = Math.max(width / PORTRAIT_W, height / PORTRAIT_H);
        float drawW = PORTRAIT_W * baseScale;
        float drawH = PORTRAIT_H * baseScale;
        float left = (width - drawW) * 0.5f;
        float cropTravel = height - drawH;
        float portraitFocus = mainDisplay ? 0.43f : 0.47f;
        float top = cropTravel * portraitFocus;

        // Coordinates measured against the bundled portrait, intentionally conservative
        // so the effect reads as lip articulation instead of a painted mask.
        float cx = left + drawW * 0.548f;
        float cy = top + drawH * 0.517f;
        float mouthW = drawW * 0.118f;
        float openH = drawH * (0.0028f + mouthOpen * 0.0105f);

        if ("happy".equals(emotion)) {
            mouthW *= 1.04f;
            cy -= drawH * 0.0008f;
        } else if ("focused".equals(emotion)) {
            mouthW *= 0.96f;
        } else if ("sleep".equals(emotion)) {
            openH *= 0.52f;
        }

        int shadowAlpha = Math.min(112, Math.round(45f + mouthOpen * 62f));
        mouthShadow.setShader(new RadialGradient(
                cx, cy, mouthW * 0.56f,
                Color.argb(shadowAlpha, 78, 31, 44),
                Color.argb(Math.max(4, shadowAlpha / 7), 119, 58, 72),
                Shader.TileMode.CLAMP));
        RectF opening = new RectF(
                cx - mouthW * 0.47f,
                cy - openH * 0.45f,
                cx + mouthW * 0.47f,
                cy + openH * 0.72f);
        canvas.drawOval(opening, mouthShadow);
        mouthShadow.setShader(null);

        float upperY = cy - openH * 0.43f;
        float lowerY = cy + openH * 0.56f;
        lipLight.setStrokeWidth(Math.max(1f, drawW * 0.0015f));
        lipLight.setShader(new LinearGradient(
                cx - mouthW * 0.5f, upperY,
                cx + mouthW * 0.5f, upperY,
                new int[]{Color.argb(18, 255, 180, 192), Color.argb(70, 255, 205, 211), Color.argb(18, 255, 180, 192)},
                null, Shader.TileMode.CLAMP));
        Path upper = new Path();
        upper.moveTo(cx - mouthW * 0.44f, upperY);
        upper.quadTo(cx, upperY - openH * 0.18f, cx + mouthW * 0.44f, upperY);
        canvas.drawPath(upper, lipLight);
        lipLight.setShader(null);

        innerLight.setStrokeWidth(Math.max(0.8f, drawW * 0.0011f));
        innerLight.setColor(Color.argb(Math.min(74, Math.round(20f + speechEnergy * 48f)), 255, 219, 218));
        Path lower = new Path();
        lower.moveTo(cx - mouthW * 0.31f, lowerY);
        lower.quadTo(cx, lowerY + openH * 0.12f, cx + mouthW * 0.31f, lowerY);
        canvas.drawPath(lower, innerLight);

        int glowAlpha = Math.min(24, Math.round(speechEnergy * 22f));
        speechGlow.setShader(new RadialGradient(
                cx, cy, mouthW * 0.92f,
                Color.argb(glowAlpha, 255, 151, 192),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, mouthW * 0.92f, speechGlow);
        speechGlow.setShader(null);
    }

    public float getMouthOpen() {
        return mouthOpen;
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
